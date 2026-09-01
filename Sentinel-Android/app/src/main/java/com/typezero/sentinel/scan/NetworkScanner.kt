package com.typezero.sentinel.scan

import android.content.Context
import com.typezero.sentinel.data.db.WatchedTarget
import com.typezero.sentinel.data.model.DiscoveredDevice
import com.typezero.sentinel.data.model.ScanResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Runs a full scan: subnet sweep + mDNS + internet/gateway/watched reachability,
 * then merges the signals into a [ScanResult]. Pure data collection — no diffing
 * or persistence happens here.
 */
class NetworkScanner(context: Context) {

    private val appContext = context.applicationContext
    private val sweeper = SubnetSweeper()
    private val mdns = MdnsDiscovery(appContext)
    private val ssdp = SsdpDiscovery(appContext)
    private val portScanner = PortScanner()
    private val reverseDns = ReverseDnsResolver()

    suspend fun scan(watched: List<WatchedTarget>): ScanResult = coroutineScope {
        val now = System.currentTimeMillis()
        val net = NetworkUtils.currentNetwork(appContext)
            ?: return@coroutineScope ScanResult.noNetwork(now)

        val hosts = NetworkUtils.hostAddresses(net)

        val sweepJob = async { sweeper.sweep(hosts) }
        val mdnsJob = async { mdns.discover() }
        val ssdpJob = async { ssdp.discover() }
        val internetJob = async { ReachabilityChecker.internetUp() }
        val gatewayJob = async { ReachabilityChecker.gatewayUp(net.gatewayIp) }
        val watchedJob = async {
            watched.associate { it.id to ReachabilityChecker.checkTarget(it) }
        }

        val reachable = sweepJob.await()
        val mdnsHosts = mdnsJob.await().associateBy { it.ip }
        val ssdpHosts = ssdpJob.await()
        val arp = ArpReader.read()

        // Union of IPs seen by sweep, mDNS, and SSDP.
        val allIps = (reachable + mdnsHosts.keys + ssdpHosts.keys)
            .toSortedSet(compareBy { ipSortKey(it) })

        // Port-fingerprint only the live hosts (cheap because there are few).
        val portMap = portScanner.scan(allIps, DeviceClassifier.SIGNATURE_PORTS)
        // Reverse-DNS the live hosts to recover DHCP-registered names.
        val ptrNames = reverseDns.resolve(allIps)

        val devices = allIps.map { ip ->
            val mdnsHost = mdnsHosts[ip]
            val ssdpInfo = ssdpHosts[ip]
            val mac = arp[ip]
            val services = mdnsHost?.services?.toList().orEmpty()
            val openPorts = portMap[ip].orEmpty()
            // Prefer mDNS name, then SSDP friendly name, then reverse-DNS, for display + identity.
            val stableName = mdnsHost?.name?.takeIf { it.isNotBlank() }
                ?: ssdpInfo?.friendlyName?.takeIf { it.isNotBlank() }
                ?: ptrNames[ip]?.takeIf { it.isNotBlank() }

            val profile = DeviceClassifier.classify(
                ip = ip,
                gatewayIp = net.gatewayIp,
                openPorts = openPorts,
                mdnsServices = services,
                ssdp = ssdpInfo,
                hostname = stableName,
                mac = mac
            )

            val identity = DeviceIdentity.resolve(mac, stableName, services, ip)
            DiscoveredDevice(
                ip = ip,
                mac = mac,
                hostname = stableName,
                services = services,
                identityKey = identity.key,
                strongIdentity = identity.strong,
                openPorts = openPorts,
                deviceType = profile.type,
                vendor = profile.vendor ?: ssdpInfo?.manufacturer,
                model = profile.model ?: ssdpInfo?.modelName,
                typeConfidence = profile.confidence,
                typeReason = profile.reason
            )
        }

        ScanResult(
            networkKey = net.networkKey,
            gatewayIp = net.gatewayIp,
            devices = devices,
            internetUp = internetJob.await(),
            gatewayUp = gatewayJob.await(),
            watchedStatuses = watchedJob.await(),
            timestamp = now
        )
    }

    private fun ipSortKey(ip: String): Long {
        val parts = ip.split(".")
        if (parts.size != 4) return Long.MAX_VALUE
        return parts.fold(0L) { acc, s -> (acc shl 8) or (s.toLongOrNull() ?: 0L) }
    }
}

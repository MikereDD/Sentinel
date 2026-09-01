package com.typezero.sentinel.scan

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import java.net.Inet4Address
import java.util.concurrent.ConcurrentHashMap

/**
 * Enriches discovery with hostnames and advertised service types via mDNS.
 * Requires a multicast lock (CHANGE_WIFI_MULTICAST_STATE, a normal permission).
 * Best-effort: devices that do not advertise mDNS simply won't appear here.
 */
class MdnsDiscovery(context: Context) {

    private val appContext = context.applicationContext
    private val nsd = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifi = appContext.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as WifiManager

    /** ip -> host record (hostname + advertised service types). */
    data class MdnsHost(val ip: String, val name: String?, val services: MutableSet<String>)

    private val serviceTypes = listOf(
        "_http._tcp.",
        "_https._tcp.",
        "_workstation._tcp.",
        "_ssh._tcp.",
        "_sftp-ssh._tcp.",
        "_googlecast._tcp.",
        "_androidtvremote2._tcp.",
        "_amzn-wplay._tcp.",
        "_home-assistant._tcp.",
        "_companion-link._tcp.",
        "_apple-mobdev2._tcp.",
        "_airplay._tcp.",
        "_raop._tcp.",
        "_spotify-connect._tcp.",
        "_sonos._tcp.",
        "_hap._tcp.",
        "_printer._tcp.",
        "_ipp._tcp.",
        "_smb._tcp."
    )

    suspend fun discover(durationMs: Long = 4000): List<MdnsHost> {
        val found = ConcurrentHashMap<String, MdnsHost>()
        val resolveMutex = Mutex()
        val lock = wifi.createMulticastLock("sentinel-mdns").apply {
            setReferenceCounted(true)
            runCatching { acquire() }
        }
        val listeners = mutableListOf<NsdManager.DiscoveryListener>()

        try {
            for (type in serviceTypes) {
                val listener = object : NsdManager.DiscoveryListener {
                    override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {}
                    override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {}
                    override fun onDiscoveryStarted(serviceType: String?) {}
                    override fun onDiscoveryStopped(serviceType: String?) {}

                    override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                        resolve(serviceInfo, found, resolveMutex)
                    }

                    override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
                }
                runCatching {
                    nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
                    listeners.add(listener)
                }
            }
            delay(durationMs)
        } finally {
            listeners.forEach { runCatching { nsd.stopServiceDiscovery(it) } }
            runCatching { lock.release() }
        }
        return found.values.toList()
    }

    private fun resolve(
        info: NsdServiceInfo,
        found: ConcurrentHashMap<String, MdnsHost>,
        mutex: Mutex
    ) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {}
            override fun onServiceResolved(resolved: NsdServiceInfo) {
                @Suppress("DEPRECATION")
                val host = resolved.host
                if (host is Inet4Address) {
                    val ip = host.hostAddress ?: return
                    val record = found.getOrPut(ip) {
                        MdnsHost(ip, cleanName(resolved.serviceName), mutableSetOf())
                    }
                    record.services.add(resolved.serviceType.trim('.'))
                    if (record.name.isNullOrBlank()) {
                        found[ip] = record.copy(name = cleanName(resolved.serviceName))
                    }
                }
            }
        }
        // NsdManager historically allows only one in-flight resolve at a time on
        // older APIs; serialize and swallow ERROR_ALREADY_ACTIVE.
        runCatching {
            @Suppress("DEPRECATION")
            nsd.resolveService(info, resolveListener)
        }
    }

    private fun cleanName(raw: String?): String? =
        raw?.replace(Regex("\\\\(\\d{3})")) { m ->
            m.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: m.value
        }?.trim()?.takeIf { it.isNotBlank() }
}

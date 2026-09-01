package com.typezero.sentinel.data.repository

import com.typezero.sentinel.data.db.KnownDevice
import com.typezero.sentinel.data.db.NetworkEvent
import com.typezero.sentinel.data.db.NetworkState
import com.typezero.sentinel.data.db.SentinelDatabase
import com.typezero.sentinel.data.db.WatchedTarget
import com.typezero.sentinel.data.model.EventType
import com.typezero.sentinel.data.model.DiscoveredDevice
import com.typezero.sentinel.data.model.NetworkStatus
import com.typezero.sentinel.data.model.ScanResult
import com.typezero.sentinel.scan.NetworkScanner
import kotlinx.coroutines.flow.Flow

/** Summary of a single scan, returned to the UI. */
data class ScanOutcome(
    val status: NetworkStatus,
    val networkKey: String?,
    val changes: List<NetworkEvent>,
    val internetUp: Boolean,
    val gatewayUp: Boolean,
    val deviceCount: Int,
    val onlineCount: Int,
    val timestamp: Long
)

class SentinelRepository(
    private val db: SentinelDatabase,
    private val scanner: NetworkScanner
) {
    private val knownDao = db.knownDeviceDao()
    private val watchedDao = db.watchedTargetDao()
    private val eventDao = db.networkEventDao()
    private val stateDao = db.networkStateDao()

    val recentEvents: Flow<List<NetworkEvent>> = eventDao.observeRecent()
    val watchedTargets: Flow<List<WatchedTarget>> = watchedDao.observeAll()

    fun devicesForNetwork(networkKey: String): Flow<List<KnownDevice>> =
        knownDao.observeForNetwork(networkKey)

    suspend fun addWatched(label: String, host: String, port: Int?) {
        watchedDao.insert(
            WatchedTarget(
                label = label,
                host = host,
                port = port,
                lastOnline = null,
                lastChecked = 0L
            )
        )
    }

    suspend fun removeWatched(target: WatchedTarget) = watchedDao.delete(target)

    /** User override: set a custom display name (null/blank clears it). */
    suspend fun renameDevice(id: Long, name: String?) =
        knownDao.setCustomName(id, name?.trim()?.takeIf { it.isNotBlank() })

    /** User override: pin a device type, or pass null to fall back to auto-detection. */
    suspend fun setDeviceType(id: Long, type: com.typezero.sentinel.data.model.DeviceType?) =
        knownDao.setUserType(id, type?.name)

    /** User override: assign a room/location (null/blank clears it). */
    suspend fun setDeviceRoom(id: Long, room: String?) =
        knownDao.setRoom(id, room?.trim()?.takeIf { it.isNotBlank() })

    suspend fun clearTimeline() = eventDao.clear()

    /** Runs a scan, diffs it against stored state, persists changes, returns a summary. */
    suspend fun runScan(): ScanOutcome {
        val watched = watchedDao.all()
        val result = scanner.scan(watched)
        val key = result.networkKey
            ?: return ScanOutcome(
                status = NetworkStatus.NO_NETWORK,
                networkKey = null,
                changes = emptyList(),
                internetUp = false,
                gatewayUp = false,
                deviceCount = 0,
                onlineCount = 0,
                timestamp = result.timestamp
            )

        val events = ArrayList<NetworkEvent>()
        val now = result.timestamp
        val isBaselineScan = stateDao.get(key) == null && knownDao.forNetwork(key).isEmpty()

        diffDevices(key, result, now, events, emitNewEvents = !isBaselineScan)
        diffInfra(key, result, now, events)
        diffWatched(watched, result, now, key, events)

        if (events.isNotEmpty()) eventDao.insertAll(events)

        val devicesNow = knownDao.forNetwork(key)
        val onlineNow = devicesNow.count { it.online }

        val degraded = !result.internetUp ||
            !result.gatewayUp ||
            result.watchedStatuses.values.any { !it }

        val status = when {
            degraded -> NetworkStatus.DEGRADED
            events.isNotEmpty() -> NetworkStatus.CHANGED
            else -> NetworkStatus.NORMAL
        }

        return ScanOutcome(
            status = status,
            networkKey = key,
            changes = events,
            internetUp = result.internetUp,
            gatewayUp = result.gatewayUp,
            deviceCount = devicesNow.size,
            onlineCount = onlineNow,
            timestamp = now
        )
    }

    private suspend fun diffDevices(
        key: String,
        result: ScanResult,
        now: Long,
        events: MutableList<NetworkEvent>,
        emitNewEvents: Boolean
    ) {
        val known = knownDao.forNetwork(key)
        val knownByIdentity = known.associateBy { it.identityKey }
        val seenIdentities = HashSet<String>()

        for (d in result.devices) {
            seenIdentities.add(d.identityKey)
            val existing = knownByIdentity[d.identityKey]
            if (existing == null) {
                knownDao.insert(
                    KnownDevice(
                        networkKey = key,
                        identityKey = d.identityKey,
                        displayName = d.label,
                        lastIp = d.ip,
                        macAddress = d.mac,
                        hostname = d.hostname,
                        services = d.services.joinToString(","),
                        strongIdentity = d.strongIdentity,
                        firstSeen = now,
                        lastSeen = now,
                        online = true,
                        missedScans = 0,
                        deviceType = d.deviceType.name,
                        vendor = d.vendor,
                        model = d.model,
                        typeConfidence = d.typeConfidence
                    )
                )
                if (emitNewEvents) {
                    events.add(
                        event(EventType.NEW_DEVICE, "New device found", describe(d), key, now)
                    )
                }
            } else {
                if (!existing.online) {
                    events.add(
                        event(
                            EventType.DEVICE_RETURNED,
                            "${existing.customName ?: existing.displayName} returned online",
                            d.ip, key, now
                        )
                    )
                }
                // Overwrite type fields when this scan is at least as confident,
                // or when we previously had no type at all.
                val takeType = d.typeConfidence >= existing.typeConfidence ||
                    existing.deviceType == "UNKNOWN"
                knownDao.update(
                    existing.copy(
                        displayName = if (looksLikeIp(existing.displayName) && !looksLikeIp(d.label))
                            d.label else existing.displayName,
                        lastIp = d.ip,
                        macAddress = d.mac ?: existing.macAddress,
                        hostname = d.hostname ?: existing.hostname,
                        services = if (d.services.isNotEmpty())
                            d.services.joinToString(",") else existing.services,
                        lastSeen = now,
                        online = true,
                        missedScans = 0,
                        deviceType = if (takeType) d.deviceType.name else existing.deviceType,
                        vendor = if (takeType) d.vendor ?: existing.vendor else existing.vendor,
                        model = if (takeType) d.model ?: existing.model else existing.model,
                        typeConfidence = if (takeType) d.typeConfidence else existing.typeConfidence
                    )
                )
            }
        }

        // Discovery is fuzzy on Android. Require two consecutive misses before
        // promoting an unseen device to an offline event. This avoids timeline
        // chatter from sleeping phones, multicast loss, and transient Wi-Fi gaps.
        for (k in known) {
            if (k.identityKey in seenIdentities) continue
            if (!k.online) continue

            val misses = (k.missedScans + 1).coerceAtMost(2)
            if (misses >= 2) {
                knownDao.update(k.copy(online = false, missedScans = misses))
                events.add(
                    event(
                        EventType.DEVICE_OFFLINE,
                        "${k.customName ?: k.displayName} went offline",
                        k.lastIp, key, now
                    )
                )
            } else {
                knownDao.update(k.copy(missedScans = misses))
            }
        }
    }

    private suspend fun diffInfra(
        key: String,
        result: ScanResult,
        now: Long,
        events: MutableList<NetworkEvent>
    ) {
        val prev = stateDao.get(key)
        if (prev != null) {
            if (prev.internetUp && !result.internetUp) {
                events.add(event(EventType.INTERNET_OUTAGE, "Internet outage", null, key, now))
            } else if (!prev.internetUp && result.internetUp) {
                events.add(event(EventType.INTERNET_RESTORED, "Internet restored", null, key, now))
            }
            if (prev.gatewayUp && !result.gatewayUp) {
                events.add(event(EventType.GATEWAY_OFFLINE, "Router unreachable", result.gatewayIp, key, now))
            } else if (!prev.gatewayUp && result.gatewayUp) {
                events.add(event(EventType.GATEWAY_RESTORED, "Router back online", result.gatewayIp, key, now))
            }
        }
        stateDao.upsert(
            NetworkState(
                networkKey = key,
                internetUp = result.internetUp,
                gatewayUp = result.gatewayUp,
                lastScan = now
            )
        )
    }

    private suspend fun diffWatched(
        watched: List<WatchedTarget>,
        result: ScanResult,
        now: Long,
        key: String,
        events: MutableList<NetworkEvent>
    ) {
        for (t in watched) {
            val up = result.watchedStatuses[t.id] ?: continue
            val prev = t.lastOnline
            if (prev != null) {
                if (prev && !up) {
                    events.add(event(EventType.WATCHED_OFFLINE, "${t.label} offline", t.host, key, now))
                } else if (!prev && up) {
                    events.add(event(EventType.WATCHED_RETURNED, "${t.label} back online", t.host, key, now))
                }
            }
            watchedDao.update(t.copy(lastOnline = up, lastChecked = now))
        }
    }

    private fun event(
        type: EventType,
        title: String,
        detail: String?,
        key: String,
        now: Long
    ) = NetworkEvent(
        timestamp = now,
        type = type.name,
        title = title,
        detail = detail,
        networkKey = key
    )

    /** Builds a "what is it" line for a freshly-found device, e.g. "Samsung TV — 192.168.4.30". */
    private fun describe(d: DiscoveredDevice): String {
        val brand = listOfNotNull(d.vendor, d.model).joinToString(" ").trim()
        val typed = when {
            brand.isNotBlank() && d.deviceType != com.typezero.sentinel.data.model.DeviceType.UNKNOWN ->
                "$brand ${d.deviceType.pretty}"
            brand.isNotBlank() -> brand
            d.deviceType != com.typezero.sentinel.data.model.DeviceType.UNKNOWN -> d.deviceType.pretty
            else -> null
        }
        return if (typed != null) "$typed — ${d.ip}" else d.ip
    }

    private fun looksLikeIp(s: String): Boolean =
        Regex("""^\d{1,3}(\.\d{1,3}){3}$""").matches(s)
}

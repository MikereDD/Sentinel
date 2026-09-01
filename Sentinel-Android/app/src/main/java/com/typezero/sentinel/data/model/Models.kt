package com.typezero.sentinel.data.model

/** Top-level state surfaced on the Status screen. */
enum class NetworkStatus { NORMAL, CHANGED, DEGRADED, SCANNING, NO_NETWORK }

/** Categories used for timeline events. */
enum class EventType {
    NEW_DEVICE,
    DEVICE_OFFLINE,
    DEVICE_RETURNED,
    WATCHED_OFFLINE,
    WATCHED_RETURNED,
    INTERNET_OUTAGE,
    INTERNET_RESTORED,
    GATEWAY_OFFLINE,
    GATEWAY_RESTORED
}

/** A device seen during a single scan, before it is reconciled with the DB. */
data class DiscoveredDevice(
    val ip: String,
    val mac: String?,
    val hostname: String?,
    val services: List<String>,
    val identityKey: String,
    val strongIdentity: Boolean,
    val openPorts: Set<Int> = emptySet(),
    val deviceType: DeviceType = DeviceType.UNKNOWN,
    val vendor: String? = null,
    val model: String? = null,
    val typeConfidence: Int = 0,
    val typeReason: String? = null
) {
    val label: String
        get() = hostname?.takeIf { it.isNotBlank() }
            ?: listOfNotNull(vendor, model).joinToString(" ").takeIf { it.isNotBlank() }
            ?: mac?.takeIf { it.isNotBlank() }
            ?: ip
}

/** The raw output of one network scan. */
data class ScanResult(
    val networkKey: String?,
    val gatewayIp: String?,
    val devices: List<DiscoveredDevice>,
    val internetUp: Boolean,
    val gatewayUp: Boolean,
    /** target id -> reachable */
    val watchedStatuses: Map<Long, Boolean>,
    val timestamp: Long
) {
    companion object {
        fun noNetwork(timestamp: Long) = ScanResult(
            networkKey = null,
            gatewayIp = null,
            devices = emptyList(),
            internetUp = false,
            gatewayUp = false,
            watchedStatuses = emptyMap(),
            timestamp = timestamp
        )
    }
}

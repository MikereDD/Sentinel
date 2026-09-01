package com.typezero.sentinel.scan

/**
 * Produces a stable identity key for a discovered device.
 *
 * Preference order:
 *   1. MAC address          -> strong identity (survives DHCP lease changes)
 *   2. mDNS hostname        -> weak but fairly stable
 *   3. service fingerprint  -> weak, anchored to IP
 *   4. IP only              -> weakest fallback
 *
 * Only MAC-based keys are marked [strong]. Weak keys can produce occasional
 * false "new device" events when a device's IP changes; this is an accepted
 * trade-off given Android's restrictions on reading neighbour MAC addresses.
 */
object DeviceIdentity {

    data class Identity(val key: String, val strong: Boolean)

    fun resolve(
        mac: String?,
        hostname: String?,
        services: List<String>,
        ip: String
    ): Identity {
        if (!mac.isNullOrBlank()) {
            return Identity(mac.lowercase(), strong = true)
        }
        val host = hostname?.lowercase()?.takeIf { it.isNotBlank() }
        if (host != null) {
            return Identity("host:$host", strong = false)
        }
        if (services.isNotEmpty()) {
            return Identity("fp:${services.sorted().joinToString(",")}@$ip", strong = false)
        }
        return Identity("ip:$ip", strong = false)
    }
}

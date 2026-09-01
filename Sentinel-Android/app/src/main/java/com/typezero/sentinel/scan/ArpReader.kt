package com.typezero.sentinel.scan

import java.io.File

/**
 * Best-effort MAC lookup from the kernel ARP cache.
 *
 * NOTE: On Android 10+ /proc/net/arp is restricted for non-system apps and will
 * usually return only the header (i.e. an empty map). It still works on some
 * OEM builds and on rooted devices, so we try it but never depend on it.
 * When this returns nothing, identity falls back to the fingerprint path in
 * [DeviceIdentity].
 */
object ArpReader {

    private val macRegex = Regex("^([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}$")

    fun read(): Map<String, String> {
        val result = HashMap<String, String>()
        try {
            File("/proc/net/arp").bufferedReader().useLines { lines ->
                lines.drop(1).forEach { line ->
                    val cols = line.trim().split(Regex("\\s+"))
                    if (cols.size >= 4) {
                        val ip = cols[0]
                        val mac = cols[3]
                        if (mac.matches(macRegex) && mac != "00:00:00:00:00:00") {
                            result[ip] = mac.lowercase()
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Expected on modern Android; silently degrade.
        }
        return result
    }
}

package com.typezero.sentinel.scan

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.URL

/**
 * SSDP / UPnP discovery (UDP multicast to 239.255.255.250:1900).
 *
 * This is the single richest MAC-free signal for typing TVs, computers, consoles,
 * media renderers, NAS boxes and routers: the M-SEARCH response carries a SERVER
 * header and a LOCATION pointing at a device-description XML that usually contains
 * manufacturer, modelName and friendlyName outright (e.g. "Samsung", "LG webOS TV",
 * "Xbox-System", "Sonos One").
 *
 * Requires only the multicast lock (CHANGE_WIFI_MULTICAST_STATE, a normal permission).
 */
class SsdpDiscovery(context: Context) {

    private val wifi = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as WifiManager

    data class SsdpInfo(
        val ip: String,
        var server: String? = null,
        var location: String? = null,
        val st: MutableSet<String> = mutableSetOf(),
        var friendlyName: String? = null,
        var manufacturer: String? = null,
        var modelName: String? = null,
        var deviceType: String? = null
    )

    suspend fun discover(durationMs: Long = 3500): Map<String, SsdpInfo> =
        withContext(Dispatchers.IO) {
            val lock = wifi.createMulticastLock("sentinel-ssdp").apply {
                setReferenceCounted(true)
                runCatching { acquire() }
            }
            val results = HashMap<String, SsdpInfo>()
            try {
                val group = InetAddress.getByName("239.255.255.250")
                val socket = DatagramSocket().apply { soTimeout = 800 }
                val msearch = (
                    "M-SEARCH * HTTP/1.1\r\n" +
                        "HOST: 239.255.255.250:1900\r\n" +
                        "MAN: \"ssdp:discover\"\r\n" +
                        "MX: 2\r\n" +
                        "ST: ssdp:all\r\n\r\n"
                    ).toByteArray()

                repeat(2) {
                    runCatching {
                        socket.send(DatagramPacket(msearch, msearch.size, group, 1900))
                    }
                }

                val deadline = System.currentTimeMillis() + durationMs
                val buf = ByteArray(2048)
                while (System.currentTimeMillis() < deadline) {
                    val resp = DatagramPacket(buf, buf.size)
                    try {
                        socket.receive(resp)
                    } catch (_: SocketTimeoutException) {
                        continue
                    } catch (_: Exception) {
                        break
                    }
                    val ip = resp.address?.hostAddress ?: continue
                    val text = String(resp.data, 0, resp.length)
                    val info = results.getOrPut(ip) { SsdpInfo(ip) }
                    parseHeaders(text, info)
                }
                runCatching { socket.close() }

                // Fetch device descriptions for richer brand/model info.
                for (info in results.values) {
                    val loc = info.location ?: continue
                    runCatching { fetchDescription(loc, info) }
                }
            } catch (_: Exception) {
                // Best-effort; return whatever we gathered.
            } finally {
                runCatching { lock.release() }
            }
            results
        }

    private fun parseHeaders(text: String, info: SsdpInfo) {
        for (line in text.split("\r\n")) {
            val idx = line.indexOf(':')
            if (idx <= 0) continue
            val key = line.substring(0, idx).trim().uppercase()
            val value = line.substring(idx + 1).trim()
            when (key) {
                "SERVER" -> if (info.server.isNullOrBlank()) info.server = value
                "LOCATION" -> if (info.location.isNullOrBlank()) info.location = value
                "ST", "NT" -> if (value.isNotBlank()) info.st.add(value)
            }
        }
    }

    private fun fetchDescription(location: String, info: SsdpInfo) {
        val conn = URL(location).openConnection().apply {
            connectTimeout = 1500
            readTimeout = 1500
        }
        val xml = conn.getInputStream().use { it.readBytes().decodeToString() }
            .take(32_768)
        info.friendlyName = info.friendlyName ?: extract(xml, "friendlyName")
        info.manufacturer = info.manufacturer ?: extract(xml, "manufacturer")
        info.modelName = info.modelName ?: extract(xml, "modelName")
        info.deviceType = info.deviceType ?: extract(xml, "deviceType")
    }

    private fun extract(xml: String, tag: String): String? {
        val m = Regex("<$tag>(.*?)</$tag>", RegexOption.DOT_MATCHES_ALL).find(xml)
        return m?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
    }
}

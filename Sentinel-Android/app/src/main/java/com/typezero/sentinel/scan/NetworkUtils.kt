package com.typezero.sentinel.scan

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import java.net.Inet4Address

/**
 * Resolves the device's current IPv4 network from [ConnectivityManager.getLinkProperties].
 * This requires no runtime permissions (unlike reading the Wi-Fi SSID).
 */
object NetworkUtils {

    data class NetInfo(
        val localIp: String,
        val prefixLength: Int,
        val gatewayIp: String?,
        /** Stable-ish key identifying "this network", used to scope known devices. */
        val networkKey: String
    )

    fun currentNetwork(context: Context): NetInfo? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return null
        val active = cm.activeNetwork ?: return null
        val lp: LinkProperties = cm.getLinkProperties(active) ?: return null

        val link = lp.linkAddresses.firstOrNull {
            it.address is Inet4Address && !it.address.isLoopbackAddress
        } ?: return null

        val gateway = lp.routes
            .firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }
            ?.gateway
            ?.hostAddress

        val localIp = link.address.hostAddress ?: return null
        val networkKey = gateway ?: subnetBase(localIp, link.prefixLength)

        return NetInfo(localIp, link.prefixLength, gateway, networkKey)
    }

    /**
     * Returns the list of candidate host addresses to sweep.
     * Sweeps are capped to a /24 around the device to keep them fast and bounded;
     * for typical home networks (/24) this is the whole subnet anyway.
     */
    fun hostAddresses(net: NetInfo): List<String> {
        val effectivePrefix = maxOf(net.prefixLength, 24)
        val ipInt = ipToInt(net.localIp) ?: return emptyList()
        val mask = if (effectivePrefix == 0) 0 else (-1 shl (32 - effectivePrefix))
        val network = ipInt and mask
        val broadcast = network or mask.inv()

        val hosts = ArrayList<String>()
        var addr = network + 1
        while (addr < broadcast) {
            val ip = intToIp(addr)
            if (ip != net.localIp) hosts.add(ip)
            addr++
        }
        return hosts
    }

    fun subnetBase(ip: String, prefix: Int): String {
        val ipInt = ipToInt(ip) ?: return ip
        val mask = if (prefix == 0) 0 else (-1 shl (32 - prefix))
        return intToIp(ipInt and mask)
    }

    private fun ipToInt(ip: String): Int? {
        val parts = ip.split(".")
        if (parts.size != 4) return null
        var result = 0
        for (p in parts) {
            val n = p.toIntOrNull() ?: return null
            if (n !in 0..255) return null
            result = (result shl 8) or n
        }
        return result
    }

    private fun intToIp(value: Int): String =
        "${(value ushr 24) and 0xFF}.${(value ushr 16) and 0xFF}." +
            "${(value ushr 8) and 0xFF}.${value and 0xFF}"
}

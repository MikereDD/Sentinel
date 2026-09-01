package com.typezero.sentinel.scan

import com.typezero.sentinel.data.db.WatchedTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

object ReachabilityChecker {

    /** TCP connect to public DNS resolvers; avoids HTTP overhead and captive-portal noise. */
    suspend fun internetUp(): Boolean = withContext(Dispatchers.IO) {
        tcp("8.8.8.8", 53, 1500) || tcp("1.1.1.1", 53, 1500)
    }

    suspend fun gatewayUp(gatewayIp: String?): Boolean = withContext(Dispatchers.IO) {
        if (gatewayIp == null) return@withContext false
        pingOrTcp(gatewayIp, null, 1500)
    }

    suspend fun checkTarget(target: WatchedTarget): Boolean = withContext(Dispatchers.IO) {
        pingOrTcp(target.host, target.port, 2000)
    }

    private fun pingOrTcp(host: String, port: Int?, timeoutMs: Int): Boolean {
        if (port != null) return tcp(host, port, timeoutMs)
        return try {
            if (InetAddress.getByName(host).isReachable(timeoutMs)) true
            else tcp(host, 80, timeoutMs) || tcp(host, 443, timeoutMs)
        } catch (_: Exception) {
            false
        }
    }

    private fun tcp(host: String, port: Int, timeoutMs: Int): Boolean = try {
        Socket().use { it.connect(InetSocketAddress(host, port), timeoutMs); true }
    } catch (_: Exception) {
        false
    }
}

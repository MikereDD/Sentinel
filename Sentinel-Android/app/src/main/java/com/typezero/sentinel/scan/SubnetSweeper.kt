package com.typezero.sentinel.scan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Discovers reachable hosts on the subnet via a bounded-concurrency sweep.
 * Uses [InetAddress.isReachable] (ICMP where the OS permits, else a TCP echo)
 * and falls back to probing a handful of common ports for hosts that drop pings.
 */
class SubnetSweeper(
    private val concurrency: Int = 48,
    private val timeoutMs: Int = 350
) {
    private val probePorts = intArrayOf(80, 443, 22, 8123, 62078, 53, 445, 139, 5000)

    suspend fun sweep(hosts: List<String>): Set<String> = coroutineScope {
        val gate = Semaphore(concurrency)
        hosts.map { ip ->
            async(Dispatchers.IO) {
                gate.withPermit { if (isReachable(ip)) ip else null }
            }
        }.awaitAll().filterNotNull().toSet()
    }

    private fun isReachable(ip: String): Boolean {
        return try {
            if (InetAddress.getByName(ip).isReachable(timeoutMs)) true else tcpProbe(ip)
        } catch (_: Exception) {
            tcpProbe(ip)
        }
    }

    private fun tcpProbe(ip: String): Boolean {
        for (port in probePorts) {
            try {
                Socket().use { s ->
                    s.connect(InetSocketAddress(ip, port), timeoutMs)
                    return true
                }
            } catch (_: Exception) {
                // connection refused still proves the host is up, but is thrown as
                // an exception; treat only explicit refusals as "present".
                // Kept simple here: ignore and continue to next port.
            }
        }
        return false
    }
}

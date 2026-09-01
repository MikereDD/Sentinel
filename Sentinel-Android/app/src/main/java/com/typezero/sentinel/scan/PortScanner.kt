package com.typezero.sentinel.scan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Probes a curated list of "signature" ports against the (few) already-discovered
 * live hosts, so we get per-host open-port evidence for classification without
 * scanning the whole subnet for every port.
 */
class PortScanner(
    private val concurrency: Int = 64,
    private val timeoutMs: Int = 400
) {
    suspend fun scan(ips: Collection<String>, ports: List<Int>): Map<String, Set<Int>> =
        coroutineScope {
            if (ips.isEmpty()) return@coroutineScope emptyMap()
            val gate = Semaphore(concurrency)
            val results = ConcurrentHashMap<String, MutableSet<Int>>()
            ips.flatMap { ip -> ports.map { port -> ip to port } }
                .map { (ip, port) ->
                    async(Dispatchers.IO) {
                        gate.withPermit {
                            if (isOpen(ip, port)) {
                                results.getOrPut(ip) {
                                    Collections.synchronizedSet(mutableSetOf())
                                }.add(port)
                            }
                        }
                    }
                }
                .awaitAll()
            results.mapValues { it.value.toSet() }
        }

    private fun isOpen(ip: String, port: Int): Boolean = try {
        Socket().use { it.connect(InetSocketAddress(ip, port), timeoutMs); true }
    } catch (_: Exception) {
        false
    }
}

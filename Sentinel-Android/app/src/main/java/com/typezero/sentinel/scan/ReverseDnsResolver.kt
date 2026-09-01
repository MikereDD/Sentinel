package com.typezero.sentinel.scan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetAddress

/**
 * Best-effort reverse-DNS (PTR) lookups against the configured resolver (usually the
 * router). Many home routers answer PTR for their DHCP clients, which can recover names
 * like "Blink-Mini-xxxx" or "DESKTOP-ABC123" for hosts that advertise no mDNS/SSDP.
 *
 * Note: [InetAddress.getCanonicalHostName] is a blocking call with no timeout argument;
 * we cap it with [withTimeoutOrNull]. A lookup that overruns simply yields null (its
 * worker thread unwinds when the OS resolver gives up).
 */
class ReverseDnsResolver(
    private val concurrency: Int = 16,
    private val perHostTimeoutMs: Long = 1500
) {
    suspend fun resolve(ips: Collection<String>): Map<String, String> = coroutineScope {
        if (ips.isEmpty()) return@coroutineScope emptyMap()
        val gate = Semaphore(concurrency)
        ips.map { ip ->
            async(Dispatchers.IO) {
                gate.withPermit {
                    val name = withTimeoutOrNull(perHostTimeoutMs) {
                        runCatching {
                            InetAddress.getByName(ip).canonicalHostName
                        }.getOrNull()
                    }
                    // A missing PTR returns the IP back; treat that as "no name".
                    if (name != null && name != ip && name.isNotBlank()) ip to name else null
                }
            }
        }.awaitAll().filterNotNull().toMap()
    }
}

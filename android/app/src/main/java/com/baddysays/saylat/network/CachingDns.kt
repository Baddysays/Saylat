package com.baddysays.saylat.network

import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap

/** DNS-кэш с TTL — меньше задержек на медленной сети. */
class CachingDns(private val ttlMs: Long = 60_000L) : Dns {

    private data class CachedAddresses(
        val addresses: List<InetAddress>,
        val expiresAt: Long,
    )

    private val cache = ConcurrentHashMap<String, CachedAddresses>()

    override fun lookup(hostname: String): List<InetAddress> {
        val now = System.currentTimeMillis()
        cache[hostname]?.let { cached ->
            if (now < cached.expiresAt) return cached.addresses
        }
        val resolved = try {
            Dns.SYSTEM.lookup(hostname)
        } catch (e: UnknownHostException) {
            cache[hostname]?.addresses ?: throw e
        }
        cache[hostname] = CachedAddresses(resolved, now + ttlMs)
        return resolved
    }

    fun invalidate(hostname: String) {
        cache.remove(hostname)
    }
}

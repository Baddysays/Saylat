package com.baddysays.saylat.network

import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * OkHttp-клиент с DNS-кэшем, connection pool и retry — как в saylat_improvements HttpClient.kt.
 */
object SaylatHttpClient {

    private val dns = CachingDns(ttlMs = 60_000L)

    fun build(
        slowNetwork: Boolean = false,
        extraInterceptors: List<Interceptor> = emptyList(),
    ): OkHttpClient {
        val connectTimeout = if (slowNetwork) 60L else 30L
        val readTimeout = if (slowNetwork) 180L else 90L
        val writeTimeout = if (slowNetwork) 60L else 30L

        val builder = OkHttpClient.Builder()
            .dns(dns)
            .connectionPool(
                ConnectionPool(
                    maxIdleConnections = 3,
                    keepAliveDuration = 90L,
                    timeUnit = TimeUnit.SECONDS,
                ),
            )
            .connectTimeout(connectTimeout, TimeUnit.SECONDS)
            .readTimeout(readTimeout, TimeUnit.SECONDS)
            .writeTimeout(writeTimeout, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val builder = chain.request().newBuilder()
                    .header("Accept-Encoding", "gzip, deflate")
                    .header("Accept-Charset", "utf-8")
                if (slowNetwork) {
                    builder.header("X-Saylat-Slow-Network", "1")
                }
                chain.proceed(builder.build())
            }
            .addInterceptor(RetryInterceptor.forSlowNetwork(slowNetwork))
            .retryOnConnectionFailure(true)

        extraInterceptors.forEach { builder.addInterceptor(it) }
        return builder.build()
    }

    fun invalidateDns(hostname: String) = dns.invalidate(hostname)

    fun evictConnections(client: OkHttpClient?) {
        client?.connectionPool?.evictAll()
    }
}

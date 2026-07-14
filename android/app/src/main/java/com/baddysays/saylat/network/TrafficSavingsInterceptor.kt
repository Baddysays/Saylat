package com.baddysays.saylat.network

import okhttp3.Interceptor
import okhttp3.Response

/** Парсит X-Saylat-Savings: original=N compressed=M savings=P% */
object TrafficSavingsBridge {
    var listener: ((originalBytes: Long, compressedBytes: Long) -> Unit)? = null
}

class TrafficSavingsInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val header = response.header("X-Saylat-Savings") ?: return response
        val original = Regex("""original=(\d+)""").find(header)?.groupValues?.get(1)?.toLongOrNull()
        val compressed = Regex("""compressed=(\d+)""").find(header)?.groupValues?.get(1)?.toLongOrNull()
        if (original != null && compressed != null && original > 0) {
            TrafficSavingsBridge.listener?.invoke(original, compressed)
        }
        return response
    }
}

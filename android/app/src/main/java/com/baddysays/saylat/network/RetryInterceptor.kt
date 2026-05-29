package com.baddysays.saylat.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Повтор GET/POST при таймаутах и 5xx — важно для 2G.
 */
class RetryInterceptor(
    private val maxRetries: Int = 2,
    private val delayMs: Long = 1500L,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var lastError: IOException? = null
        val request = chain.request()
        while (attempt <= maxRetries) {
            try {
                val response = chain.proceed(request)
                if (attempt < maxRetries && shouldRetryResponse(response)) {
                    response.close()
                    sleep()
                    attempt++
                    continue
                }
                return response
            } catch (e: IOException) {
                lastError = e
                if (attempt >= maxRetries || !shouldRetryException(e)) {
                    throw e
                }
                sleep()
                attempt++
            }
        }
        throw lastError ?: IOException("retry failed")
    }

    private fun shouldRetryResponse(response: Response): Boolean {
        val code = response.code
        return code == 408 || code == 429 || code in 500..599
    }

    private fun shouldRetryException(e: IOException): Boolean {
        return e is SocketTimeoutException ||
            e.message?.contains("timeout", ignoreCase = true) == true ||
            e.message?.contains("timed out", ignoreCase = true) == true
    }

    private fun sleep() {
        try {
            Thread.sleep(delayMs)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}

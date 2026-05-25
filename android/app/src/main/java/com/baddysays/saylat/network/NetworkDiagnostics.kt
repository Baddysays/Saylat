package com.baddysays.saylat.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class NetworkTestResult(
    val ok: Boolean,
    val latencyMs: Int? = null,
    val downloadKbps: Double? = null,
    val bytesDownloaded: Long = 0,
    val profile: SpeedProfile? = null,
    val error: String? = null,
    val testedAt: Long = System.currentTimeMillis(),
    val liteMode: Boolean = false,
)

data class SpeedProfile(
    val tier: SpeedTier,
    val title: String,
    val description: String,
)

enum class SpeedTier {
    OFFLINE,
    EDGE_2G,
    SLOW_3G,
    FAST_3G,
    LTE,
    BROADBAND,
}

object NetworkDiagnostics {

    fun httpClient(slowNetwork: Boolean): OkHttpClient {
        val connectSec = if (slowNetwork) 60L else 25L
        val readSec = if (slowNetwork) 180L else 75L
        return OkHttpClient.Builder()
            .connectTimeout(connectSec, TimeUnit.SECONDS)
            .readTimeout(readSec, TimeUnit.SECONDS)
            .writeTimeout(connectSec, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    suspend fun runFullTest(baseUrl: String, slowNetwork: Boolean): NetworkTestResult =
        withContext(Dispatchers.IO) {
            val root = normalizeBase(baseUrl)
            val client = httpClient(slowNetwork)
            val lite = slowNetwork
            try {
                val samples = if (slowNetwork) 1 else 2
                val latency = measureLatency("$root/health", client, samples)
                var bench: BenchResult? = null
                val benchUrls = if (lite) {
                    listOf("$root/api/bench/lite", "$root/api/bench")
                } else {
                    listOf("$root/api/bench")
                }
                for (url in benchUrls) {
                    try {
                        bench = measureDownload(url, client)
                        break
                    } catch (_: Exception) {
                        continue
                    }
                }
                if (bench != null) {
                    NetworkTestResult(
                        ok = true,
                        latencyMs = latency,
                        downloadKbps = bench.kbps,
                        bytesDownloaded = bench.bytes,
                        profile = classify(bench.kbps, latency),
                        liteMode = lite,
                    )
                } else {
                    NetworkTestResult(
                        ok = true,
                        latencyMs = latency,
                        profile = classifyFromLatency(latency),
                        liteMode = lite,
                    )
                }
            } catch (e: Exception) {
                NetworkTestResult(
                    ok = false,
                    error = friendlyError(e),
                    profile = SpeedProfile(
                        tier = SpeedTier.OFFLINE,
                        title = "Нет связи",
                        description = if (slowNetwork) {
                            "На 2G тест может занять до 3 мин. Проверьте URL и подождите."
                        } else {
                            "Проверьте URL прокси и интернет."
                        },
                    ),
                    liteMode = lite,
                )
            }
        }

    fun classify(kbps: Double?, latencyMs: Int?): SpeedProfile {
        val k = kbps ?: 0.0
        val tier = when {
            k < 40 -> SpeedTier.EDGE_2G
            k < 150 -> SpeedTier.SLOW_3G
            k < 600 -> SpeedTier.FAST_3G
            k < 2500 -> SpeedTier.LTE
            else -> SpeedTier.BROADBAND
        }
        return tierProfile(tier)
    }

    private fun classifyFromLatency(latencyMs: Int): SpeedProfile {
        val tier = when {
            latencyMs > 8_000 -> SpeedTier.EDGE_2G
            latencyMs > 3_000 -> SpeedTier.SLOW_3G
            latencyMs > 1_200 -> SpeedTier.FAST_3G
            latencyMs > 400 -> SpeedTier.LTE
            else -> SpeedTier.BROADBAND
        }
        return tierProfile(tier, extra = "Замер скорости не выполнен")
    }

    private fun tierProfile(tier: SpeedTier, extra: String? = null): SpeedProfile = when (tier) {
        SpeedTier.EDGE_2G -> SpeedProfile(
            tier = tier,
            title = "2G / EDGE",
            description = extra ?: "Только текст и лёгкие страницы",
        )
        SpeedTier.SLOW_3G -> SpeedProfile(
            tier = tier,
            title = "Медленный 3G",
            description = extra ?: "Новости без тяжёлых картинок",
        )
        SpeedTier.FAST_3G -> SpeedProfile(
            tier = tier,
            title = "3G",
            description = extra ?: "Комфортное чтение, картинки сжимаются",
        )
        SpeedTier.LTE -> SpeedProfile(
            tier = tier,
            title = "4G / LTE",
            description = extra ?: "Быстрый поиск и статьи с фото",
        )
        SpeedTier.BROADBAND -> SpeedProfile(
            tier = tier,
            title = "Широкий канал",
            description = extra ?: "Подходит для всего, что отдаёт Saylat",
        )
        SpeedTier.OFFLINE -> SpeedProfile(
            tier = SpeedTier.OFFLINE,
            title = "Нет связи",
            description = "Сервер недоступен",
        )
    }

    private fun friendlyError(e: Exception): String {
        val raw = e.message.orEmpty()
        return when {
            raw.contains("timeout", ignoreCase = true) ||
                raw.contains("12000", ignoreCase = true) ||
                raw.contains("failed to connect", ignoreCase = true) ->
                "Таймаут на медленной сети. Включите «Режим 2G» в настройках и повторите."
            raw.length > 120 -> raw.take(120) + "…"
            else -> raw.ifBlank { "Ошибка соединения" }
        }
    }

    private fun normalizeBase(url: String): String {
        val t = url.trim().trimEnd('/')
        return if (t.startsWith("http")) t else "http://$t"
    }

    private fun measureLatency(url: String, client: OkHttpClient, samples: Int): Int {
        val times = mutableListOf<Long>()
        repeat(samples) {
            val start = System.nanoTime()
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
                resp.body?.close()
            }
            times += (System.nanoTime() - start) / 1_000_000
        }
        return times.average().roundToInt()
    }

    private data class BenchResult(val bytes: Long, val kbps: Double)

    private fun measureDownload(url: String, client: OkHttpClient): BenchResult {
        val start = System.nanoTime()
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
            val body = resp.body ?: throw IllegalStateException("Пустой ответ")
            val data = body.bytes()
            val ms = (System.nanoTime() - start) / 1_000_000.0
            val kbps = if (ms > 0) data.size * 8.0 / ms else 0.0
            return BenchResult(bytes = data.size.toLong(), kbps = kbps)
        }
    }
}

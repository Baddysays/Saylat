package com.baddysays.saylat.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
/**
 * Единая загрузка статей: progressive (2G), saylat-binary, delta, JSON fallback.
 */
object ArticleFetcher {
    suspend fun fetchArticle(
        context: Context,
        client: OkHttpClient,
        api: SaylatApi,
        baseUrl: String,
        url: String,
        images: String,
        level: String,
        slowNetwork: Boolean,
        apiKey: String,
        onPartial: suspend (SaylatArticle) -> Unit = {},
    ): SaylatArticle {
        if (slowNetwork) {
            runCatching {
                return ProgressiveArticleLoader.load(
                    client, baseUrl, url, images, level, apiKey, onPartial,
                )
            }
        }

        val wireCache = ArticleWireCache(context)
        // Always try delta first — first call without cache gets full body + ETag and seeds cache
        runCatching {
            return fetchDelta(client, baseUrl, url, images, level, apiKey, wireCache, slowNetwork = false)
        }

        runCatching {
            val binary = api.extractBinary(url, images, level)
            if (binary.isSuccessful) {
                return PayloadCodec.decompressBinaryResponse(binary)
            }
        }

        return PayloadCodec.expandArticle(api.extract(url, images, level))
    }

    suspend fun fetchOpenArticle(
        context: Context,
        api: SaylatApi,
        request: OpenRequest,
        slowNetwork: Boolean,
        baseUrl: String,
        client: OkHttpClient,
        apiKey: String,
        onPartial: suspend (SaylatArticle) -> Unit = {},
    ): SaylatArticle {
        val url = request.url.orEmpty()
        if (request.target == "url" && url.startsWith("http")) {
            return fetchArticle(
                context, client, api, baseUrl, url,
                request.images, request.level, slowNetwork, apiKey, onPartial,
            )
        }
        runCatching {
            val binary = api.openBinary(request)
            if (binary.isSuccessful) {
                return PayloadCodec.decompressBinaryResponse(binary)
            }
        }
        val open = PayloadCodec.expandOpenResponse(api.open(request))
        return open.article ?: error("Empty open response")
    }

    private suspend fun fetchDelta(
        client: OkHttpClient,
        baseUrl: String,
        url: String,
        images: String,
        level: String,
        apiKey: String,
        wireCache: ArticleWireCache,
        slowNetwork: Boolean = false,
    ): SaylatArticle = withContext(Dispatchers.IO) {
        val cached = wireCache.get(url, images, level)
        val root = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val httpUrl = (root + "api/extract/delta").toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("url", url)
            .addQueryParameter("images", images)
            .addQueryParameter("level", level)
            .build()

        val builder = Request.Builder()
            .url(httpUrl)
            .header(PayloadCodec.HEADER_CODEC, PayloadCodec.HEADER_CODEC_VALUE)
        if (slowNetwork) builder.header("X-Saylat-Slow-Network", "1")
        if (apiKey.isNotBlank()) builder.header("X-API-Key", apiKey)
        cached?.etag?.let { builder.header("If-None-Match", "\"$it\"") }

        client.newCall(builder.build()).execute().use { response ->
            when (response.code) {
                304 -> {
                    val hit = cached ?: error("304 without cache")
                    parseWireBytes(hit.bytes)
                }
                200 -> {
                    val body = response.body?.bytes() ?: error("Empty delta body")
                    val isDelta = response.header("X-Saylat-Delta") == "true"
                    val restored = if (isDelta) {
                        val base = cached ?: error("Delta without base cache")
                        DeltaCodec.applyDelta(base.bytes, body)
                    } else {
                        body
                    }
                    val etag = response.header("ETag")?.trim('"').orEmpty()
                    if (etag.isNotEmpty()) wireCache.put(url, etag, restored, images, level)
                    parseWireBytes(restored)
                }
                else -> error("Delta HTTP ${response.code}")
            }
        }
    }

    private fun parseWireBytes(bytes: ByteArray): SaylatArticle {
        val codec = detectCodec(bytes)
        return when (codec) {
            PayloadCodec.CODEC_SAYLAT_BINARY -> SaylatBinaryCodec.decode(bytes)
            else -> {
                val json = String(bytes, Charsets.UTF_8)
                val env = PayloadCodec.parseEnvelope(json)
                PayloadCodec.expandArticleSync(env)
            }
        }
    }

    private fun detectCodec(bytes: ByteArray): String {
        if (bytes.isNotEmpty() && bytes[0].toInt() and 0xFF == 0x5A) return PayloadCodec.CODEC_SAYLAT_BINARY
        return PayloadCodec.CODEC_GZIP_B64
    }
}

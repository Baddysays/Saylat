package com.baddysays.saylat.data

import com.github.luben.zstd.ZstdInputStream
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

/**
 * Сервер сжимает JSON статьи (zstd или gzip), приложение распаковывает в фоне.
 */
object PayloadCodec {
    const val CODEC_SAYLAT_BINARY = "saylat-binary"
    const val CODEC_ZSTD_BINARY = "zstd-binary"
    const val CODEC_GZIP_BINARY = "gzip-binary"
    const val CODEC_GZIP_B64 = "gzip-b64"
    const val MEDIA_TYPE_SAYLAT_ZSTD = "application/vnd.saylat.v1+zstd"
    const val MEDIA_TYPE_SAYLAT_GZIP = "application/vnd.saylat.v1+gzip"
    const val MEDIA_TYPE_SAYLAT_BINARY = "application/vnd.saylat.v1+protobuf"

    const val HEADER_CODEC = "X-Saylat-Payload-Codec"
    const val HEADER_CODEC_VALUE =
        "$CODEC_SAYLAT_BINARY,$CODEC_ZSTD_BINARY,$CODEC_GZIP_BINARY,$CODEC_GZIP_B64,identity,delta"
    const val HDR_WIRE_BYTES = "X-Saylat-Wire-Bytes"
    const val HDR_UNCOMPRESSED_BYTES = "X-Saylat-Uncompressed-Bytes"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val articleAdapter = moshi.adapter(SaylatArticle::class.java)
    private val envelopeAdapter = moshi.adapter(ArticleWireEnvelope::class.java)

    suspend fun expandArticle(envelope: ArticleWireEnvelope): SaylatArticle =
        withContext(Dispatchers.Default) { expandArticleSync(envelope) }

    fun expandArticleSync(envelope: ArticleWireEnvelope): SaylatArticle {
        envelope.article?.let { return it }
        val wire = envelope.wire ?: error("Empty article envelope")
        return decompressArticle(wire)
    }

    suspend fun decompressBinaryResponse(response: Response<ResponseBody>): SaylatArticle =
        withContext(Dispatchers.Default) {
            val body = response.body() ?: error("Empty binary response")
            val bytes = body.bytes()
            body.close()
            val payloadCodec = response.headers()["X-Saylat-Payload-Codec"].orEmpty()
            val contentType = response.headers()["Content-Type"].orEmpty()
            val wireBytes = response.headers()[HDR_WIRE_BYTES]?.toIntOrNull() ?: bytes.size
            val uncompressed = response.headers()[HDR_UNCOMPRESSED_BYTES]?.toIntOrNull() ?: 0
            val codec = when {
                payloadCodec == CODEC_SAYLAT_BINARY ||
                    contentType.contains("protobuf", ignoreCase = true) -> CODEC_SAYLAT_BINARY
                payloadCodec == CODEC_ZSTD_BINARY || contentType.contains("zstd", ignoreCase = true) ->
                    CODEC_ZSTD_BINARY
                payloadCodec == CODEC_GZIP_BINARY || contentType.contains("gzip", ignoreCase = true) ->
                    CODEC_GZIP_BINARY
                else -> CODEC_GZIP_BINARY
            }
            if (codec == CODEC_SAYLAT_BINARY) {
                return@withContext decodeSaylatBinary(bytes, wireBytes, uncompressed)
            }
            if (codec != CODEC_ZSTD_BINARY && codec != CODEC_GZIP_BINARY) {
                val json = String(bytes, Charsets.UTF_8)
                val env = envelopeAdapter.fromJson(json)
                if (env != null) return@withContext expandArticleSync(env)
                return@withContext articleAdapter.fromJson(json) ?: error("Bad JSON fallback")
            }
            decompressPayloadBytes(bytes, codec, wireBytes, uncompressed)
        }

    private fun decodeSaylatBinary(bytes: ByteArray, wireBytes: Int, uncompressedBytes: Int): SaylatArticle {
        val article = SaylatBinaryCodec.decode(bytes)
        val stats = article.stats
        return article.copy(
            stats = stats.copy(
                wire_bytes = if (wireBytes > 0) wireBytes else bytes.size,
                payload_bytes = if (stats.payload_bytes > 0) stats.payload_bytes else uncompressedBytes,
            ),
        )
    }

    fun parseEnvelope(json: String): ArticleWireEnvelope =
        envelopeAdapter.fromJson(json) ?: articleAdapter.fromJson(json)?.let {
            ArticleWireEnvelope(article = it)
        } ?: error("Bad article envelope")

    fun decompressPayloadBytes(
        payload: ByteArray,
        codec: String,
        wireBytes: Int,
        uncompressedBytes: Int,
    ): SaylatArticle {
        if (codec == CODEC_SAYLAT_BINARY) {
            return decodeSaylatBinary(payload, wireBytes, uncompressedBytes)
        }
        val json = when (codec) {
            CODEC_ZSTD_BINARY -> ZstdInputStream(ByteArrayInputStream(payload)).bufferedReader()
                .use { it.readText() }
            CODEC_GZIP_BINARY, CODEC_GZIP_B64 -> GZIPInputStream(ByteArrayInputStream(payload))
                .bufferedReader().use { it.readText() }
            else -> error("Unsupported codec: $codec")
        }
        val article = articleAdapter.fromJson(json) ?: error("Failed to parse decompressed article")
        val stats = article.stats
        return article.copy(
            stats = stats.copy(
                wire_bytes = if (wireBytes > 0) wireBytes else payload.size,
                payload_bytes = if (stats.payload_bytes > 0) stats.payload_bytes else uncompressedBytes,
            ),
        )
    }

    fun decompressArticle(wire: WireCompressedPayload): SaylatArticle {
        val compressed = java.util.Base64.getDecoder().decode(wire.data)
        val codec = when (wire.codec) {
            CODEC_ZSTD_BINARY, CODEC_GZIP_BINARY, CODEC_GZIP_B64 -> wire.codec
            else -> CODEC_GZIP_B64
        }
        return decompressPayloadBytes(compressed, codec, wire.wire_bytes, wire.uncompressed_bytes)
    }

    suspend fun expandOpenResponse(response: OpenResponse): OpenResponse =
        withContext(Dispatchers.Default) { expandOpenResponseSync(response) }

    fun expandOpenResponseSync(response: OpenResponse): OpenResponse {
        val wire = response.wire ?: return response
        if (response.article != null) return response
        return response.copy(
            article = decompressArticle(wire),
            wire = null,
        )
    }
}

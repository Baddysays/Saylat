package com.baddysays.saylat.data

import android.content.Context
import okhttp3.OkHttpClient

/**
 * Загрузка статей: progressive (2G), saylat-binary, delta, JSON fallback.
 */
suspend fun SaylatApi.fetchArticle(
    context: Context,
    client: OkHttpClient,
    baseUrl: String,
    url: String,
    images: String = "normal",
    level: String = CompressionLevel.MEDIUM,
    slowNetwork: Boolean = false,
    apiKey: String = "",
    onPartial: suspend (SaylatArticle) -> Unit = {},
): SaylatArticle = ArticleFetcher.fetchArticle(
    context, client, this, baseUrl, url, images, level, slowNetwork, apiKey, onPartial,
)

/** @deprecated Используйте fetchArticle с контекстом — сохранено для layout lab. */
suspend fun SaylatApi.fetchArticleLegacy(
    url: String,
    images: String = "normal",
    level: String = CompressionLevel.MEDIUM,
): SaylatArticle {
    runCatching {
        val binary = extractBinary(url, images, level)
        if (binary.isSuccessful) {
            return PayloadCodec.decompressBinaryResponse(binary)
        }
    }
    return PayloadCodec.expandArticle(extract(url, images, level))
}

suspend fun SaylatApi.fetchOpen(
    context: Context,
    client: OkHttpClient,
    baseUrl: String,
    request: OpenRequest,
    slowNetwork: Boolean = false,
    apiKey: String = "",
    onPartial: suspend (SaylatArticle) -> Unit = {},
): OpenResponse {
    if (request.target == "url" && !request.url.isNullOrBlank()) {
        val article = ArticleFetcher.fetchOpenArticle(
            context, this, request, slowNetwork, baseUrl, client, apiKey, onPartial,
        )
        return OpenResponse(kind = "article", article = article)
    }
    return PayloadCodec.expandOpenResponse(open(request))
}

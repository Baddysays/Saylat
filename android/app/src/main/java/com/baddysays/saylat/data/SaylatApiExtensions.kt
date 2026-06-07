package com.baddysays.saylat.data

import retrofit2.Response

/**
 * Загрузка статей: сначала бинарный gzip (меньше трафика), затем JSON-envelope.
 */
suspend fun SaylatApi.fetchArticle(
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

suspend fun SaylatApi.fetchOpen(request: OpenRequest): OpenResponse {
    if (request.target == "url" && !request.url.isNullOrBlank()) {
        runCatching {
            val binary = openBinary(request)
            if (binary.isSuccessful) {
                val article = PayloadCodec.decompressBinaryResponse(binary)
                return OpenResponse(kind = "article", article = article)
            }
        }
    }
    return PayloadCodec.expandOpenResponse(open(request))
}

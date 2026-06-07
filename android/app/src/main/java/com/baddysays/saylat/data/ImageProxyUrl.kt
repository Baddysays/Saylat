package com.baddysays.saylat.data

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * В режиме images=refs в статье только HTTPS-ссылки.
 * Прокси на Saylat нужен, чтобы подставить Referer и отдать мини-JPEG с кэшем.
 */
object ImageProxyUrl {
    fun resolve(
        serverBase: String,
        imageUrl: String?,
        pageUrl: String?,
        useProxy: Boolean = true,
    ): Any? {
        val src = imageUrl?.trim().orEmpty()
        if (src.isEmpty()) return null
        if (!useProxy || src.startsWith("data:") || src.contains("/api/image")) {
            return src
        }
        if (!src.startsWith("http://") && !src.startsWith("https://")) {
            return src
        }
        val base = serverBase.trim().trimEnd('/')
        if (base.isEmpty()) return src
        val page = pageUrl?.trim().takeIf { it?.startsWith("http") == true } ?: src
        val q = buildString {
            append("url=")
            append(URLEncoder.encode(src, StandardCharsets.UTF_8.name()))
            append("&page_url=")
            append(URLEncoder.encode(page, StandardCharsets.UTF_8.name()))
            append("&tiny=true")
        }
        return "$base/api/image?$q"
    }

    fun shouldProxy(imageUrl: String?): Boolean {
        val src = imageUrl?.trim().orEmpty()
        return src.startsWith("http://") || src.startsWith("https://")
    }
}

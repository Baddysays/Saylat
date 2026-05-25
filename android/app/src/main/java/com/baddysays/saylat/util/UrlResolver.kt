package com.baddysays.saylat.util

import java.net.URI

object UrlResolver {
    fun resolve(href: String, pageUrl: String?): String {
        val h = href.trim()
        if (h.startsWith("http://", ignoreCase = true) ||
            h.startsWith("https://", ignoreCase = true) ||
            h.startsWith("mailto:", ignoreCase = true) ||
            h.startsWith("tel:", ignoreCase = true)
        ) {
            return h
        }
        if (h.startsWith("//")) return "https:$h"
        val base = pageUrl?.trim().orEmpty()
        if (base.isEmpty()) return h
        return try {
            URI(base).resolve(h).toString()
        } catch (_: Exception) {
            val slash = if (base.endsWith("/")) "" else "/"
            base + slash + h.removePrefix("/")
        }
    }
}

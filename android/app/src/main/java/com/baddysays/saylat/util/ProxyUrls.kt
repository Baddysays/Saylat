package com.baddysays.saylat.util

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object ProxyUrls {
    fun page(base: String, target: String): String {
        val root = base.trim().trimEnd('/')
        val normalized = SaylatUserAgents.normalizeFetchUrl(target)
        val encoded = URLEncoder.encode(normalized, StandardCharsets.UTF_8.name())
        return "$root/api/proxy/page?url=$encoded"
    }
}

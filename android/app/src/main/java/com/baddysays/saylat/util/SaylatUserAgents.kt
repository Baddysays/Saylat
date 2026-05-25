package com.baddysays.saylat.util

import android.net.Uri

object SaylatUserAgents {
    const val CHROME_DESKTOP =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    const val CHROME_MOBILE =
        "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.6778.135 Mobile Safari/537.36"

    fun forUrl(url: String): String {
        val host = Uri.parse(url).host?.lowercase().orEmpty()
        if (host.contains("vk.com") || host.contains("vk.ru")) {
            return CHROME_DESKTOP
        }
        if (host.contains("pikabu.ru")) {
            return CHROME_MOBILE
        }
        return CHROME_DESKTOP
    }

    /** m.vk.com отдаёт «браузер устарел» — открываем vk.com. */
    fun normalizeFetchUrl(url: String): String {
        val trimmed = url.trim()
        return when {
            trimmed.contains("://m.vk.com") ->
                trimmed.replace("://m.vk.com", "://vk.com")
            trimmed.contains("://m.vk.ru") ->
                trimmed.replace("://m.vk.ru", "://vk.ru")
            else -> trimmed
        }
    }
}

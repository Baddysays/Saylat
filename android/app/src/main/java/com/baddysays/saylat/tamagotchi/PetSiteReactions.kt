package com.baddysays.saylat.tamagotchi

import com.baddysays.saylat.prefs.AppLanguage
import java.util.Calendar

object PetSiteReactions {

    fun forHost(host: String): String? {
        val h = host.lowercase()
        return when {
            "github" in h -> "GitHub! Код! Моё всё!"
            "wikipedia" in h -> "Wikipedia! Люблю знания!"
            "habr" in h -> "Habr! Тут умные люди!"
            "youtube" in h -> "YouTube? Загружу текстом!"
            "reddit" in h -> "Reddit! Много мнений!"
            "pikabu" in h -> "Пикабу? Весело!"
            "vk.com" in h -> "ВКонтакте! Загружаю…"
            "lenta.ru" in h -> "Lenta.ru — на связи!"
            "rbc.ru" in h -> "РБК — деловые новости!"
            "medium" in h -> "Medium! Знаю как сжать!"
            else -> null
        }
    }

    fun hostFromUrl(url: String): String? =
        runCatching { java.net.URI(url).host }.getOrNull()

    fun lineForUrl(url: String, lang: AppLanguage): String? {
        val host = hostFromUrl(url) ?: return null
        return lineForHost(host, lang)
    }

    fun lineForHost(host: String, lang: AppLanguage): String? {
        val ru = forHost(host) ?: return null
        if (lang == AppLanguage.RU) return ru
        return enForHost(host)
    }

    private fun enForHost(host: String): String {
        val h = host.lowercase()
        return when {
            "github" in h -> "GitHub! Code! Love it!"
            "wikipedia" in h -> "Wikipedia! Knowledge!"
            "habr" in h -> "Habr! Smart folks!"
            "youtube" in h -> "YouTube? I'll load the text!"
            "reddit" in h -> "Reddit! Many opinions!"
            "pikabu" in h -> "Pikabu? Fun!"
            "vk.com" in h -> "VK… loading…"
            "lenta.ru" in h -> "Lenta.ru — online!"
            "rbc.ru" in h -> "RBC — business news!"
            "medium" in h -> "Medium! I know how to compress!"
            else -> forHost(host) ?: ""
        }
    }

    fun isNight(): Boolean {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return h >= 23 || h <= 5
    }

    fun isMorning(): Boolean {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return h in 6..10
    }

    fun formatBytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes Б"
        bytes < 1_048_576 -> "${bytes / 1024} КБ"
        else -> "%.1f МБ".format(bytes / 1_048_576.0)
    }
}

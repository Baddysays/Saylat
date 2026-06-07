package com.baddysays.saylat.util

import com.baddysays.saylat.data.ArticleStats

data class PageLoadBanner(
    val headline: String,
    val comparison: String,
    val detail: String? = null,
)

object PageLoadStats {

    fun savingsPercent(originalBytes: Int, payloadBytes: Int): Int? {
        if (originalBytes <= 0 || payloadBytes <= 0 || payloadBytes >= originalBytes) return null
        return ((originalBytes - payloadBytes) * 100L / originalBytes).toInt().coerceIn(1, 99)
    }

    fun formatBytes(n: Int): String = when {
        n >= 1_000_000 -> String.format("%.1f МБ", n / 1_000_000f)
        n >= 1_000 -> String.format("%.1f КБ", n / 1_000f)
        else -> "$n Б"
    }

    fun fromStats(stats: ArticleStats): PageLoadBanner {
        val original = stats.original_bytes
        val payload = stats.payload_bytes
        val wire = stats.wire_bytes
        val received = if (wire > 0) wire else payload
        val pct = savingsPercent(original, received)
        val headline = buildString {
            append("Лента: ")
            append(formatBytes(received))
            if (wire > 0 && payload > wire) {
                append(" (распак. ")
                append(formatBytes(payload))
                append(")")
            }
            if (original > 0) {
                append(" · оригинал ")
                append(formatBytes(original))
            }
        }
        val comparison = when {
            pct != null -> "На $pct% меньше оригинала · ${stats.fetch_ms} мс на сервере"
            original > 0 && received > 0 -> "Загрузка ${stats.fetch_ms} мс · без сжатия по объёму"
            stats.fetch_ms > 0 -> "Обработка на сервере: ${stats.fetch_ms} мс"
            else -> "Страница готова"
        }
        val detail = buildList {
            if (stats.images_omitted > 0) {
                add("Картинок не грузили: ${stats.images_omitted} (макет сохранён)")
            }
            if (stats.images_inlined > 0) add("JPEG с сервера: ${stats.images_inlined}")
            if (wire > 0) add("По сети (сжато): ${formatBytes(wire)}")
            if (original > 0 && received > 0) {
                val saved = (original - received).coerceAtLeast(0)
                if (saved > 0) add("Экономия трафика: ${formatBytes(saved)}")
            }
        }.joinToString(" · ").ifBlank { null }
        return PageLoadBanner(headline = headline, comparison = comparison, detail = detail)
    }
}

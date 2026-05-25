package com.baddysays.saylat.search

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Поиск всегда идёт через прокси Saylat (DuckDuckGo + Wikipedia + SearXNG). */
enum class SearchEngine(
    val id: String,
    val label: String,
    val description: String,
) {
    SEARXNG(
        id = "searxng",
        label = "Saylat",
        description = "Веб-выдача через VPS: DuckDuckGo, Wikipedia, при возможности SearXNG",
    ),
    ;

    fun buildSearchPageUrl(query: String, searxInstance: String): String {
        val encoded = query.urlEncoded()
        val base = searxInstance.trimEnd('/')
        return "$base/search?q=$encoded"
    }

    companion object {
        fun fromId(id: String): SearchEngine {
            val normalized = id.trim().lowercase()
            if (normalized in REMOVED_ENGINE_IDS) return SEARXNG
            return entries.firstOrNull { it.id == normalized } ?: SEARXNG
        }

        /** Старые сохранённые движки — сбрасываем на Saylat. */
        private val REMOVED_ENGINE_IDS = setOf("metager", "mojeek", "ddg_lite", "duckduckgo_lite")
    }
}

private fun String.urlEncoded(): String =
    URLEncoder.encode(this, StandardCharsets.UTF_8.name())

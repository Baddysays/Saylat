package com.baddysays.saylat.search

import com.baddysays.saylat.data.ApiFactory

class SearchRepository {
    suspend fun search(
        query: String,
        engine: SearchEngine,
        searxInstance: String,
        proxyBaseUrl: String? = null,
        slowNetwork: Boolean = false,
    ): List<SearchHit> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return emptyList()

        val proxy = proxyBaseUrl?.trim().orEmpty()
        if (!proxy.startsWith("http")) {
            throw IllegalArgumentException(
                "Укажите адрес вашего Saylat-сервера в настройках (http://IP:8787)",
            )
        }

        val api = ApiFactory.create(proxy, slowNetwork = slowNetwork)
        return api.search(trimmed, engine.id).results.map { it.toHit() }
    }
}

private fun com.baddysays.saylat.data.ProxySearchHit.toHit(): SearchHit = SearchHit(
    title = title.ifBlank { url },
    url = url,
    snippet = snippet,
    source = source,
)

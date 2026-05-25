package com.baddysays.saylat.search

import com.squareup.moshi.Json

data class SearchHit(
    val title: String,
    val url: String,
    val snippet: String,
    val source: String? = null,
)

data class SearxResponse(
    val results: List<SearxResult> = emptyList(),
)

data class SearxResult(
    val title: String? = null,
    val url: String? = null,
    val content: String? = null,
    val engine: String? = null,
)

fun SearxResult.toHit(): SearchHit? {
    val link = url?.trim().orEmpty()
    if (link.isBlank() || !link.startsWith("http")) return null
    return SearchHit(
        title = title?.trim().orEmpty().ifBlank { link },
        url = link,
        snippet = content?.trim().orEmpty(),
        source = engine,
    )
}

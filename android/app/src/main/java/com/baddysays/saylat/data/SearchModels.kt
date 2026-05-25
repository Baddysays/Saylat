package com.baddysays.saylat.data

import com.squareup.moshi.Json

data class ProxySearchResponse(
    val query: String = "",
    val engine: String = "searxng",
    val results: List<ProxySearchHit> = emptyList(),
    val fetch_ms: Int = 0,
)

data class ProxySearchHit(
    val title: String = "",
    val url: String = "",
    val snippet: String = "",
    val source: String? = null,
)

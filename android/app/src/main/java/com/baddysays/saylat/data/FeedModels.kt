package com.baddysays.saylat.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SaylatFeed(
    val source: String = "",
    val title: String = "",
    val subtitle: String = "",
    val context_id: String = "",
    val items: List<FeedItem> = emptyList(),
    val stats: FeedStats = FeedStats(),
    val has_more: Boolean = false,
    val total_items: Int = 0,
)

@JsonClass(generateAdapter = true)
data class FeedItem(
    val id: String,
    val kind: String = "message",
    val title: String,
    @Json(name = "from") val from: String? = null,
    val time: String = "",
    val body: String = "",
    val unread: Boolean = false,
    val href: String? = null,
    val thumb: String? = null,
    val actions: List<String> = listOf("open"),
)

@JsonClass(generateAdapter = true)
data class FeedStats(
    val payload_bytes: Int = 0,
    val fetch_ms: Int = 0,
)

@JsonClass(generateAdapter = true)
data class OpenRequest(
    val target: String = "url",
    val url: String? = null,
    val resource_id: String? = null,
    val images: String = "normal",
    val level: String = "medium",
)

@JsonClass(generateAdapter = true)
data class OpenResponse(
    val kind: String = "article",
    val article: SaylatArticle? = null,
    val feed: SaylatFeed? = null,
    val wire: WireCompressedPayload? = null,
)

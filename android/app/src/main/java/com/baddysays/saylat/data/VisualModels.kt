package com.baddysays.saylat.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VisualTile(
    val kind: String = "paragraph",
    val text: String = "",
    val level: Int? = null,
    val src: String? = null,
    val alt: String = "",
    val width: Int? = null,
    val height: Int? = null,
    val items: List<String>? = null,
    val href: String? = null,
    val bytes_approx: Int = 0,
)

@JsonClass(generateAdapter = true)
data class VisualStats(
    val original_bytes: Int = 0,
    val payload_bytes: Int = 0,
    val fetch_ms: Int = 0,
    val build_ms: Int = 0,
    val images_inlined: Int = 0,
    val image_bytes_approx: Int = 0,
)

@JsonClass(generateAdapter = true)
data class VisualPage(
    val url: String,
    val title: String,
    val excerpt: String = "",
    val lang: String = "",
    val tiles: List<VisualTile> = emptyList(),
    val structure_hint: String = "article",
    val stats: VisualStats = VisualStats(),
)

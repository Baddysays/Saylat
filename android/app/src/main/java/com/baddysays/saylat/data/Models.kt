package com.baddysays.saylat.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ArticleLink(
    val text: String,
    val href: String,
)

@JsonClass(generateAdapter = true)
data class CssHints(
    val primary_color: String? = null,
    val background_color: String? = null,
    val body_font_size_sp: Float? = null,
    val heading_color: String? = null,
)

@JsonClass(generateAdapter = true)
data class SaylatArticle(
    val url: String,
    val title: String,
    val excerpt: String = "",
    val byline: String = "",
    val lang: String = "",
    val blocks: List<Block> = emptyList(),
    val stats: ArticleStats = ArticleStats(),
    val layout_hint: String = "article",
    val site_profile: String = "generic",
    val compression_level: String = "medium",
    val plain_text: String = "",
    val links: List<ArticleLink> = emptyList(),
    val css_hints: CssHints? = null,
)

@JsonClass(generateAdapter = true)
data class StripSegment(
    val index: Int = 0,
    val src: String = "",
    val width: Int = 360,
    val height: Int = 0,
    val bytes_approx: Int = 0,
)

@JsonClass(generateAdapter = true)
data class StripStats(
    val original_bytes: Int = 0,
    val payload_bytes: Int = 0,
    val strip_count: Int = 0,
    val fetch_ms: Int = 0,
    val build_ms: Int = 0,
)

@JsonClass(generateAdapter = true)
data class StripPage(
    val url: String,
    val title: String = "",
    val site_profile: String = "generic",
    val strips: List<StripSegment> = emptyList(),
    val strip_width: Int = 360,
    val render_engine: String = "pillow",
    val stats: StripStats = StripStats(),
)

@JsonClass(generateAdapter = true)
data class TextSpan(
    val text: String,
    val href: String? = null,
)

@JsonClass(generateAdapter = true)
data class Block(
    val type: String,
    val text: String? = null,
    val level: Int? = null,
    val src: String? = null,
    val alt: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val items: List<String>? = null,
    val spans: List<TextSpan>? = null,
    val href: String? = null,
)

@JsonClass(generateAdapter = true)
data class TranslateRequest(
    val texts: List<String>,
    val source: String = "auto",
    val target: String = "ru",
)

@JsonClass(generateAdapter = true)
data class TranslateResponse(
    val translations: List<String> = emptyList(),
    val source: String = "auto",
    val target: String = "ru",
    val provider: String = "mymemory",
    val fetch_ms: Int = 0,
)

@JsonClass(generateAdapter = true)
data class ArticleStats(
    val original_bytes: Int = 0,
    val payload_bytes: Int = 0,
    val images_inlined: Int = 0,
    val images_omitted: Int = 0,
    val fetch_ms: Int = 0,
)

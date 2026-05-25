package com.baddysays.saylat.data


/** Конвертация визуальной копии в SaylatArticle для умной вёрстки на устройстве. */
object VisualMapper {
    fun toArticle(page: VisualPage): SaylatArticle {
        val blocks = page.tiles.mapNotNull { tile ->
            when (tile.kind) {
                "heading" -> Block(
                    type = "heading",
                    text = tile.text,
                    level = tile.level ?: 2,
                )
                "paragraph" -> Block(type = "paragraph", text = tile.text)
                "quote" -> Block(type = "quote", text = tile.text)
                "list" -> Block(type = "list", items = tile.items ?: emptyList())
                "divider" -> Block(type = "divider")
                "link" -> Block(type = "link", text = tile.text, href = tile.href)
                "image" -> Block(
                    type = "image",
                    src = tile.src,
                    alt = tile.alt,
                    width = tile.width,
                    height = tile.height,
                )
                else -> null
            }
        }
        return SaylatArticle(
            url = page.url,
            title = page.title,
            excerpt = page.excerpt,
            lang = page.lang,
            blocks = blocks,
            layout_hint = page.structure_hint,
            stats = ArticleStats(
                original_bytes = page.stats.original_bytes,
                payload_bytes = page.stats.payload_bytes,
                images_inlined = page.stats.images_inlined,
                images_omitted = page.tiles.count { it.kind == "image" && it.src.isNullOrBlank() },
                fetch_ms = page.stats.fetch_ms + page.stats.build_ms,
            ),
        )
    }

    fun looksEmpty(page: VisualPage): Boolean {
        if (page.tiles.isEmpty()) return true
        if (page.tiles.size > 2) return false
        val text = page.tiles
            .filter { it.kind == "paragraph" }
            .joinToString(" ") { it.text }
            .lowercase()
        return text.contains("пуста") || text.contains("не удалось")
    }
}

package com.baddysays.saylat.engine

import com.baddysays.saylat.data.Block
import com.baddysays.saylat.data.SaylatArticle
import com.baddysays.saylat.util.PageLoadStats

object LayoutPlanRenderer {

    fun render(article: SaylatArticle, plan: LayoutPlan, includeStatsCard: Boolean = false): RenderPlan {
        val mergedTexts = buildMergedParagraphs(article.blocks, plan.mergeParagraphGroups)
        val consumed = plan.mergeParagraphGroups.flatten().toSet()

        val cards = mutableListOf<RenderCard>()
        cards += RenderCard(
            kind = CardKind.HERO,
            title = article.title,
            body = plan.heroExcerpt,
            meta = article.byline.ifBlank { hostLabel(article.url) },
            emphasis = true,
        )

        var imageShown = 0
        val maxImages = when (plan.source) {
            LayoutPlanSource.AI_PROTOTYPE,
            LayoutPlanSource.AI_GEMMA,
            -> Int.MAX_VALUE
            LayoutPlanSource.HEURISTIC -> maxImages(plan.density)
        }

        for (idx in plan.blockOrder) {
            if (idx in plan.hideBlockIds) continue
            val block = article.blocks.getOrNull(idx) ?: continue

            if (idx in consumed) continue

            val mergeText = mergedTexts[idx]
            if (mergeText != null) {
                renderParagraphChunks(mergeText, plan.density, cards)
                continue
            }

            when (block.type) {
                "heading" -> cards += RenderCard(
                    kind = CardKind.HEADING,
                    title = block.text.orEmpty(),
                    meta = "H${block.level ?: 2}",
                    spans = block.spans?.takeIf { s -> s.any { !it.href.isNullOrBlank() } },
                )
                "paragraph" -> {
                    val spans = block.spans
                    if (!spans.isNullOrEmpty() && spans.any { !it.href.isNullOrBlank() }) {
                        cards += RenderCard(
                            kind = CardKind.BODY,
                            body = block.text.orEmpty(),
                            spans = spans,
                        )
                    } else {
                        renderParagraphChunks(block.text.orEmpty(), plan.density, cards)
                    }
                }
                "link" -> {
                    val href = block.href.orEmpty()
                    if (href.isNotBlank()) {
                        cards += RenderCard(
                            kind = CardKind.LINK,
                            title = block.text.orEmpty().ifBlank { href },
                            linkHref = href,
                        )
                    }
                }
                "quote" -> cards += RenderCard(
                    kind = CardKind.QUOTE,
                    body = block.text.orEmpty(),
                )
                "list" -> cards += RenderCard(
                    kind = CardKind.LIST,
                    body = (block.items ?: emptyList()).joinToString("\n") { "• $it" },
                )
                "image" -> {
                    val placeholder = block.src.isNullOrBlank()
                    if (placeholder || imageShown < maxImages) {
                        cards += RenderCard(
                            kind = CardKind.IMAGE,
                            imageSrc = block.src,
                            meta = block.alt.orEmpty().ifBlank { "Изображение" },
                            imagePlaceholder = placeholder,
                        )
                        if (!placeholder) imageShown++
                    }
                }
            }
        }

        val saved = article.stats.original_bytes - article.stats.payload_bytes
        val savingsLabel = if (saved > 0) {
            "−${PageLoadStats.formatBytes(saved)} (${article.stats.fetch_ms} ms)"
        } else {
            "${article.stats.fetch_ms} ms"
        }

        if (includeStatsCard) {
            val sourceLabel = when (plan.source) {
                LayoutPlanSource.HEURISTIC -> "Быстрая вёрстка"
                LayoutPlanSource.AI_PROTOTYPE -> "Умная вёрстка (прототип)"
                LayoutPlanSource.AI_GEMMA -> "Умная вёрстка (Gemma)"
            }
            cards += RenderCard(
                kind = CardKind.STATS,
                body = "Получено ${PageLoadStats.formatBytes(article.stats.payload_bytes)} · $sourceLabel",
                meta = savingsLabel,
            )
        }

        return RenderPlan(
            density = plan.density,
            cards = cards,
            savingsLabel = savingsLabel,
            layoutSource = plan.source,
        )
    }

    private fun buildMergedParagraphs(blocks: List<Block>, groups: List<List<Int>>): Map<Int, String> {
        val map = mutableMapOf<Int, String>()
        for (group in groups) {
            if (group.isEmpty()) continue
            val text = group
                .mapNotNull { blocks.getOrNull(it)?.text?.trim() }
                .filter { it.isNotEmpty() }
                .joinToString("\n\n")
            map[group.first()] = text
        }
        return map
    }

    private fun renderParagraphChunks(text: String, density: Density, cards: MutableList<RenderCard>) {
        splitForNarrow(text, density).forEach { chunk ->
            if (chunk.isNotBlank()) {
                cards += RenderCard(kind = CardKind.BODY, body = chunk)
            }
        }
    }

    private fun splitForNarrow(text: String, density: Density): List<String> {
        val limit = when (density) {
            Density.COMPACT -> 420
            Density.COMFORTABLE -> 680
            Density.AIRY -> 900
        }
        if (text.length <= limit) return listOf(text)
        return text.split(Regex("(?<=[.!?])\\s+"))
            .fold(mutableListOf<String>()) { acc, sentence ->
                if (acc.isEmpty()) {
                    acc.add(sentence)
                } else if ((acc.last().length + sentence.length) < limit) {
                    acc[acc.lastIndex] = acc.last() + " " + sentence
                } else {
                    acc.add(sentence)
                }
                acc
            }
    }

    private fun maxImages(density: Density) = when (density) {
        Density.COMPACT -> 2
        Density.COMFORTABLE -> 4
        Density.AIRY -> 8
    }

    private fun hostLabel(url: String): String =
        runCatching { android.net.Uri.parse(url).host }.getOrNull() ?: url

}

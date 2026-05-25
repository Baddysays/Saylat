package com.baddysays.saylat.engine

import com.baddysays.saylat.data.Block
import com.baddysays.saylat.data.SaylatArticle

/** Базовый план без LLM — мгновенно. */
object HeuristicLayoutEnhancer : LayoutEnhancer {
    override val source = LayoutPlanSource.HEURISTIC

    override suspend fun enhance(article: SaylatArticle, baseline: LayoutPlan): LayoutPlan =
        produce(article)

    fun produce(article: SaylatArticle): LayoutPlan {
        val density = when (article.layout_hint) {
            "minimal" -> Density.COMPACT
            "gallery" -> Density.AIRY
            "feed" -> Density.COMFORTABLE
            else -> Density.COMFORTABLE
        }
        val order = article.blocks.indices.toList()
        return LayoutPlan(
            layoutHint = article.layout_hint.ifBlank { "article" },
            heroExcerpt = article.excerpt.ifBlank { smartExcerpt(article.blocks) },
            density = density,
            blockOrder = order,
            source = source,
        )
    }

    private fun smartExcerpt(blocks: List<Block>): String =
        blocks
            .firstOrNull { it.type == "paragraph" && (it.text?.length ?: 0) > 40 }
            ?.text
            ?.take(180)
            .orEmpty()
}

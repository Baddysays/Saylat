package com.baddysays.saylat.engine

import com.baddysays.saylat.data.Block
import com.baddysays.saylat.data.SaylatArticle
import kotlinx.coroutines.delay

/**
 * Прототип «локального ИИ»: эвристики второго прохода + задержка как у модели.
 * Заменяется на Gemma 3 1B + LiteRT-LM без смены [LayoutEnhancer].
 *
 * Важно: умная вёрстка не должна выкидывать контент — только улучшать читаемость.
 */
class PrototypeAiLayoutEnhancer(
    private val simulatedLatencyMs: Long = 0,
) : LayoutEnhancer {
    override val source = LayoutPlanSource.AI_PROTOTYPE

    override suspend fun enhance(article: SaylatArticle, baseline: LayoutPlan): LayoutPlan {
        if (simulatedLatencyMs > 0) delay(simulatedLatencyMs)
        return refine(article, baseline)
    }

    private fun refine(article: SaylatArticle, baseline: LayoutPlan): LayoutPlan {
        val blocks = article.blocks
        val mergeGroups = buildMergeGroups(blocks)

        val density = when {
            blocks.size > 20 -> Density.COMFORTABLE
            blocks.count { it.type == "image" } >= 6 -> Density.COMFORTABLE
            baseline.density == Density.AIRY -> Density.AIRY
            blocks.size > 10 -> Density.COMFORTABLE
            else -> baseline.density
        }

        return baseline.copy(
            heroExcerpt = buildHeroExcerpt(blocks, baseline.heroExcerpt),
            density = density,
            blockOrder = baseline.blockOrder,
            hideBlockIds = emptySet(),
            mergeParagraphGroups = mergeGroups,
            source = source,
        )
    }

    /** Склеиваем только короткие соседние абзацы; текст остаётся в ленте через merge. */
    private fun buildMergeGroups(blocks: List<Block>): List<List<Int>> {
        val groups = mutableListOf<List<Int>>()
        var i = 0
        while (i < blocks.size) {
            if (blocks[i].type != "paragraph") {
                i++
                continue
            }
            val firstLen = blocks[i].text?.trim()?.length ?: 0
            if (firstLen >= 80) {
                i++
                continue
            }
            val group = mutableListOf(i)
            var total = firstLen
            var j = i + 1
            while (j < blocks.size && blocks[j].type == "paragraph") {
                val len = blocks[j].text?.trim()?.length ?: 0
                if (len == 0 || total + len > 280) break
                if (len >= 120) break
                group.add(j)
                total += len
                j++
            }
            if (group.size > 1) groups += group
            i = if (group.size > 1) j else i + 1
        }
        return groups
    }

    private fun buildHeroExcerpt(blocks: List<Block>, fallback: String): String {
        val parts = blocks
            .filter { it.type == "paragraph" }
            .mapNotNull { it.text?.trim() }
            .filter { it.length > 30 }
        if (parts.isEmpty()) return fallback
        val joined = parts.take(2).joinToString(" ")
        return joined.take(220).ifBlank { fallback }
    }
}

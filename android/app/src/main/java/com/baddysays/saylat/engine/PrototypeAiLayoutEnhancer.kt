package com.baddysays.saylat.engine

import com.baddysays.saylat.data.Block
import com.baddysays.saylat.data.SaylatArticle
import kotlinx.coroutines.delay

/**
 * Прототип «локального ИИ»: эвристики второго прохода + задержка как у модели.
 * Заменяется на Gemma 3 1B + LiteRT-LM без смены [LayoutEnhancer].
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
        val hide = mutableSetOf<Int>()
        val mergeGroups = mutableListOf<List<Int>>()

        blocks.forEachIndexed { i, block ->
            if (block.type == "paragraph") {
                val len = block.text?.length ?: 0
                if (len in 1..24) hide.add(i)
            }
        }

        var i = 0
        while (i < blocks.size) {
            if (blocks[i].type == "paragraph" && i !in hide) {
                val group = mutableListOf(i)
                var j = i + 1
                var total = blocks[i].text?.length ?: 0
                while (j < blocks.size && blocks[j].type == "paragraph" && j !in hide) {
                    val len = blocks[j].text?.length ?: 0
                    if (total + len > 320) break
                    if (len < 120) {
                        group.add(j)
                        hide.add(j)
                        total += len
                        j++
                    } else break
                }
                if (group.size > 1) mergeGroups += group
                i = j
            } else i++
        }

        val textFirst = mutableListOf<Int>()
        val images = mutableListOf<Int>()
        val other = mutableListOf<Int>()
        baseline.blockOrder.forEach { idx ->
            if (idx in hide) return@forEach
            when (blocks.getOrNull(idx)?.type) {
                "image" -> images += idx
                "heading", "paragraph", "quote", "list" -> textFirst += idx
                else -> other += idx
            }
        }

        val density = when {
            blocks.size > 14 -> Density.COMPACT
            blocks.count { it.type == "image" } >= 4 -> Density.COMPACT
            baseline.density == Density.AIRY -> Density.AIRY
            else -> baseline.density
        }

        val maxImages = when (density) {
            Density.COMPACT -> 2
            Density.COMFORTABLE -> 4
            Density.AIRY -> 8
        }
        val keptImages = images.take(maxImages)
        val droppedImages = images.drop(maxImages)
        hide.addAll(droppedImages)

        val blockOrder = textFirst + other + keptImages
        val hero = buildHeroExcerpt(article, blocks, baseline.heroExcerpt)

        return baseline.copy(
            heroExcerpt = hero,
            density = density,
            blockOrder = blockOrder,
            hideBlockIds = hide,
            mergeParagraphGroups = mergeGroups,
            source = source,
        )
    }

    private fun buildHeroExcerpt(article: SaylatArticle, blocks: List<Block>, fallback: String): String {
        val parts = blocks
            .filter { it.type == "paragraph" }
            .mapNotNull { it.text?.trim() }
            .filter { it.length > 30 }
        if (parts.isEmpty()) return fallback
        val joined = parts.take(2).joinToString(" ")
        return joined.take(220).ifBlank { fallback }
    }
}

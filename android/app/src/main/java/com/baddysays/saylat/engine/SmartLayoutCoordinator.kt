package com.baddysays.saylat.engine

import com.baddysays.saylat.data.SaylatArticle

/**
 * Двухфазная сборка: сначала эвристика, опционально — улучшение (прототип / будущая Gemma).
 */
object SmartLayoutCoordinator {

    private val prototypeAi = PrototypeAiLayoutEnhancer()

    fun quickRender(article: SaylatArticle): RenderPlan {
        val plan = HeuristicLayoutEnhancer.produce(article)
        return LayoutPlanRenderer.render(article, plan)
    }

    suspend fun renderWithSmartLayout(article: SaylatArticle, useAi: Boolean): RenderPlan {
        val baseline = HeuristicLayoutEnhancer.produce(article)
        if (!useAi) {
            return LayoutPlanRenderer.render(article, baseline)
        }
        val refined = prototypeAi.enhance(article, baseline)
        return LayoutPlanRenderer.render(article, refined)
    }
}

package com.baddysays.saylat.ai

import com.baddysays.saylat.data.SaylatArticle
import com.baddysays.saylat.engine.LayoutEnhancer
import com.baddysays.saylat.engine.LayoutPlan
import com.baddysays.saylat.engine.LayoutPlanSource
import com.baddysays.saylat.engine.PrototypeAiLayoutEnhancer

/**
 * Заглушка под Gemma 3 1B + LiteRT-LM.
 * Сейчас делегирует [PrototypeAiLayoutEnhancer]; заменить тело `enhance` на вызов LLM.
 */
class GemmaLayoutEnhancer : LayoutEnhancer {
    override val source = LayoutPlanSource.AI_GEMMA

    private val fallback = PrototypeAiLayoutEnhancer(simulatedLatencyMs = 0)

    override suspend fun enhance(article: SaylatArticle, baseline: LayoutPlan): LayoutPlan {
        // TODO: LiteRT-LM Engine + prompt → JSON LayoutPlan
        return fallback.enhance(article, baseline).copy(source = source)
    }
}

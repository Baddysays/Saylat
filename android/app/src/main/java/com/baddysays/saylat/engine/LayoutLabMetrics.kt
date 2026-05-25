package com.baddysays.saylat.engine

import com.baddysays.saylat.data.SaylatArticle

data class LayoutLabResult(
    val url: String,
    val title: String,
    val baselineCards: Int,
    val smartCards: Int,
    val baselineHiddenBlocks: Int,
    val smartHiddenBlocks: Int,
    val baselineMergedGroups: Int,
    val smartMergedGroups: Int,
    val smartSourceLabel: String,
    val fetchMs: Int,
)

data class LayoutLabComparison(
    val metrics: LayoutLabResult,
    val baseline: RenderPlan,
    val smart: RenderPlan,
    val article: SaylatArticle,
)

object LayoutLabMetrics {

    suspend fun compare(article: SaylatArticle): LayoutLabComparison {
        val baselinePlan = HeuristicLayoutEnhancer.produce(article)
        val smartPlan = PrototypeAiLayoutEnhancer().enhance(article, baselinePlan)
        val metrics = LayoutLabResult(
            url = article.url,
            title = article.title,
            baselineCards = LayoutPlanRenderer.render(article, baselinePlan).cards.size,
            smartCards = LayoutPlanRenderer.render(article, smartPlan).cards.size,
            baselineHiddenBlocks = baselinePlan.hideBlockIds.size,
            smartHiddenBlocks = smartPlan.hideBlockIds.size,
            baselineMergedGroups = baselinePlan.mergeParagraphGroups.size,
            smartMergedGroups = smartPlan.mergeParagraphGroups.size,
            smartSourceLabel = when (smartPlan.source) {
                LayoutPlanSource.HEURISTIC -> "Эвристика"
                LayoutPlanSource.AI_PROTOTYPE -> "Прототип ИИ"
                LayoutPlanSource.AI_GEMMA -> "Gemma"
            },
            fetchMs = article.stats.fetch_ms,
        )
        return LayoutLabComparison(
            metrics = metrics,
            baseline = LayoutPlanRenderer.render(article, baselinePlan),
            smart = LayoutPlanRenderer.render(article, smartPlan),
            article = article,
        )
    }
}

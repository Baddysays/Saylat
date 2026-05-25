package com.baddysays.saylat.engine

enum class LayoutPlanSource {
    HEURISTIC,
    AI_PROTOTYPE,
    AI_GEMMA,
}

/**
 * План отображения: метки блоков после пережатия → как рисовать ленту.
 * Совместим с [shared/layout-plan.schema.json].
 */
data class LayoutPlan(
    val layoutHint: String,
    val heroExcerpt: String,
    val density: Density,
    val blockOrder: List<Int>,
    val hideBlockIds: Set<Int> = emptySet(),
    val mergeParagraphGroups: List<List<Int>> = emptyList(),
    val source: LayoutPlanSource = LayoutPlanSource.HEURISTIC,
)

package com.baddysays.saylat.engine

import com.baddysays.saylat.data.SaylatArticle
import com.baddysays.saylat.data.TextSpan

data class RenderCard(
    val kind: CardKind,
    val title: String = "",
    val body: String = "",
    val imageSrc: String? = null,
    val imagePlaceholder: Boolean = false,
    val meta: String = "",
    val emphasis: Boolean = false,
    val spans: List<TextSpan>? = null,
    val linkHref: String? = null,
)

enum class CardKind {
    HERO,
    HEADING,
    BODY,
    QUOTE,
    LIST,
    IMAGE,
    LINK,
    STATS,
}

data class RenderPlan(
    val density: Density,
    val cards: List<RenderCard>,
    val savingsLabel: String,
    val layoutSource: LayoutPlanSource = LayoutPlanSource.HEURISTIC,
)

enum class Density { COMPACT, COMFORTABLE, AIRY }

/** Совместимость: быстрый рендер через [SmartLayoutCoordinator]. */
object ArticleLayoutEngine {
    fun plan(article: SaylatArticle): RenderPlan =
        SmartLayoutCoordinator.quickRender(article)
}

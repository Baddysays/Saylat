package com.baddysays.saylat.engine

import com.baddysays.saylat.data.SaylatArticle

interface LayoutEnhancer {
    val source: LayoutPlanSource
    suspend fun enhance(article: SaylatArticle, baseline: LayoutPlan): LayoutPlan
}

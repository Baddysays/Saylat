package com.baddysays.saylat.ui

import com.baddysays.saylat.data.ArticleStats
import com.baddysays.saylat.data.SaylatArticle
import com.baddysays.saylat.data.StripPage
import com.baddysays.saylat.data.VisualPage
import com.baddysays.saylat.engine.RenderPlan
import com.baddysays.saylat.prefs.ReaderMode

/** Срез UI читалки — отдельный StateFlow уменьшает лишние рекомпозиции. */
data class ReaderUiState(
    val loading: Boolean = false,
    val layoutEnhancing: Boolean = false,
    val error: String? = null,
    val article: SaylatArticle? = null,
    val plan: RenderPlan? = null,
    val readerMode: ReaderMode = ReaderMode.LAYOUT,
    val visualPage: VisualPage? = null,
    val stripPage: StripPage? = null,
    val webViewUrl: String? = null,
    val pageLoadStats: ArticleStats? = null,
    val translationActive: Boolean = false,
    val translating: Boolean = false,
    val cachedNotice: String? = null,
)

fun BrowserUiState.toReaderUi(): ReaderUiState = ReaderUiState(
    loading = loading,
    layoutEnhancing = layoutEnhancing,
    error = error,
    article = article,
    plan = plan,
    readerMode = readerMode,
    visualPage = visualPage,
    stripPage = stripPage,
    webViewUrl = webViewUrl,
    pageLoadStats = pageLoadStats,
    translationActive = translationActive,
    translating = translating,
    cachedNotice = cachedNotice,
)

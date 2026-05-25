package com.baddysays.saylat.engine

import com.baddysays.saylat.data.ArticleStats
import com.baddysays.saylat.data.SaylatArticle
import com.baddysays.saylat.prefs.ReaderMode

/** Правки HERO (byline). Без навязчивых плашек — их закрывают баннеры в UI. */
object ArticleDisplayEnricher {

    fun enrich(
        plan: RenderPlan,
        article: SaylatArticle,
        @Suppress("UNUSED_PARAMETER") readerMode: ReaderMode,
    ): RenderPlan {
        val cards = plan.cards.toMutableList()
        val heroIdx = cards.indexOfFirst { it.kind == CardKind.HERO }
        if (heroIdx >= 0 && article.byline.isNotBlank()) {
            val hero = cards[heroIdx]
            cards[heroIdx] = hero.copy(meta = article.byline)
        }
        return plan.copy(cards = cards)
    }

    fun statsDetailForMode(stats: ArticleStats, readerMode: ReaderMode): String? {
        val base = com.baddysays.saylat.util.PageLoadStats.fromStats(stats).detail
        val modeNote = when (readerMode) {
            ReaderMode.WEBVIEW -> "Режим: полная страница (без сжатия)"
            ReaderMode.STRIPS -> "Режим: полосы JPEG (Opera Mini)"
            ReaderMode.LAYOUT, ReaderMode.AUTO ->
                if (stats.images_omitted > 0) "Режим: макет без JPEG" else null
            ReaderMode.NATIVE, ReaderMode.VISUAL ->
                if (stats.images_inlined > 0) "Режим: мини-картинки" else "Режим: карточки"
            else -> null
        }
        return listOfNotNull(base, modeNote).joinToString(" · ").ifBlank { null }
    }
}

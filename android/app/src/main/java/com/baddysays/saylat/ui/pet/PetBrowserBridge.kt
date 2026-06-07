package com.baddysays.saylat.ui.pet

import com.baddysays.saylat.data.ArticleStats
import com.baddysays.saylat.prefs.AppLanguage
import com.baddysays.saylat.tamagotchi.DialogCategory
import com.baddysays.saylat.tamagotchi.PetDialogues
import com.baddysays.saylat.tamagotchi.PetSiteReactions
import com.baddysays.saylat.tamagotchi.PetXpRewards

data class PetBrowserCue(
    val line: String,
    val xp: Int = 0,
)

sealed class PetBrowserAction {
    data class LoadStart(val url: String) : PetBrowserAction()
    data class LoadSuccess(
        val url: String,
        val savedBytes: Long,
        val durationMs: Long,
        val stats: ArticleStats,
        val host: String?,
        val ecoMode: Boolean,
    ) : PetBrowserAction()
    data class LoadError(val message: String?) : PetBrowserAction()
    data class SearchStart(val query: String) : PetBrowserAction()
    data class SearchDone(val count: Int) : PetBrowserAction()
    data object FeedOpen : PetBrowserAction()
    data object EcoModeEnabled : PetBrowserAction()
    data object Offline : PetBrowserAction()
    data object Online : PetBrowserAction()
    data object Refresh : PetBrowserAction()
    data object SaveReadLater : PetBrowserAction()
}

object PetBrowserBridge {

    fun cueFor(action: PetBrowserAction, lang: AppLanguage): PetBrowserCue = when (action) {
        is PetBrowserAction.LoadStart -> PetBrowserCue(
            line = PetDialogueMix.random(DialogCategory.LOADING, lang),
        )
        is PetBrowserAction.LoadSuccess -> loadSuccessCue(action, lang)
        is PetBrowserAction.LoadError -> PetBrowserCue(
            line = action.message?.take(60)
                ?: PetDialogueMix.random(DialogCategory.ERROR, lang),
        )
        is PetBrowserAction.SearchStart -> PetBrowserCue(
            line = if (lang == AppLanguage.RU) {
                "Ищу «${action.query.take(30)}»…"
            } else {
                "Searching «${action.query.take(30)}»…"
            },
        )
        is PetBrowserAction.SearchDone -> PetBrowserCue(
            line = if (lang == AppLanguage.RU) {
                "Нашёл ${action.count} результатов!"
            } else {
                "Found ${action.count} results!"
            },
            xp = PetXpRewards.SEARCH,
        )
        PetBrowserAction.FeedOpen -> PetBrowserCue(
            line = if (lang == AppLanguage.RU) "Загружаю ленту…" else "Loading feed…",
            xp = PetXpRewards.OPEN_FEED,
        )
        PetBrowserAction.EcoModeEnabled -> PetBrowserCue(
            line = if (lang == AppLanguage.RU) {
                "ECO активирован! Экономим трафик!"
            } else {
                "ECO on! Saving data!"
            },
        )
        PetBrowserAction.Offline -> PetBrowserCue(
            line = if (lang == AppLanguage.RU) {
                "Нет интернета… работаю из кэша!"
            } else {
                "Offline… reading from cache!"
            },
        )
        PetBrowserAction.Online -> PetBrowserCue(
            line = if (lang == AppLanguage.RU) "Интернет вернулся! Ура!" else "Back online! Yay!",
        )
        PetBrowserAction.Refresh -> PetBrowserCue(
            line = if (lang == AppLanguage.RU) {
                "Обновляю! Свежий контент!"
            } else {
                "Refreshing! Fresh content!"
            },
        )
        PetBrowserAction.SaveReadLater -> PetBrowserCue(
            line = if (lang == AppLanguage.RU) {
                "Сохранено! Прочитаешь потом."
            } else {
                "Saved for later!"
            },
            xp = PetXpRewards.SAVE_READ_LATER,
        )
    }

    private fun loadSuccessCue(action: PetBrowserAction.LoadSuccess, lang: AppLanguage): PetBrowserCue {
        val host = action.host.orEmpty()
        val siteLine = PetSiteReactions.forHost(host)?.let { ru ->
            if (lang == AppLanguage.RU) ru else siteReactionEn(host)
        }
        val saved = action.savedBytes
        val originalKb = (action.stats.original_bytes / 1024).coerceAtLeast(1)
        val compressedKb = (action.stats.payload_bytes / 1024).coerceAtLeast(1)
        val ratio = if (action.stats.original_bytes > 0) {
            ((1 - action.stats.payload_bytes.toFloat() / action.stats.original_bytes) * 100).toInt()
                .coerceIn(0, 99)
        } else {
            0
        }
        val line = when {
            siteLine != null -> siteLine
            saved > 10_000 && lang == AppLanguage.RU -> {
                "Готово! Сэкономлено ${PetSiteReactions.formatBytes(saved)}!"
            }
            saved > 10_000 -> {
                "Done! Saved ${PetSiteReactions.formatBytes(saved)}!"
            }
            ratio >= 40 && originalKb > 64 -> {
                if (lang == AppLanguage.RU) {
                    "Сжал на $ratio%! $originalKb КБ → $compressedKb КБ"
                } else {
                    "Compressed $ratio%! $originalKb KB → $compressedKb KB"
                }
            }
            else -> PetDialogueMix.random(DialogCategory.SUCCESS, lang)
        }
        val xp = PetXpRewards.forLoad(saved, action.durationMs, action.ecoMode)
        return PetBrowserCue(line = line, xp = xp)
    }

    private fun siteReactionEn(host: String): String {
        val h = host.lowercase()
        return when {
            "github" in h -> "GitHub! Code! Love it!"
            "wikipedia" in h -> "Wikipedia! Knowledge!"
            "habr" in h -> "Habr! Smart folks!"
            "youtube" in h -> "YouTube? I'll load the text!"
            "reddit" in h -> "Reddit! Many opinions!"
            "pikabu" in h -> "Pikabu? Fun!"
            "medium" in h -> "Medium! I know how to compress!"
            else -> PetDialogues.random(DialogCategory.REACTIONS)
        }
    }
}

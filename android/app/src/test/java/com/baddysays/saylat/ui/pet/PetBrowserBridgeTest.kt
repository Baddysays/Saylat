package com.baddysays.saylat.ui.pet

import com.baddysays.saylat.data.ArticleStats
import com.baddysays.saylat.prefs.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetBrowserBridgeTest {

    @Test
    fun loadSuccess_bigSave_mentionsSavedBytes() {
        val stats = ArticleStats(original_bytes = 200_000, payload_bytes = 80_000)
        val cue = PetBrowserBridge.cueFor(
            PetBrowserAction.LoadSuccess(
                url = "https://example.com/page",
                savedBytes = 120_000,
                durationMs = 900,
                stats = stats,
                host = "example.com",
                ecoMode = false,
            ),
            AppLanguage.RU,
        )
        assertTrue(cue.line.contains("КБ"))
        assertTrue(cue.xp > 0)
    }

    @Test
    fun searchDone_returnsCount() {
        val cue = PetBrowserBridge.cueFor(
            PetBrowserAction.SearchDone(3),
            AppLanguage.EN,
        )
        assertEquals("Found 3 results!", cue.line)
    }

    @Test
    fun offline_ru_mentionsCache() {
        val cue = PetBrowserBridge.cueFor(PetBrowserAction.Offline, AppLanguage.RU)
        assertTrue(cue.line.contains("кэш", ignoreCase = true))
    }
}

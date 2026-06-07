package com.baddysays.saylat.ui.pet

import com.baddysays.saylat.prefs.AppLanguage
import com.baddysays.saylat.tamagotchi.DialogCategory
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PetDialogueMixTest {

    @Test
    fun random_loading_ru_isNonBlank() {
        val line = PetDialogueMix.random(DialogCategory.LOADING, AppLanguage.RU, Random(1))
        assertTrue(line.isNotBlank())
    }

    @Test
    fun random_success_en_isNonBlank() {
        val line = PetDialogueMix.random(DialogCategory.SUCCESS, AppLanguage.EN, Random(2))
        assertTrue(line.isNotBlank())
    }
}

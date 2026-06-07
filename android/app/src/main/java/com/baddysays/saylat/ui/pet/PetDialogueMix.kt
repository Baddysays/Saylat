package com.baddysays.saylat.ui.pet

import com.baddysays.saylat.prefs.AppLanguage
import com.baddysays.saylat.tamagotchi.DialogCategory
import com.baddysays.saylat.tamagotchi.PetDialogues
import com.baddysays.saylat.tamagotchi.PetSiteReactions
import kotlin.random.Random

/** Смешивает импортированные RU-реплики с существующими EN-пулами. */
object PetDialogueMix {

    fun random(category: DialogCategory, lang: AppLanguage, random: Random = Random.Default): String {
        if (lang == AppLanguage.RU) return PetDialogues.random(category)
        return PetDialogue.forEvent(category.toSpeechEvent(), AppLanguage.EN, random)
    }

    fun randomIdle(lang: AppLanguage, random: Random = Random.Default): String {
        val roll = random.nextFloat()
        val category = when {
            PetSiteReactions.isNight() -> DialogCategory.NIGHT
            PetSiteReactions.isMorning() -> DialogCategory.MORNING
            roll < 0.15f -> DialogCategory.TIPS
            roll < 0.25f -> DialogCategory.PHILOSOPHICAL
            roll < 0.35f -> DialogCategory.FUN
            else -> DialogCategory.IDLE
        }
        return if (lang == AppLanguage.RU && random.nextFloat() < 0.55f) {
            PetDialogues.random(category)
        } else {
            PetDialogue.randomIdle(lang, random)
        }
    }

    private fun DialogCategory.toSpeechEvent(): SpeechEvent = when (this) {
        DialogCategory.LOADING -> SpeechEvent.WAIT
        DialogCategory.SUCCESS, DialogCategory.VICTORY -> SpeechEvent.READY
        DialogCategory.ERROR -> SpeechEvent.LOAD_FAILED
        DialogCategory.TIPS, DialogCategory.PHILOSOPHICAL, DialogCategory.REACTIONS -> SpeechEvent.THINK
        DialogCategory.NIGHT, DialogCategory.SLEEPING -> SpeechEvent.AUTONOMY_SLEEP
        DialogCategory.MORNING, DialogCategory.EXCITED -> SpeechEvent.EXCITED
        DialogCategory.FUN, DialogCategory.BORED -> SpeechEvent.AUTONOMY_PLAY
        DialogCategory.IDLE -> SpeechEvent.IDLE
    }
}

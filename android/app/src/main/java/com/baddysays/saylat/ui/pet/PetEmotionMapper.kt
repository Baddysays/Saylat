package com.baddysays.saylat.ui.pet

import com.baddysays.saylat.prefs.PetShopCatalog
import com.baddysays.saylat.tamagotchi.PetEmotion
import com.baddysays.saylat.tamagotchi.shop.AccessoryOverlayType
import com.baddysays.saylat.ui.PetMood
import com.baddysays.saylat.ui.PetPhase

object PetEmotionMapper {

    fun resolve(
        mood: PetMood,
        anim: PetAnim,
        eggMode: Boolean,
        phase: PetPhase? = null,
        loadFailed: Boolean = false,
    ): PetEmotion {
        if (eggMode) return PetEmotion.SLEEPING
        if (loadFailed || phase == PetPhase.LoadFailed) return PetEmotion.ERROR
        when (phase) {
            PetPhase.Waiting -> return PetEmotion.LOADING
            PetPhase.PageReady -> return PetEmotion.DONE
            else -> Unit
        }
        return when (anim) {
            PetAnim.SLEEP, PetAnim.SLEEP_DEEP -> PetEmotion.SLEEPING
            PetAnim.CRY -> PetEmotion.CRYING
            PetAnim.ANGRY -> PetEmotion.ANGRY
            PetAnim.CELEBRATE, PetAnim.JUMP -> PetEmotion.VICTORY
            PetAnim.DANCE, PetAnim.DANCE_SPIN, PetAnim.DANCE_SHUFFLE -> PetEmotion.DANCING
            PetAnim.EAT, PetAnim.CHEW -> PetEmotion.EATING
            PetAnim.READ -> PetEmotion.READING
            PetAnim.THINK -> PetEmotion.THINKING
            PetAnim.YAWN -> PetEmotion.YAWNING
            PetAnim.WAVE, PetAnim.BOUNCE -> PetEmotion.HAPPY
            PetAnim.EXCITED, PetAnim.RUN -> PetEmotion.HYPER
            PetAnim.PLAY, PetAnim.PLAY_BALL -> PetEmotion.HAPPY
            PetAnim.CHAIR_ROCK -> PetEmotion.MUSIC
            PetAnim.LOVE -> PetEmotion.IN_LOVE
            PetAnim.PEEK, PetAnim.LOOK_LEFT, PetAnim.LOOK_RIGHT -> PetEmotion.SNEAKY
            else -> when (mood) {
                PetMood.HUNGRY -> PetEmotion.WORRIED
                PetMood.SLEEPY -> PetEmotion.YAWNING
                PetMood.SICK -> PetEmotion.SICK
                PetMood.EXCITED -> PetEmotion.ECSTATIC
                else -> PetEmotion.IDLE
            }
        }
    }

    fun applyShopHat(hatId: String?, base: PetEmotion): PetEmotion {
        if (base == PetEmotion.LOADING || base == PetEmotion.ERROR || base == PetEmotion.DOWNLOADING) {
            return base
        }
        return when (hatId) {
            "hat_crown" -> PetEmotion.VICTORY
            "hat_party" -> PetEmotion.PARTY
            PetShopCatalog.HAT_LEAVES -> base
            else -> base
        }
    }

    fun shopAccessoryOverlay(hatId: String?): AccessoryOverlayType? = when (hatId) {
        "hat_cowboy", "hat_sombrero" -> AccessoryOverlayType.COWBOY_HAT
        "hat_visor" -> AccessoryOverlayType.PILOT_GOGGLES
        "hat_beanie" -> AccessoryOverlayType.SCARF
        else -> null
    }

    fun shopPreviewEmotion(itemId: String): PetEmotion = when (itemId) {
        "hat_crown" -> PetEmotion.VICTORY
        "hat_party" -> PetEmotion.PARTY
        PetShopCatalog.TOY_BALL, PetShopCatalog.TOY_CHAIR -> PetEmotion.HAPPY
        else -> PetEmotion.HAPPY
    }
}

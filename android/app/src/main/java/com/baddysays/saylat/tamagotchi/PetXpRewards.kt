package com.baddysays.saylat.tamagotchi

/** Награды XP из saylat_tamagotchi_v2 — используем существующий [com.baddysays.saylat.prefs.SaylatPrefs.addPetXp]. */
object PetXpRewards {
    const val LOAD_PAGE_SUCCESS = 5
    const val LOAD_PAGE_FAST = 3
    const val SEARCH = 3
    const val SAVE_READ_LATER = 8
    const val ECO_MODE_LOAD = 7
    const val OPEN_FEED = 4
    const val BIG_SAVE_BONUS = 10

    fun forLoad(savedBytes: Long, durationMs: Long, ecoMode: Boolean): Int {
        var xp = if (ecoMode) ECO_MODE_LOAD else LOAD_PAGE_SUCCESS
        if (durationMs in 1..999) xp += LOAD_PAGE_FAST
        if (savedBytes > 500_000) xp += BIG_SAVE_BONUS
        return xp
    }
}

package com.baddysays.saylat.prefs

/** Сохранённый прогресс питомца между сессиями. */
data class PetProfile(
    val name: String = PetSaladEconomy.DEFAULT_PET_NAME,
    val xp: Int = 0,
    val streak: Int = 0,
    val lastOpenDay: Long = 0L,
    val lastCareAt: Long = 0L,
    val salads: Int = 0,
    val bytesProgress: Long = 0L,
    val lifetimeBytesSaved: Long = 0L,
    /** Кошелёк Saylatik — КБ экономии для магазина. */
    val walletBytes: Long = 0L,
    val ownedItemIds: Set<String> = PetShopCatalog.defaultOwned,
    val equippedHatId: String? = PetShopCatalog.HAT_LEAVES,
    val ballEquipped: Boolean = false,
    val chairEquipped: Boolean = false,
    /** Байты «съеденных» салатиков — только они дают рост (до 100 МиБ). */
    val saladsEatenBytes: Long = 0L,
    /** Первый тап вылупляет яйцо → Level2. */
    val petHatched: Boolean = false,
) {
    val isEgg: Boolean get() = !petHatched && saladsEatenBytes <= 0L

    val growthLevel: Int get() = PetGrowth.effectiveLevel(saladsEatenBytes, petHatched)

    val stage: Int get() = PetGrowth.visualStage(growthLevel)

    val stageTitle: String get() = PetGrowth.titleForLevel(growthLevel)

    val bytesUntilNextSalad: Long get() = PetSaladEconomy.bytesUntilNextSalad(bytesProgress)

    fun owns(itemId: String): Boolean = itemId in ownedItemIds

    fun needsCare(nowMs: Long = System.currentTimeMillis()): Boolean {
        if (lastCareAt == 0L) return true
        return nowMs - lastCareAt > 12 * 60 * 60 * 1000L
    }
}

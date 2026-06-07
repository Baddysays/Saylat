package com.baddysays.saylat.ui.pet

import com.baddysays.saylat.prefs.PetProfile
import com.baddysays.saylat.prefs.PetShopCatalog

/** Надетые предметы из магазина. */
data class PetCosmetics(
    val hatId: String? = PetShopCatalog.HAT_LEAVES,
    val ballEquipped: Boolean = false,
    val chairEquipped: Boolean = false,
    val ownsBall: Boolean = false,
    val ownsChair: Boolean = false,
) {
    val hasPartyGear: Boolean get() = ballEquipped || chairEquipped

    companion object {
        fun from(profile: PetProfile): PetCosmetics = PetCosmetics(
            hatId = profile.equippedHatId ?: PetShopCatalog.HAT_LEAVES,
            ballEquipped = profile.ballEquipped && profile.owns(PetShopCatalog.TOY_BALL),
            chairEquipped = profile.chairEquipped && profile.owns(PetShopCatalog.TOY_CHAIR),
            ownsBall = profile.owns(PetShopCatalog.TOY_BALL),
            ownsChair = profile.owns(PetShopCatalog.TOY_CHAIR),
        )
    }
}

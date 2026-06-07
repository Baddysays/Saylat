package com.baddysays.saylat.prefs

/** Кошелёк Saylatik — тратится в магазине (1 байт экономии = 1 байт в кошельке). */
object PetWallet {
    fun credit(profile: PetProfile, savedBytes: Long): PetProfile {
        if (savedBytes <= 0) return profile
        return profile.copy(walletBytes = profile.walletBytes + savedBytes)
    }

    fun spend(profile: PetProfile, costBytes: Long): PetProfile? {
        if (costBytes <= 0 || profile.walletBytes < costBytes) return null
        return profile.copy(walletBytes = profile.walletBytes - costBytes)
    }

    fun formatWallet(profile: PetProfile): String = PetSaladEconomy.formatKb(profile.walletBytes)

    fun formatPrice(costBytes: Long): String = PetSaladEconomy.formatKb(costBytes)
}

package com.baddysays.saylat.prefs

/** Салатики за экономию трафика Saylat. */
object PetSaladEconomy {
    const val DEFAULT_PET_NAME = "Saylat"
    /** Один салатик за 50 КиБ сэкономленных байт. */
    const val BYTES_PER_SALAD = 50L * 1024L

    fun bytesUntilNextSalad(bytesProgress: Long): Long =
        (BYTES_PER_SALAD - (bytesProgress % BYTES_PER_SALAD)).let { if (it == BYTES_PER_SALAD) 0L else it }

    fun progressRatio(bytesProgress: Long): Float {
        val p = (bytesProgress % BYTES_PER_SALAD).toFloat()
        return (p / BYTES_PER_SALAD).coerceIn(0f, 1f)
    }

    fun applySavedBytes(profile: PetProfile, savedBytes: Long): PetProfile {
        if (savedBytes <= 0) return profile
        var progress = profile.bytesProgress + savedBytes
        var salads = profile.salads
        while (progress >= BYTES_PER_SALAD) {
            salads++
            progress -= BYTES_PER_SALAD
        }
        return PetWallet.credit(
            profile.copy(
                salads = salads,
                bytesProgress = progress,
                lifetimeBytesSaved = profile.lifetimeBytesSaved + savedBytes,
            ),
            savedBytes,
        )
    }

    fun spendSalad(profile: PetProfile): PetProfile? {
        if (profile.salads <= 0) return null
        val eaten = (profile.saladsEatenBytes + BYTES_PER_SALAD)
            .coerceAtMost(PetGrowth.MAX_GROWTH_BYTES)
        return profile.copy(
            salads = profile.salads - 1,
            saladsEatenBytes = eaten,
        )
    }

    fun formatKb(bytes: Long): String {
        val kb = bytes / 1024.0
        return if (kb >= 100) "${kb.toInt()} КБ" else "%.1f КБ".format(kb)
    }
}

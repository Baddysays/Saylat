package com.baddysays.saylat.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PetSaladEconomyTest {

    @Test
    fun applySavedBytes_grantsSaladAtThreshold() {
        val profile = PetProfile(bytesProgress = 0, salads = 0)
        val updated = PetSaladEconomy.applySavedBytes(profile, PetSaladEconomy.BYTES_PER_SALAD)
        assertEquals(1, updated.salads)
        assertEquals(0L, updated.bytesProgress)
    }

    @Test
    fun spendSalad_requiresBalance() {
        val broke = PetSaladEconomy.spendSalad(PetProfile(salads = 0))
        assertNull(broke)
        val fed = PetSaladEconomy.spendSalad(PetProfile(salads = 2))!!
        assertEquals(1, fed.salads)
    }

    @Test
    fun defaultPetName_isSaylat() {
        assertEquals("Saylat", PetSaladEconomy.DEFAULT_PET_NAME)
    }
}

package com.baddysays.saylat.data

/** Уровень сжатия: light / medium / full — согласован с сервером. */
object CompressionLevel {
    const val LIGHT = "light"
    const val MEDIUM = "medium"
    const val FULL = "full"

    fun resolve(slowNetwork: Boolean, smartLayoutEnabled: Boolean, smartLayoutAvailable: Boolean): String =
        when {
            slowNetwork -> LIGHT
            smartLayoutEnabled && smartLayoutAvailable -> FULL
            else -> MEDIUM
        }
}

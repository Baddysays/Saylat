package com.baddysays.saylat.ui.theme

enum class AppThemeId(val id: String, val label: String) {
    SYSTEM("system", "Системная"),
    TEAL("teal", "Saylat"),
    MIDNIGHT("midnight", "Полночь"),
    AURORA("aurora", "Аврора"),
    SUNSET("sunset", "Закат"),
    OCEAN("ocean", "Океан"),
    FOREST("forest", "Лес"),
    AMOLED("amoled", "AMOLED"),
    PAPER("paper", "Бумага"),
    SNOW("snow", "Снег"),
    LEMON("lemon", "Лимон"),
    LILAC("lilac", "Сирень"),
    CORAL("coral", "Коралл"),
    ;

    /** Всегда светлая палитра, даже при тёмной системе. */
    fun isAlwaysLight(): Boolean = this in AlwaysLight

    /** Всегда тёмная палитра. */
    fun isAlwaysDark(): Boolean = this == AMOLED

    companion object {
        val AlwaysLight: Set<AppThemeId> = setOf(PAPER, SNOW, LEMON, LILAC, CORAL)

        val Adaptive: List<AppThemeId> = listOf(
            SYSTEM, TEAL, MIDNIGHT, AURORA, SUNSET, OCEAN, FOREST, AMOLED,
        )

        val LightPicker: List<AppThemeId> = AlwaysLight.toList()

        fun fromId(raw: String?): AppThemeId =
            entries.firstOrNull { it.id == raw } ?: TEAL
    }
}

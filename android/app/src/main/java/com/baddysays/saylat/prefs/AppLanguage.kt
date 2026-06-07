package com.baddysays.saylat.prefs

/** Язык интерфейса и реплик питомца. */
enum class AppLanguage(val id: String, val nativeName: String) {
    RU("ru", "Русский"),
    EN("en", "English"),
    ;

    companion object {
        fun fromId(id: String?): AppLanguage =
            entries.find { it.id.equals(id?.trim(), ignoreCase = true) } ?: RU
    }
}

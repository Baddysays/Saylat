package com.baddysays.saylat.prefs

/** Как показывать произвольный URL (не ленты сервисов). */
enum class ReaderMode(val id: String, val label: String, val hint: String) {
    LAYOUT(
        id = "layout",
        label = "Экономия",
        hint = "Текст и рамки вместо фото. Минимум трафика.",
    ),
    STRIPS(
        id = "strips",
        label = "Полосы (Opera)",
        hint = "Скриншот на VPS + JPEG-полосы. Сохранение в галерею.",
    ),
    WEBVIEW(
        id = "webview",
        label = "Как на сайте",
        hint = "Полная страница на телефоне, без сжатия прокси.",
    ),
    AUTO(
        id = "auto",
        label = "Авто",
        hint = "Медленная сеть — экономия, быстрая — полосы; пустая страница — WebView.",
    ),
    /** Legacy prefs: `native` → экономия при чтении id. */
    NATIVE(id = "native", label = "", hint = ""),
    /** Legacy prefs: `visual` → полосы при чтении id. */
    VISUAL(id = "visual", label = "", hint = ""),
    ;

    companion object {
        val settingsChoices: List<ReaderMode> = listOf(LAYOUT, STRIPS, WEBVIEW, AUTO)

        fun fromId(raw: String?): ReaderMode {
            val id = raw?.trim()?.lowercase() ?: return LAYOUT
            return when (id) {
                "native" -> LAYOUT
                "visual" -> STRIPS
                else -> entries.firstOrNull { it.id == id && it.label.isNotEmpty() } ?: LAYOUT
            }
        }
    }
}

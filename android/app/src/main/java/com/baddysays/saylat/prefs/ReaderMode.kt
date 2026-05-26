package com.baddysays.saylat.prefs

/** Как показывать произвольный URL (не ленты сервисов). */
enum class ReaderMode(val id: String, val label: String, val hint: String) {
    LAYOUT(
        id = "layout",
        label = "Экономия текста",
        hint = "Сайт превращается в карточки и текст. Самый лёгкий режим чтения.",
    ),
    STRIPS(
        id = "strips",
        label = "Полосы",
        hint = "VPS делает длинные JPEG-полосы страницы. Похоже на Opera Mini, можно сохранить в галерею.",
    ),
    WEBVIEW(
        id = "webview",
        label = "Как на сайте",
        hint = "Обычный сайт внутри телефона. Нужен, когда важна точная вёрстка, а не экономия.",
    ),
    AUTO(
        id = "auto",
        label = "Авто",
        hint = "Выбирает вид страницы сам: на медленной сети уходит в текст, на быстрой — в полосы; если страница плохо читается, открывает сайт как есть.",
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

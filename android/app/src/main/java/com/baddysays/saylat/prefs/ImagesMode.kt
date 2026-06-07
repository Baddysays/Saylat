package com.baddysays.saylat.prefs

/** Режим картинок при запросе /api/extract */
object ImagesMode {
    const val NORMAL = "normal"
    const val TINY = "tiny"
    const val OFF = "off"
    const val LAYOUT = "layout"
    /** URL картинок в JSON без base64 — грузятся на устройстве. */
    const val REFS = "refs"

    fun resolve(slowNetwork: Boolean, liteImagesEnabled: Boolean): String = when {
        slowNetwork || liteImagesEnabled -> REFS
        else -> NORMAL
    }

    /** Подсказка, почему в читалке нет картинок. */
    fun hint(slowNetwork: Boolean, liteImagesEnabled: Boolean, readerMode: ReaderMode): String = when {
        readerMode == ReaderMode.LAYOUT -> "Макет сохранён, JPEG не качаются — только подписи к картинкам"
        liteImagesEnabled -> "Картинки по ссылкам — JPEG качает приложение"
        slowNetwork -> "Эко: картинки по ссылкам, меньше трафика на статью"
        else -> "Картинки в полном качестве (до 6 шт.)"
    }
}

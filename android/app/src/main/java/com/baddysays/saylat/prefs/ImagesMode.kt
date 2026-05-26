package com.baddysays.saylat.prefs

/** Режим картинок при запросе /api/extract */
object ImagesMode {
    const val NORMAL = "normal"
    const val TINY = "tiny"
    const val OFF = "off"
    const val LAYOUT = "layout"

    fun resolve(slowNetwork: Boolean, liteImagesEnabled: Boolean): String = when {
        liteImagesEnabled -> TINY
        slowNetwork -> OFF
        else -> NORMAL
    }

    /** Подсказка, почему в читалке нет картинок. */
    fun hint(slowNetwork: Boolean, liteImagesEnabled: Boolean, readerMode: ReaderMode): String = when {
        readerMode == ReaderMode.LAYOUT -> "Макет сохранён, JPEG не качаются — только подписи к картинкам"
        liteImagesEnabled -> "Картинки в постах: сжатые JPEG с прокси"
        slowNetwork -> "Картинки выключены — включите облегчённые картинки в «Подключение»"
        else -> "Картинки в полном качестве (до 6 шт.)"
    }
}

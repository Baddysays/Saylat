package com.baddysays.saylat.prefs

/** Рост Saylat только от съеденных салатиков (до 100 МиБ). */
object PetGrowth {
    const val LEVEL_COUNT = 10
    const val MAX_GROWTH_BYTES = 100L * 1024L * 1024L
    private const val BYTES_PER_LEVEL = MAX_GROWTH_BYTES / LEVEL_COUNT

    /** Уровень 1…10 по объёму съеденных салатиков. */
    fun levelFromEatenBytes(bytes: Long): Int {
        if (bytes <= 0L) return 1
        val capped = bytes.coerceAtMost(MAX_GROWTH_BYTES)
        val band = (capped / BYTES_PER_LEVEL).toInt().coerceIn(0, LEVEL_COUNT - 1)
        return (band + 1).coerceIn(1, LEVEL_COUNT)
    }

    /** После первого тапа яйцо сразу Level2, дальше рост только от салатиков. */
    fun effectiveLevel(bytes: Long, hatched: Boolean): Int {
        val fromBytes = levelFromEatenBytes(bytes)
        if (fromBytes > 1) return fromBytes
        return if (hatched) 2 else 1
    }

    fun titleForLevel(level: Int): String =
        "Level${level.coerceIn(1, LEVEL_COUNT)}"

    /** 0 = яйцо, 4 = максимальный размер спрайта. */
    fun visualStage(level: Int): Int = when (level.coerceIn(1, LEVEL_COUNT)) {
        1 -> 0
        2, 3 -> 1
        4, 5 -> 2
        6, 7 -> 3
        else -> 4
    }

    fun bytesForLevel(level: Int): Long =
        ((level - 1).coerceIn(0, LEVEL_COUNT - 1)) * BYTES_PER_LEVEL

    fun bytesUntilNextLevel(bytes: Long): Long {
        val level = levelFromEatenBytes(bytes)
        if (level >= LEVEL_COUNT) return 0L
        return (bytesForLevel(level + 1) - bytes).coerceAtLeast(0L)
    }

    fun progressToNextLevel(bytes: Long): Float {
        val level = levelFromEatenBytes(bytes)
        if (level >= LEVEL_COUNT) return 1f
        val start = bytesForLevel(level)
        val end = bytesForLevel(level + 1)
        val span = (end - start).coerceAtLeast(1L)
        return ((bytes - start).toFloat() / span).coerceIn(0f, 1f)
    }

    fun progressToMax(bytes: Long): Float =
        (bytes.toFloat() / MAX_GROWTH_BYTES).coerceIn(0f, 1f)

    fun formatEatenMb(bytes: Long): String {
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return if (mb >= 10) "${mb.toInt()} МБ" else "%.1f МБ".format(mb)
    }
}

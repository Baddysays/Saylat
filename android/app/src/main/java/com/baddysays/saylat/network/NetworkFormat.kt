package com.baddysays.saylat.network

import kotlin.math.roundToInt

object NetworkFormat {
    fun latency(ms: Int): String = when {
        ms >= 60_000 -> {
            val sec = (ms % 60_000) / 1000
            val min = ms / 60_000
            if (sec > 0) "$min мин $sec с" else "$min мин"
        }
        ms >= 1000 -> String.format("%.1f с", ms / 1000.0)
        else -> "$ms мс"
    }

    fun speedKbps(kbps: Double): String = when {
        kbps >= 1000 -> "${(kbps / 1000.0).roundToInt()} Мбит/с"
        kbps < 1 -> "< 1 Кбит/с"
        else -> "${kbps.roundToInt()} Кбит/с"
    }
}

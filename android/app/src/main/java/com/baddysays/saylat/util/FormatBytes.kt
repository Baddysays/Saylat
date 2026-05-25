package com.baddysays.saylat.util

fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> String.format("%.1f ГБ", bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> String.format("%.1f МБ", bytes / 1_048_576.0)
    bytes >= 1024 -> String.format("%.1f КБ", bytes / 1024.0)
    else -> "$bytes Б"
}

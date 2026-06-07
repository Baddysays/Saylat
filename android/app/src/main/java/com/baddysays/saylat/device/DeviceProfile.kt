package com.baddysays.saylat.device

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.baddysays.saylat.prefs.AppLanguage
import com.baddysays.saylat.ui.strings.SaylatStrings

enum class DeviceTier {
    LOW,
    MID,
    HIGH,
}

data class DeviceProfile(
    val tier: DeviceTier,
    val ramMb: Long,
    val apiLevel: Int,
    val label: String,
    val hint: String,
)

object DeviceCapabilities {

    private const val MIN_RAM_MB_FOR_SMART_LAYOUT = 3500
    private const val LOW_RAM_MB = 2048
    private const val MID_RAM_MB = 3500

    fun totalRamMb(context: Context): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.totalMem / (1024 * 1024)
    }

    fun profile(context: Context): DeviceProfile {
        val ram = totalRamMb(context)
        val api = Build.VERSION.SDK_INT
        val tier = when {
            ram < LOW_RAM_MB || api < 26 -> DeviceTier.LOW
            ram < MID_RAM_MB -> DeviceTier.MID
            else -> DeviceTier.HIGH
        }
        val label = when (tier) {
            DeviceTier.LOW -> "Слабое устройство"
            DeviceTier.MID -> "Средний класс"
            DeviceTier.HIGH -> "Мощное устройство"
        }
        val hint = when (tier) {
            DeviceTier.LOW -> "~${ram} МБ RAM · включите режим 2G в настройках"
            DeviceTier.MID -> "~${ram} МБ RAM · умная вёрстка по желанию"
            DeviceTier.HIGH -> "~${ram} МБ RAM · все функции доступны"
        }
        return DeviceProfile(tier = tier, ramMb = ram, apiLevel = api, label = label, hint = hint)
    }

    /** Saylat ориентирован на медленные сети — по умолчанию длинные таймауты. */
    fun shouldDefaultSlowNetwork(@Suppress("UNUSED_PARAMETER") context: Context): Boolean = true

    fun canRunSmartLayout(context: Context): Boolean =
        totalRamMb(context) >= MIN_RAM_MB_FOR_SMART_LAYOUT

    fun smartLayoutUnavailableReason(context: Context): String? =
        smartLayoutHint(context, AppLanguage.RU)

    fun smartLayoutHint(context: Context, lang: AppLanguage): String? {
        if (canRunSmartLayout(context)) return null
        return SaylatStrings.smartLayoutRamHint(lang, totalRamMb(context), MIN_RAM_MB_FOR_SMART_LAYOUT)
    }
}

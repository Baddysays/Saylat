package com.baddysays.saylat.util

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import com.baddysays.saylat.MainActivity
import com.baddysays.saylat.R

object HomeShortcutHelper {
    fun requestPin(context: Context, url: String, title: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val manager = context.getSystemService(ShortcutManager::class.java) ?: return false
        if (!manager.isRequestPinShortcutSupported) return false

        val label = title.trim().ifBlank { url }.take(24)
        val shortcut = ShortcutInfo.Builder(context, "saylat_${url.hashCode()}")
            .setShortLabel(label)
            .setLongLabel(title.trim().ifBlank { url }.take(48))
            .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(
                Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra(MainActivity.EXTRA_START_URL, url)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
            .build()

        return manager.requestPinShortcut(shortcut, null)
    }
}

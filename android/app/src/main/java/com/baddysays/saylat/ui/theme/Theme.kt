package com.baddysays.saylat.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

internal data class ThemePalette(
    val light: androidx.compose.material3.ColorScheme,
    val dark: androidx.compose.material3.ColorScheme,
    val preview: Color,
)

private val palettes = lightOnlyPalettes() + mapOf(
    AppThemeId.TEAL to ThemePalette(
        preview = Color(0xFF0F766E),
        light = lightColorScheme(
            primary = Color(0xFF0F766E),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFCCFBF1),
            onPrimaryContainer = Color(0xFF134E4A),
            secondary = Color(0xFF14B8A6),
            tertiary = Color(0xFF6366F1),
            background = Color(0xFFF1F5F9),
            surface = Color.White,
            surfaceVariant = Color(0xFFE2E8F0),
            onBackground = Color(0xFF0F172A),
            onSurface = Color(0xFF0F172A),
        ),
        dark = darkColorScheme(
            primary = Color(0xFF2DD4BF),
            onPrimary = Color(0xFF042F2E),
            primaryContainer = Color(0xFF134E4A),
            onPrimaryContainer = Color(0xFF99F6E4),
            secondary = Color(0xFF14B8A6),
            tertiary = Color(0xFFA5B4FC),
            background = Color(0xFF0B1220),
            surface = Color(0xFF111827),
            surfaceVariant = Color(0xFF1E293B),
            onBackground = Color(0xFFE2E8F0),
            onSurface = Color(0xFFE2E8F0),
        ),
    ),
    AppThemeId.MIDNIGHT to ThemePalette(
        preview = Color(0xFF312E81),
        light = lightColorScheme(
            primary = Color(0xFF4338CA),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE0E7FF),
            onPrimaryContainer = Color(0xFF312E81),
            secondary = Color(0xFF6366F1),
            tertiary = Color(0xFF8B5CF6),
            background = Color(0xFFF8FAFC),
            surface = Color.White,
            surfaceVariant = Color(0xFFE2E8F0),
            onBackground = Color(0xFF1E1B4B),
            onSurface = Color(0xFF1E1B4B),
        ),
        dark = darkColorScheme(
            primary = Color(0xFF818CF8),
            onPrimary = Color(0xFF1E1B4B),
            primaryContainer = Color(0xFF3730A3),
            onPrimaryContainer = Color(0xFFC7D2FE),
            secondary = Color(0xFF6366F1),
            tertiary = Color(0xFFA78BFA),
            background = Color(0xFF030712),
            surface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFF1E293B),
            onBackground = Color(0xFFE2E8F0),
            onSurface = Color(0xFFE2E8F0),
        ),
    ),
    AppThemeId.AURORA to ThemePalette(
        preview = Color(0xFF7C3AED),
        light = lightColorScheme(
            primary = Color(0xFF7C3AED),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFEDE9FE),
            onPrimaryContainer = Color(0xFF4C1D95),
            secondary = Color(0xFF06B6D4),
            tertiary = Color(0xFFEC4899),
            background = Color(0xFFFAF5FF),
            surface = Color.White,
            surfaceVariant = Color(0xFFF3E8FF),
            onBackground = Color(0xFF1F2937),
            onSurface = Color(0xFF1F2937),
        ),
        dark = darkColorScheme(
            primary = Color(0xFFC4B5FD),
            onPrimary = Color(0xFF2E1065),
            primaryContainer = Color(0xFF5B21B6),
            onPrimaryContainer = Color(0xFFEDE9FE),
            secondary = Color(0xFF22D3EE),
            tertiary = Color(0xFFF472B6),
            background = Color(0xFF0C0A14),
            surface = Color(0xFF15121F),
            surfaceVariant = Color(0xFF2A2438),
            onBackground = Color(0xFFF5F3FF),
            onSurface = Color(0xFFF5F3FF),
        ),
    ),
    AppThemeId.SUNSET to ThemePalette(
        preview = Color(0xFFEA580C),
        light = lightColorScheme(
            primary = Color(0xFFEA580C),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFEDD5),
            onPrimaryContainer = Color(0xFF9A3412),
            secondary = Color(0xFFF97316),
            tertiary = Color(0xFFE11D48),
            background = Color(0xFFFFFBEB),
            surface = Color.White,
            surfaceVariant = Color(0xFFFED7AA),
            onBackground = Color(0xFF431407),
            onSurface = Color(0xFF431407),
        ),
        dark = darkColorScheme(
            primary = Color(0xFFFDBA74),
            onPrimary = Color(0xFF431407),
            primaryContainer = Color(0xFF9A3412),
            onPrimaryContainer = Color(0xFFFFEDD5),
            secondary = Color(0xFFFB923C),
            tertiary = Color(0xFFFB7185),
            background = Color(0xFF1A0F0A),
            surface = Color(0xFF27150D),
            surfaceVariant = Color(0xFF3F2315),
            onBackground = Color(0xFFFFF7ED),
            onSurface = Color(0xFFFFF7ED),
        ),
    ),
    AppThemeId.OCEAN to ThemePalette(
        preview = Color(0xFF0284C7),
        light = lightColorScheme(
            primary = Color(0xFF0284C7),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE0F2FE),
            onPrimaryContainer = Color(0xFF075985),
            secondary = Color(0xFF0EA5E9),
            tertiary = Color(0xFF14B8A6),
            background = Color(0xFFF0F9FF),
            surface = Color.White,
            surfaceVariant = Color(0xFFBAE6FD),
            onBackground = Color(0xFF0C4A6E),
            onSurface = Color(0xFF0C4A6E),
        ),
        dark = darkColorScheme(
            primary = Color(0xFF7DD3FC),
            onPrimary = Color(0xFF082F49),
            primaryContainer = Color(0xFF0369A1),
            onPrimaryContainer = Color(0xFFE0F2FE),
            secondary = Color(0xFF38BDF8),
            tertiary = Color(0xFF2DD4BF),
            background = Color(0xFF051018),
            surface = Color(0xFF0B1929),
            surfaceVariant = Color(0xFF164E63),
            onBackground = Color(0xFFE0F2FE),
            onSurface = Color(0xFFE0F2FE),
        ),
    ),
    AppThemeId.FOREST to ThemePalette(
        preview = Color(0xFF15803D),
        light = lightColorScheme(
            primary = Color(0xFF15803D),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFDCFCE7),
            onPrimaryContainer = Color(0xFF14532D),
            secondary = Color(0xFF22C55E),
            tertiary = Color(0xFF84CC16),
            background = Color(0xFFF7FEE7),
            surface = Color.White,
            surfaceVariant = Color(0xFFBBF7D0),
            onBackground = Color(0xFF14532D),
            onSurface = Color(0xFF14532D),
        ),
        dark = darkColorScheme(
            primary = Color(0xFF86EFAC),
            onPrimary = Color(0xFF052E16),
            primaryContainer = Color(0xFF166534),
            onPrimaryContainer = Color(0xFFDCFCE7),
            secondary = Color(0xFF4ADE80),
            tertiary = Color(0xFFA3E635),
            background = Color(0xFF071209),
            surface = Color(0xFF0F1F14),
            surfaceVariant = Color(0xFF14532D),
            onBackground = Color(0xFFECFCCB),
            onSurface = Color(0xFFECFCCB),
        ),
    ),
    AppThemeId.AMOLED to ThemePalette(
        preview = Color(0xFF000000),
        light = lightColorScheme(
            primary = Color(0xFF18181B),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFF4F4F5),
            onPrimaryContainer = Color(0xFF18181B),
            secondary = Color(0xFF52525B),
            tertiary = Color(0xFF3B82F6),
            background = Color(0xFFFAFAFA),
            surface = Color.White,
            surfaceVariant = Color(0xFFE4E4E7),
            onBackground = Color(0xFF09090B),
            onSurface = Color(0xFF09090B),
        ),
        dark = darkColorScheme(
            primary = Color(0xFFFAFAFA),
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF27272A),
            onPrimaryContainer = Color(0xFFFAFAFA),
            secondary = Color(0xFFA1A1AA),
            tertiary = Color(0xFF60A5FA),
            background = Color.Black,
            surface = Color(0xFF09090B),
            surfaceVariant = Color(0xFF18181B),
            onBackground = Color(0xFFFAFAFA),
            onSurface = Color(0xFFFAFAFA),
        ),
    ),
)

fun themePreviewColor(id: AppThemeId): Color =
    palettes[id]?.preview ?: palettes[AppThemeId.TEAL]!!.preview

@Composable
fun SaylatTheme(
    themeId: AppThemeId = AppThemeId.TEAL,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val dark = when {
        themeId.isAlwaysLight() -> false
        themeId.isAlwaysDark() -> true
        themeId == AppThemeId.SYSTEM -> isSystemInDarkTheme()
        else -> isSystemInDarkTheme()
    }

    val colors = remember(themeId, dark) {
        when (themeId) {
            AppThemeId.SYSTEM -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                } else {
                    palettes[AppThemeId.TEAL]!!.let { if (dark) it.dark else it.light }
                }
            }
            else -> palettes[themeId]?.let { if (dark) it.dark else it.light }
                ?: palettes[AppThemeId.TEAL]!!.let { if (dark) it.dark else it.light }
        }
    }

    SyncSystemBarsForTheme(useDarkTheme = dark)
    MaterialTheme(colorScheme = colors, content = content)
}

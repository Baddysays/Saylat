package com.baddysays.saylat.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.baddysays.saylat.ui.theme.AppThemeId
import com.baddysays.saylat.ui.theme.themePreviewColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemePickerRow(
    selected: AppThemeId,
    onSelect: (AppThemeId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ThemeSection(
            title = "Светлые",
            hint = "Всегда светлый интерфейс",
            icon = { Icon(Icons.Default.LightMode, null, Modifier.size(16.dp)) },
            themes = AppThemeId.LightPicker,
            selected = selected,
            onSelect = onSelect,
        )
        ThemeSection(
            title = "Адаптивные",
            hint = "Светлая или тёмная — по системе (кроме AMOLED)",
            icon = { Icon(Icons.Default.DarkMode, null, Modifier.size(16.dp)) },
            themes = AppThemeId.Adaptive,
            selected = selected,
            onSelect = onSelect,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeSection(
    title: String,
    hint: String,
    icon: @Composable () -> Unit,
    themes: List<AppThemeId>,
    selected: AppThemeId,
    onSelect: (AppThemeId) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RowWithIcon(icon, title, hint)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            themes.forEach { theme ->
                ThemeSwatch(theme = theme, isSelected = theme == selected, onSelect = { onSelect(theme) })
            }
        }
    }
}

@Composable
private fun RowWithIcon(
    icon: @Composable () -> Unit,
    title: String,
    hint: String,
) {
    Column(Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        }
        Text(
            hint,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun ThemeSwatch(
    theme: AppThemeId,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onSelect),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(themePreviewColor(theme))
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    },
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = if (theme.isAlwaysLight()) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Text(
            theme.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

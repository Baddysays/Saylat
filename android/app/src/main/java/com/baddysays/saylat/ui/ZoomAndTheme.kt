package com.baddysays.saylat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val MIN_SCALE = 1.0f
private const val MAX_SCALE = 4.0f

@Composable
fun ZoomableBox(
    modifier: Modifier = Modifier,
    minScale: Float = MIN_SCALE,
    maxScale: Float = MAX_SCALE,
    content: @Composable () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(minScale, maxScale)
                    val maxX = (size.width * (newScale - 1f)) / 2f
                    val maxY = (size.height * (newScale - 1f)) / 2f
                    offsetX = if (newScale <= 1f) 0f
                    else (offsetX + pan.x).coerceIn(-maxX, maxX)
                    offsetY = if (newScale <= 1f) 0f
                    else (offsetY + pan.y).coerceIn(-maxY, maxY)
                    scale = newScale
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offsetX
                translationY = offsetY
            },
    ) {
        content()
    }
}

enum class ReaderTheme(
    val label: String,
    val background: Color,
    val textColor: Color,
    val surfaceColor: Color,
) {
    AUTO("Авто", Color.Unspecified, Color.Unspecified, Color.Unspecified),
    SEPIA("Сепия", Color(0xFFF8F0E3), Color(0xFF3B2A1A), Color(0xFFF0E6D3)),
    NIGHT("Ночь", Color(0xFF121212), Color(0xFFE0E0E0), Color(0xFF1E1E1E)),
}

fun Modifier.readerBackground(theme: ReaderTheme): Modifier =
    if (theme == ReaderTheme.AUTO) this else this.background(theme.background)

@Composable
fun readerTextColor(theme: ReaderTheme): Color =
    if (theme == ReaderTheme.AUTO || theme.textColor == Color.Unspecified) {
        MaterialTheme.colorScheme.onBackground
    } else {
        theme.textColor
    }

@Composable
fun ReaderThemeSelector(
    currentTheme: ReaderTheme,
    onSelect: (ReaderTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "Тема читалки",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReaderTheme.entries.forEach { theme ->
                ThemeOption(
                    theme = theme,
                    selected = currentTheme == theme,
                    onSelect = { onSelect(theme) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ThemeOption(
    theme: ReaderTheme,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val previewBg = when (theme) {
        ReaderTheme.AUTO -> MaterialTheme.colorScheme.surface
        else -> theme.background
    }
    val previewText = when (theme) {
        ReaderTheme.AUTO -> MaterialTheme.colorScheme.onSurface
        else -> theme.textColor
    }
    Column(
        modifier = modifier
            .selectable(selected = selected, onClick = onSelect)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color = previewBg, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("Аа", color = previewText, fontSize = 14.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            theme.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RadioButton(selected = selected, onClick = onSelect, modifier = Modifier.size(24.dp))
    }
}

data class ReaderFontSettings(
    val sizeSp: Float = 15f,
    val lineHeightMultiplier: Float = 1.55f,
)

@Composable
fun FontSizeControl(
    settings: ReaderFontSettings,
    onUpdate: (ReaderFontSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = {
            onUpdate(settings.copy(sizeSp = (settings.sizeSp - 1f).coerceAtLeast(12f)))
        }) {
            Icon(Icons.Default.TextDecrease, contentDescription = "Уменьшить шрифт")
        }
        Slider(
            value = settings.sizeSp,
            onValueChange = { onUpdate(settings.copy(sizeSp = it)) },
            valueRange = 12f..22f,
            steps = 9,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = {
            onUpdate(settings.copy(sizeSp = (settings.sizeSp + 1f).coerceAtMost(22f)))
        }) {
            Icon(Icons.Default.TextIncrease, contentDescription = "Увеличить шрифт")
        }
        Text(
            text = "${settings.sizeSp.toInt()} sp",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(40.dp),
        )
    }
}

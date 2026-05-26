package com.baddysays.saylat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SaylatTopBar(
    screen: AppScreen,
    activeQuery: String?,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onSettings: () -> Unit,
    showTranslate: Boolean = false,
    translationActive: Boolean = false,
    translating: Boolean = false,
    onToggleTranslate: () -> Unit = {},
    showGallerySave: Boolean = false,
    gallerySaveInProgress: Boolean = false,
    onSaveGallery: () -> Unit = {},
    showFavoriteActions: Boolean = false,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onPinShortcut: () -> Unit = {},
) {
    val clockLabel = rememberClockLabel()
    val (title, subtitle) = when (screen) {
        AppScreen.HOME -> null to null
        AppScreen.SEARCH_RESULTS -> (activeQuery?.let { " «$it»" } ?: "Поиск") to "результаты"
        AppScreen.READER -> "Чтение" to "сжатая лента"
        AppScreen.FEED -> "Лента" to "Пикабу · ВК · Дзен"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (screen != AppScreen.HOME) {
                IconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            }

            when (screen) {
                AppScreen.HOME -> SaylatBrandMark(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    expanded = true,
                    iconSize = 40.dp,
                )
                else -> {
                    val t = title ?: ""
                    val s = subtitle ?: ""
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                    ) {
                        Text(
                            t,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            s,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                ) {
                    Text(
                        clockLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (showFavoriteActions) {
                    IconButton(
                        onClick = onToggleFavorite,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(),
                    ) {
                        Icon(
                            if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "В избранное",
                        )
                    }
                    IconButton(
                        onClick = onPinShortcut,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(),
                    ) {
                        Icon(Icons.Default.PushPin, contentDescription = "На рабочий стол")
                    }
                }
                if (showGallerySave) {
                    IconButton(
                        onClick = onSaveGallery,
                        enabled = !gallerySaveInProgress,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(),
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = "Сохранить в галерею")
                    }
                }
                if (showTranslate) {
                    IconButton(
                        onClick = onToggleTranslate,
                        enabled = !translating,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (translationActive) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                        ),
                    ) {
                        Icon(Icons.Default.Translate, contentDescription = "Перевести")
                    }
                }
                if (screen != AppScreen.HOME) {
                    IconButton(
                        onClick = onHome,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(),
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Домой")
                    }
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    IconButton(onClick = onSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Настройки",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberClockLabel(): String {
    var label by remember { mutableStateOf(formatNow()) }
    LaunchedEffect(Unit) {
        while (true) {
            label = formatNow()
            delay(30_000)
        }
    }
    return label
}

private fun formatNow(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

package com.baddysays.saylat.ui

import com.baddysays.saylat.data.HistoryEntry
import com.baddysays.saylat.prefs.AppLanguage
import com.baddysays.saylat.ui.strings.SaylatStrings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.imePadding

import androidx.compose.foundation.layout.navigationBarsPadding

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Search

import androidx.compose.material.icons.filled.Tune

import androidx.compose.material3.DropdownMenu

import androidx.compose.material3.DropdownMenuItem

import androidx.compose.material3.FilledIconButton

import androidx.compose.material3.Icon

import androidx.compose.material3.IconButton

import androidx.compose.material3.IconButtonDefaults

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.OutlinedTextField

import androidx.compose.material3.OutlinedTextFieldDefaults

import androidx.compose.material3.Surface

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.focus.focusRequester

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.dp



enum class QuickSpeedMode(

    val title: String,

    val subtitle: String,

    val summary: String,

) {

    ECO(

        "Эко",

        "2G / минимум трафика",

        "Длинные таймауты, максимально лёгкие картинки. Лучше для очень слабой сети.",

    ),

    BALANCED(

        "Баланс",

        "стабильно / умеренно",

        "Длинные таймауты без агрессивного сжатия. Хорошо для нестабильного мобильного интернета.",

    ),

    FAST(

        "Макс",

        "Wi-Fi / быстро",

        "Обычные таймауты и без экономии на картинках. Лучше для нормального интернета.",

    ),

    ;



    companion object {

        fun fromFlags(slowNetwork: Boolean, liteImagesEnabled: Boolean): QuickSpeedMode = when {

            slowNetwork && liteImagesEnabled -> ECO

            slowNetwork -> BALANCED

            else -> FAST

        }



        fun next(current: QuickSpeedMode): QuickSpeedMode = when (current) {

            ECO -> BALANCED

            BALANCED -> FAST

            FAST -> ECO

        }

    }

}



@Composable

fun BottomSearchBar(

    externalValue: String,

    searchEngine: com.baddysays.saylat.search.SearchEngine,

    enabled: Boolean,

    speedMode: QuickSpeedMode,

    onSpeedModeChange: (QuickSpeedMode) -> Unit,

    onSearch: (String) -> Unit,

    modifier: Modifier = Modifier,

    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,

    uiLanguage: AppLanguage = AppLanguage.RU,

    historySuggestions: List<HistoryEntry> = emptyList(),

    onHistorySelect: (String) -> Unit = {},

) {

    var draft by rememberSaveable { mutableStateOf(externalValue) }

    var modeMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(externalValue) {

        if (externalValue != draft) draft = externalValue

    }



    Surface(

        modifier = modifier

            .fillMaxWidth()

            .navigationBarsPadding()

            .imePadding(),

        color = MaterialTheme.colorScheme.surface,

        tonalElevation = 0.dp,

        shadowElevation = 3.dp,

        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),

    ) {

        Column {

            HistorySuggestions(
                suggestions = historySuggestions,
                onSelect = { url ->
                    draft = url
                    onHistorySelect(url)
                    onSearch(url)
                },
            )

        Row(

            modifier = Modifier

                .fillMaxWidth()

                .padding(horizontal = 10.dp, vertical = 8.dp),

            verticalAlignment = Alignment.CenterVertically,

        ) {

            Box {

                IconButton(

                    onClick = { modeMenuOpen = true },

                    enabled = enabled,

                ) {

                    Icon(

                        Icons.Default.Tune,

                        contentDescription = "Режим сети: ${speedMode.title}",

                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),

                    )

                }

                DropdownMenu(

                    expanded = modeMenuOpen,

                    onDismissRequest = { modeMenuOpen = false },

                ) {

                    QuickSpeedMode.entries.forEach { mode ->

                        DropdownMenuItem(

                            text = {

                                Text(

                                    if (mode == speedMode) "${mode.title} ✓" else mode.title,

                                    fontWeight = if (mode == speedMode) FontWeight.SemiBold else FontWeight.Normal,

                                )

                            },

                            onClick = {

                                onSpeedModeChange(mode)

                                modeMenuOpen = false

                            },

                        )

                    }

                }

            }

            OutlinedTextField(

                value = draft,

                onValueChange = { draft = it },

                modifier = Modifier

                    .weight(1f)

                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),

                singleLine = true,

                placeholder = {
                    Text(SaylatStrings.searchPlaceholder(uiLanguage), style = MaterialTheme.typography.bodyMedium)
                },

                shape = RoundedCornerShape(22.dp),

                enabled = enabled,

                colors = OutlinedTextFieldDefaults.colors(

                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),

                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),

                ),

            )

            FilledIconButton(

                onClick = { onSearch(draft.trim()) },

                enabled = enabled && draft.isNotBlank(),

                modifier = Modifier.padding(start = 6.dp),

                colors = IconButtonDefaults.filledIconButtonColors(

                    containerColor = MaterialTheme.colorScheme.primary,

                ),

            ) {

                Icon(Icons.Default.Search, contentDescription = "Искать · ${searchEngine.label}")

            }

        }

        }

    }

}



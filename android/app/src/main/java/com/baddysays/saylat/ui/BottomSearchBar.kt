package com.baddysays.saylat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baddysays.saylat.search.SearchEngine

enum class QuickSpeedMode(val title: String, val subtitle: String) {
    ECO("Эко", "2G"),
    BALANCED("Баланс", "умеренно"),
    FAST("Макс", "быстро"),
}

@Composable
fun BottomSearchBar(
    externalValue: String,
    searchEngine: SearchEngine,
    enabled: Boolean,
    speedMode: QuickSpeedMode,
    onSpeedModeChange: (QuickSpeedMode) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by rememberSaveable { mutableStateOf(externalValue) }
    LaunchedEffect(externalValue) {
        if (externalValue != draft) draft = externalValue
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    searchEngine.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuickSpeedMode.entries.forEach { mode ->
                        FilterChip(
                            selected = speedMode == mode,
                            onClick = { onSpeedModeChange(mode) },
                            label = {
                                Text(
                                    mode.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (speedMode == mode) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            ),
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Поиск или URL") },
                    shape = RoundedCornerShape(28.dp),
                    enabled = enabled,
                )
                FilledIconButton(
                    onClick = { onSearch(draft.trim()) },
                    enabled = enabled && draft.isNotBlank(),
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Искать")
                }
            }
        }
    }
}

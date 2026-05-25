package com.baddysays.saylat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReaderLayoutBar(
    useSmart: Boolean,
    smartAvailable: Boolean,
    enhancing: Boolean,
    onSelectBaseline: () -> Unit,
    onSelectSmart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = !useSmart,
            onClick = onSelectBaseline,
            label = { Text("Базовая") },
            shape = RoundedCornerShape(12.dp),
        )
        FilterChip(
            selected = useSmart,
            onClick = onSelectSmart,
            enabled = smartAvailable && !enhancing,
            label = { Text(if (enhancing) "Умная…" else "Умная") },
            shape = RoundedCornerShape(12.dp),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        )
    }
}

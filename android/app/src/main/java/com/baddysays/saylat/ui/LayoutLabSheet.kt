package com.baddysays.saylat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baddysays.saylat.engine.LayoutLabResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutLabSheet(
    visible: Boolean,
    loading: Boolean,
    result: LayoutLabResult?,
    smartAvailable: Boolean,
    smartHint: String?,
    onDismiss: () -> Unit,
    onOpenBaseline: () -> Unit,
    onOpenSmart: () -> Unit,
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Сравнение вёрстки", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Одна и та же страница: быстрая лента и улучшение ИИ (прототип). Без смайликов и анимаций — только структура карточек.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )

            if (loading) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                    Text("Загружаем страницу и считаем планы…")
                }
            }

            result?.let { r ->
                Text(r.title, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(
                    r.url,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    CompareColumn(
                        title = "Базовая",
                        cards = r.baselineCards,
                        hidden = r.baselineHiddenBlocks,
                        merged = r.baselineMergedGroups,
                        modifier = Modifier.weight(1f),
                    )
                    CompareColumn(
                        title = "Умная",
                        cards = r.smartCards,
                        hidden = r.smartHiddenBlocks,
                        merged = r.smartMergedGroups,
                        subtitle = r.smartSourceLabel,
                        modifier = Modifier.weight(1f),
                        highlight = true,
                    )
                }

                Text(
                    "Скрыто блоков: меньше шума. Объединено абзацев: длиннее удобные карточки.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onOpenBaseline, modifier = Modifier.weight(1f)) {
                        Text("Открыть базовую")
                    }
                    Button(
                        onClick = onOpenSmart,
                        enabled = smartAvailable,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("Открыть умную")
                    }
                }
                if (!smartAvailable) {
                    smartHint?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareColumn(
    title: String,
    cards: Int,
    hidden: Int,
    merged: Int,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    highlight: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (highlight) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            MetricLine("Карточек", cards.toString())
            MetricLine("Скрыто", hidden.toString())
            MetricLine("Склейка", merged.toString())
        }
    }
}

@Composable
private fun MetricLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

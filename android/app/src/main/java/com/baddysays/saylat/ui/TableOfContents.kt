package com.baddysays.saylat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class TocEntry(
    val text: String,
    val level: Int,       // 1–4, h1–h4
    val blockIndex: Int,  // индекс в LazyColumn для прокрутки
)

/**
 * Построить оглавление из списка блоков статьи.
 * blocks — List<SaylatBlock> или любой объект с полями type и text.
 * startOffset — сдвиг индекса если в LazyColumn есть элементы до блоков (заголовок статьи и т.п.)
 */
fun buildToc(blocks: List<*>, startOffset: Int = 1): List<TocEntry> {
    val entries = mutableListOf<TocEntry>()
    blocks.forEachIndexed { index, block ->
        if (block == null) return@forEachIndexed
        try {
            val type = block.javaClass.getMethod("getType").invoke(block) as? String ?: return@forEachIndexed
            if (type != "heading") return@forEachIndexed
            val text = block.javaClass.getMethod("getText").invoke(block) as? String ?: return@forEachIndexed
            val level = try {
                (block.javaClass.getMethod("getLevel").invoke(block) as? Int) ?: 2
            } catch (_: Exception) { 2 }
            if (text.isNotBlank()) {
                entries.add(TocEntry(text = text.trim(), level = level, blockIndex = index + startOffset))
            }
        } catch (_: Exception) { }
    }
    return entries
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableOfContentsSheet(
    entries: List<TocEntry>,
    listState: LazyListState,
    scope: CoroutineScope,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                "Содержание",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            HorizontalDivider(thickness = 0.5.dp)

            if (entries.isEmpty()) {
                Text(
                    "Заголовки не найдены",
                    modifier = Modifier.padding(20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    itemsIndexed(entries) { _, entry ->
                        TocEntryRow(
                            entry = entry,
                            onClick = {
                                scope.launch {
                                    listState.animateScrollToItem(
                                        index = entry.blockIndex,
                                        scrollOffset = -16,
                                    )
                                    onDismiss()
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TocEntryRow(
    entry: TocEntry,
    onClick: () -> Unit,
) {
    // Отступ по уровню заголовка
    val indentDp = (entry.level - 1) * 16
    val fontSize = when (entry.level) {
        1 -> 15.sp
        2 -> 14.sp
        else -> 13.sp
    }
    val fontWeight = if (entry.level <= 2) FontWeight.Medium else FontWeight.Normal
    val textColor = if (entry.level <= 2)
        MaterialTheme.colorScheme.onSurface
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                start = (20 + indentDp).dp,
                end = 20.dp,
                top = 10.dp,
                bottom = 10.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (entry.level > 1) {
            Spacer(modifier = Modifier.width(4.dp))
            // Маленький индикатор вложенности
            Text(
                "·",
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = entry.text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = textColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Кнопка для TopBar — показывает TocSheet.
 * Показывать только если entries.isNotEmpty()
 *
 * Пример использования в SaylatTopBar:
 *
 * var showToc by remember { mutableStateOf(false) }
 * val tocEntries = remember(article) { buildToc(article?.blocks ?: emptyList()) }
 * if (tocEntries.isNotEmpty()) {
 *     TocButton(onClick = { showToc = true })
 * }
 * if (showToc) {
 *     TableOfContentsSheet(
 *         entries = tocEntries,
 *         listState = readerListState,
 *         scope = coroutineScope,
 *         onDismiss = { showToc = false },
 *     )
 * }
 */
@Composable
fun TocButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = "Содержание статьи",
        )
    }
}

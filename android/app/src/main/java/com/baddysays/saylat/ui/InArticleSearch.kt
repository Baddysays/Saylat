package com.baddysays.saylat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────
// Модель состояния поиска
// ─────────────────────────────────────────────────────────────

data class ArticleSearchState(
    val query: String = "",
    val visible: Boolean = false,
    val currentMatch: Int = 0,
    val totalMatches: Int = 0,
)

/**
 * Подсветить все вхождения query в тексте.
 * Возвращает AnnotatedString с подсвеченными совпадениями.
 * currentMatch — индекс текущего (другой цвет).
 */
fun highlightMatches(
    text: String,
    query: String,
    currentMatchIndex: Int = -1,
    matchIndexOffset: Int = 0,
    highlightColor: androidx.compose.ui.graphics.Color,
    currentHighlightColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
): AnnotatedString {
    if (query.isBlank() || text.isBlank()) {
        return AnnotatedString(text)
    }
    return buildAnnotatedString {
        var lastEnd = 0
        var matchCount = matchIndexOffset
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        var idx = lowerText.indexOf(lowerQuery)
        while (idx >= 0) {
            // Текст до совпадения
            withStyle(SpanStyle(color = textColor)) {
                append(text.substring(lastEnd, idx))
            }
            // Само совпадение
            val isCurrent = matchCount == currentMatchIndex
            withStyle(
                SpanStyle(
                    color = if (isCurrent) currentHighlightColor else highlightColor,
                    background = if (isCurrent)
                        currentHighlightColor.copy(alpha = 0.25f)
                    else
                        highlightColor.copy(alpha = 0.20f),
                )
            ) {
                append(text.substring(idx, idx + lowerQuery.length))
            }
            lastEnd = idx + lowerQuery.length
            matchCount++
            idx = lowerText.indexOf(lowerQuery, lastEnd)
        }
        withStyle(SpanStyle(color = textColor)) {
            append(text.substring(lastEnd))
        }
    }
}

/**
 * Подсчитать количество совпадений query в списке текстов.
 */
fun countMatches(texts: List<String>, query: String): Int {
    if (query.isBlank()) return 0
    val lowerQuery = query.lowercase()
    return texts.sumOf { text ->
        var count = 0
        var idx = text.lowercase().indexOf(lowerQuery)
        while (idx >= 0) {
            count++
            idx = text.lowercase().indexOf(lowerQuery, idx + 1)
        }
        count
    }
}

// ─────────────────────────────────────────────────────────────
// UI панели поиска
// ─────────────────────────────────────────────────────────────

/**
 * Панель поиска по статье — размещается над BottomSearchBar или под TopBar.
 *
 * Использование:
 *   InArticleSearchBar(
 *       state = searchState,
 *       onQueryChange = { searchState = searchState.copy(query = it) },
 *       onNext = { /* прокрутить к следующему совпадению */ },
 *       onPrev = { /* прокрутить к предыдущему */ },
 *       onClose = { searchState = searchState.copy(visible = false, query = "") },
 *   )
 */
@Composable
fun InArticleSearchBar(
    state: ArticleSearchState,
    onQueryChange: (String) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(state.visible) {
        if (state.visible) focusRequester.requestFocus()
    }

    AnimatedVisibility(
        visible = state.visible,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = { Text("Найти в статье…", style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                trailingIcon = {
                    if (state.totalMatches > 0) {
                        Text(
                            "${state.currentMatch + 1}/${state.totalMatches}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onNext() }),
            )
            IconButton(onClick = onPrev, enabled = state.totalMatches > 1) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Предыдущее совпадение")
            }
            IconButton(onClick = onNext, enabled = state.totalMatches > 1) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Следующее совпадение")
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Закрыть поиск")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Логика прокрутки к совпадению
// ─────────────────────────────────────────────────────────────

/**
 * Найти индексы блоков содержащих совпадения.
 * Возвращает список (blockIndex, matchIndexInBlock) для навигации.
 */
fun findMatchPositions(
    blockTexts: List<String>,
    query: String,
    startOffset: Int = 0,
): List<Pair<Int, Int>> {
    if (query.isBlank()) return emptyList()
    val result = mutableListOf<Pair<Int, Int>>()
    val lowerQuery = query.lowercase()
    blockTexts.forEachIndexed { blockIdx, text ->
        val lowerText = text.lowercase()
        var idx = lowerText.indexOf(lowerQuery)
        while (idx >= 0) {
            result.add(Pair(blockIdx + startOffset, idx))
            idx = lowerText.indexOf(lowerQuery, idx + 1)
        }
    }
    return result
}

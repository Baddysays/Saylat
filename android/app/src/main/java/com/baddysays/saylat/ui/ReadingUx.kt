@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.baddysays.saylat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────
// Прогресс чтения
// ─────────────────────────────────────────────────────────────

/**
 * Тонкая полоска прогресса чтения под TopBar.
 * Использует LazyListState для определения позиции.
 *
 * Использование:
 *   val listState = rememberLazyListState()
 *   ReadingProgressBar(listState = listState, totalItems = article.blocks.size)
 *   LazyColumn(state = listState) { ... }
 */
@Composable
fun ReadingProgressBar(
    listState: LazyListState,
    totalItems: Int,
    modifier: Modifier = Modifier,
) {
    if (totalItems <= 0) return

    val progress by remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            // Учитываем частично видимый последний элемент
            val visibleFraction = run {
                val info = layoutInfo.visibleItemsInfo.lastOrNull() ?: return@run 0f
                val viewportEnd = layoutInfo.viewportEndOffset
                val itemEnd = info.offset + info.size
                if (itemEnd > viewportEnd) {
                    (viewportEnd - info.offset).toFloat() / info.size.toFloat()
                } else 1f
            }
            ((lastVisible + visibleFraction) / totalItems.toFloat()).coerceIn(0f, 1f)
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "reading_progress",
    )

    AnimatedVisibility(
        visible = progress > 0.01f && progress < 0.99f,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxWidth(),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shadowElevation = 2.dp,
        ) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Square,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Время чтения
// ─────────────────────────────────────────────────────────────

/**
 * Подсчитать время чтения по списку блоков статьи.
 * Возвращает строку "~N мин" или null если текста слишком мало.
 */
fun calculateReadingTime(blocks: List<Any?>, wordsPerMinute: Int = 200): String? {
    // Работает с любым типом блоков у которых есть поле text
    // Используем рефлексию чтобы не зависеть от конкретного типа
    val totalWords = blocks.sumOf { block ->
        if (block == null) return@sumOf 0
        try {
            val text = block.javaClass.getMethod("getText").invoke(block) as? String
                ?: (block.javaClass.getField("text").get(block) as? String)
                ?: ""
            text.split(Regex("\\s+")).count { it.length > 2 }
        } catch (_: Exception) { 0 }
    }
    if (totalWords < 100) return null
    val minutes = (totalWords.toFloat() / wordsPerMinute).roundToInt().coerceAtLeast(1)
    return "~$minutes мин"
}

/**
 * Версия для SaylatArticle.blocks напрямую.
 * Передавать article.blocks as List<*>
 */
fun readingTimeFromText(plainText: String, wordsPerMinute: Int = 200): String? {
    val words = plainText.split(Regex("\\s+")).count { it.length > 2 }
    if (words < 80) return null
    val minutes = (words.toFloat() / wordsPerMinute).roundToInt().coerceAtLeast(1)
    return "~$minutes мин"
}

// ─────────────────────────────────────────────────────────────
// Swipe-back жест
// ─────────────────────────────────────────────────────────────

private enum class SwipeState { IDLE, DISMISSED }

/**
 * Обёртка добавляющая swipe-right-to-go-back к любому экрану.
 *
 * Использование:
 *   SwipeBackContainer(onBack = viewModel::backFromReader) {
 *       ReaderContent(...)
 *   }
 */
@Composable
fun SwipeBackContainer(
    onBack: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        content()
        return
    }

    val density = LocalDensity.current
    val screenWidthPx = with(density) { 400.dp.toPx() } // приблизительная ширина

    val state = remember {
        AnchoredDraggableState(
            initialValue = SwipeState.IDLE,
            anchors = DraggableAnchors {
                SwipeState.IDLE at 0f
                SwipeState.DISMISSED at screenWidthPx
            },
            positionalThreshold = { totalDistance -> totalDistance * 0.4f },
            velocityThreshold = { with(density) { 125.dp.toPx() } },
            snapAnimationSpec = spring(),
            decayAnimationSpec = androidx.compose.animation.core.exponentialDecay(),
        )
    }

    // Триггерим onBack когда пользователь доswipал до конца
    val currentValue = state.currentValue
    val targetValue = state.targetValue
    if (targetValue == SwipeState.DISMISSED || currentValue == SwipeState.DISMISSED) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset {
                IntOffset(
                    x = state.requireOffset().roundToInt().coerceAtLeast(0),
                    y = 0,
                )
            }
            .anchoredDraggable(
                state = state,
                orientation = Orientation.Horizontal,
            ),
    ) {
        content()
    }
}

package com.baddysays.saylat.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────
// Базовый shimmer-brush
// ─────────────────────────────────────────────────────────────

@Composable
fun shimmerBrush(
    baseColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    highlightColor: Color = MaterialTheme.colorScheme.surface,
): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_offset",
    )
    return Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(offset * 400f, 0f),
        end = Offset(offset * 400f + 600f, 400f),
    )
}

// ─────────────────────────────────────────────────────────────
// Строительные блоки
// ─────────────────────────────────────────────────────────────

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    cornerRadius: Dp = 6.dp,
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(shimmerBrush()),
    )
}

// ─────────────────────────────────────────────────────────────
// Скелетон карточки статьи
// ─────────────────────────────────────────────────────────────

@Composable
fun ArticleCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Заголовок — 2 строки
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.85f), height = 22.dp)
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.60f), height = 22.dp)
        Spacer(Modifier.height(4.dp))
        // Автор
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.40f), height = 13.dp)
        Spacer(Modifier.height(8.dp))
        // Параграфы
        repeat(4) {
            ShimmerBox(modifier = Modifier.fillMaxWidth(), height = 15.dp)
        }
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.70f), height = 15.dp)
        Spacer(Modifier.height(8.dp))
        // Изображение-заглушка
        ShimmerBox(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            cornerRadius = 12.dp,
        )
        Spacer(Modifier.height(8.dp))
        // Ещё параграфы
        repeat(3) {
            ShimmerBox(modifier = Modifier.fillMaxWidth(), height = 15.dp)
        }
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.55f), height = 15.dp)
    }
}

// ─────────────────────────────────────────────────────────────
// Скелетон элемента ленты
// ─────────────────────────────────────────────────────────────

@Composable
fun FeedItemSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Аватар источника
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(shimmerBrush()),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.50f), height = 12.dp)
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.90f), height = 16.dp)
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.75f), height = 16.dp)
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.40f), height = 13.dp)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Скелетон результата поиска
// ─────────────────────────────────────────────────────────────

@Composable
fun SearchResultSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.30f), height = 12.dp)
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.85f), height = 18.dp)
        ShimmerBox(modifier = Modifier.fillMaxWidth(), height = 14.dp)
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.70f), height = 14.dp)
    }
}

// ─────────────────────────────────────────────────────────────
// Полноэкранные скелетон-экраны
// ─────────────────────────────────────────────────────────────

@Composable
fun ArticleLoadingSkeleton() {
    LazyColumn {
        item { ArticleCardSkeleton() }
    }
}

@Composable
fun FeedLoadingSkeleton(itemCount: Int = 6) {
    LazyColumn {
        items(itemCount) {
            FeedItemSkeleton()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .padding(horizontal = 16.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}

@Composable
fun SearchLoadingSkeleton(itemCount: Int = 5) {
    LazyColumn {
        items(itemCount) { SearchResultSkeleton() }
    }
}

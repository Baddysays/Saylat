package com.baddysays.saylat.ui.pet

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Реплика: видимая часть текста и курсор (тайминг — в [TamagotchiController]). */
@Composable
fun PetSpeechBubble(
    fullText: String,
    visibleChars: Int,
    showCursor: Boolean,
    modifier: Modifier = Modifier,
) {
    val cursorBlink = rememberInfiniteTransition(label = "petCursor")
    val cursorAlpha by cursorBlink.animateFloat(
        initialValue = 0.15f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "cursorAlpha",
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    ) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                fullText.take(visibleChars.coerceIn(0, fullText.length)),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
            )
            if (showCursor) {
                Text(
                    "▌",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = cursorAlpha),
                )
            }
        }
    }
}

fun speechCharDelayMs(textLength: Int): Long = when {
    textLength > 120 -> 38L
    textLength > 70 -> 48L
    else -> 58L
}

fun speechReadingMs(textLength: Int): Long =
    (2_800L + textLength * 42L).coerceIn(3_200L, 12_000L)

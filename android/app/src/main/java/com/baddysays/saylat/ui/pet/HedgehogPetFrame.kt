package com.baddysays.saylat.ui.pet

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class HedgehogFrameVariant {
    /** Плавающая кнопка при загрузке. */
    Fab,
    /** Большая сцена в карточке питомца. */
    Stage,
    /** Чип «Готово» / ошибка. */
    Chip,
    /** Миниатюра в шапке. */
    Mini,
    /** Без декора (магазин и т.п.). */
    Plain,
}

object HedgehogPetColors {
    val cream = Color(0xFFFFF8EE)
    val meadow = Color(0xFFE6F4EA)
    val warmGlow = Color(0xFFFFE0B2)
    val spine = Color(0xFFC07838)
    val border = Color(0xFF3D2814).copy(alpha = 0.22f)
}

@Composable
fun HedgehogPetFrame(
    modifier: Modifier = Modifier,
    variant: HedgehogFrameVariant = HedgehogFrameVariant.Stage,
    loadingGlow: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    when (variant) {
        HedgehogFrameVariant.Plain -> Box(modifier, contentAlignment = Alignment.Center, content = content)
        HedgehogFrameVariant.Fab -> FabFrame(modifier, loadingGlow, content)
        else -> CardLikeFrame(modifier, variant, loadingGlow, content)
    }
}

@Composable
private fun FabFrame(
    modifier: Modifier,
    loadingGlow: Boolean,
    content: @Composable BoxScope.() -> Unit,
) {
    val pulse = rememberInfiniteTransition(label = "fabGlow")
    val glowAlpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
        label = "fabGlowA",
    )
    val outerGlow = if (loadingGlow) {
        Modifier.shadow(
            elevation = 14.dp,
            shape = CircleShape,
            ambientColor = HedgehogPetColors.warmGlow.copy(alpha = glowAlpha),
            spotColor = HedgehogPetColors.spine.copy(alpha = glowAlpha * 0.6f),
        )
    } else {
        Modifier.shadow(6.dp, CircleShape)
    }
    Surface(
        modifier = modifier.then(outerGlow),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
        tonalElevation = 4.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(2.dp, HedgehogPetColors.border),
    ) {
        Box(
            Modifier
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.55f),
                            HedgehogPetColors.meadow.copy(alpha = 0.85f),
                            HedgehogPetColors.cream,
                        ),
                    ),
                )
                .padding(6.dp),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

@Composable
private fun CardLikeFrame(
    modifier: Modifier,
    variant: HedgehogFrameVariant,
    loadingGlow: Boolean,
    content: @Composable BoxScope.() -> Unit,
) {
    val corner: Dp
    val pad: Dp
    val elevation: Dp
    when (variant) {
        HedgehogFrameVariant.Stage -> {
            corner = 22.dp
            pad = 10.dp
            elevation = 2.dp
        }
        HedgehogFrameVariant.Chip -> {
            corner = 14.dp
            pad = 4.dp
            elevation = 0.dp
        }
        HedgehogFrameVariant.Mini -> {
            corner = 10.dp
            pad = 3.dp
            elevation = 0.dp
        }
        else -> {
            corner = 16.dp
            pad = 6.dp
            elevation = 1.dp
        }
    }
    val shape = RoundedCornerShape(corner)
    val pulse = rememberInfiniteTransition(label = "stageGlow")
    val glowAlpha by pulse.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "stageGlowA",
    )
    Box(
        modifier = modifier
            .clip(shape)
            .then(if (loadingGlow) Modifier.shadow(8.dp, shape) else Modifier)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.7f),
                        if (loadingGlow) {
                            HedgehogPetColors.warmGlow.copy(alpha = 0.25f + glowAlpha * 0.2f)
                        } else {
                            HedgehogPetColors.meadow.copy(alpha = 0.55f)
                        },
                        HedgehogPetColors.cream,
                    ),
                ),
            )
            .border(
                width = if (variant == HedgehogFrameVariant.Stage) 2.dp else 1.dp,
                color = if (loadingGlow) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f + glowAlpha * 0.25f)
                } else {
                    HedgehogPetColors.border
                },
                shape = shape,
            )
            .padding(pad),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

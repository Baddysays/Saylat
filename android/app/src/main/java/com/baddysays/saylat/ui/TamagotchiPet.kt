package com.baddysays.saylat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

// ---- Data model ----

data class TamagotchiStats(
    val health: Float = 100f,
    val hunger: Float = 100f,
    val attention: Float = 100f,
)

enum class TamagotchiMood {
    HAPPY, HUNGRY, SICK, EXCITED, SLEEPY, DEAD
}

private val SALAD_REACTIONS = listOf(
    "Вкусный салатик! 🥗",
    "Ням-ням! Ещё?",
    "Салатик — сила!",
    "Обожаю зелень!",
    "Хрум-хрум!",
    "Спасибо, друг!",
)

private val TAP_REACTIONS = listOf(
    "Привет! Я скучал!",
    "Не уходи!",
    "Ты мой друг!",
    "Почеши меня!",
    "Страница грузится...",
    "Ещё чуть-чуть!",
    "Я жду!",
    "Тук-тук!",
)

private val HUNGRY_REACTIONS = listOf(
    "Я голоден! Дай салат!",
    "Живот урчит...",
    "Салатика бы...",
    "Покорми меня!",
)

private val SICK_REACTIONS = listOf(
    "Мне плохо...",
    "Интернет, где ты?",
    "Помоги!",
)

// ---- Game logic ----

class TamagotchiController(
    val onReady: () -> Unit,
) {
    var stats by mutableStateOf(TamagotchiStats())
    var mood by mutableStateOf(TamagotchiMood.HAPPY)
    var reaction by mutableStateOf("Привет!")
    var reactionVisible by mutableStateOf(false)
    var showBounce by mutableStateOf(false)
    var showNibble by mutableStateOf(false)
    var saladCount by mutableIntStateOf(0)
    var isVisible by mutableStateOf(false)
    var isReady by mutableStateOf(false)

    private var active = false

    fun start() {
        active = true
        isVisible = false
        isReady = false
        stats = TamagotchiStats()
        mood = TamagotchiMood.HAPPY
        reaction = "Привет!"
        saladCount = 3
    }

    fun stop() {
        active = false
        isReady = true
    }

    fun tick() {
        if (!active || isReady) return
        if (isVisible) {
            val newHunger = (stats.hunger - 1.5f).coerceAtLeast(0f)
            val newHealth = if (newHunger < 20) (stats.health - 0.8f).coerceAtLeast(0f) else stats.health
            val newAttention = (stats.attention - 0.5f).coerceAtLeast(0f)

            stats = TamagotchiStats(
                health = newHealth,
                hunger = newHunger,
                attention = newAttention,
            )

            mood = when {
                newHealth <= 0 -> TamagotchiMood.DEAD
                newHealth < 25 -> TamagotchiMood.SICK
                newHunger < 20 -> TamagotchiMood.HUNGRY
                newAttention < 30 -> TamagotchiMood.SLEEPY
                else -> TamagotchiMood.HAPPY
            }

            if (mood == TamagotchiMood.HUNGRY && saladCount > 0) {
                reaction = HUNGRY_REACTIONS.random()
                reactionVisible = true
            }
        }
    }

    fun onTapPet() {
        if (!isVisible || isReady) return
        showBounce = true
        val newAttention = (stats.attention + 12f).coerceAtMost(100f)
        stats = stats.copy(attention = newAttention)
        reaction = TAP_REACTIONS.random()
        reactionVisible = true
        mood = TamagotchiMood.EXCITED
    }

    fun onFeed() {
        if (!isVisible || isReady || saladCount <= 0) return
        saladCount--
        showNibble = true
        val newHunger = (stats.hunger + 30f).coerceAtMost(100f)
        val newHealth = (stats.health + 5f).coerceAtMost(100f)
        stats = TamagotchiStats(
            health = newHealth,
            hunger = newHunger,
            attention = (stats.attention + 5f).coerceAtMost(100f),
        )
        reaction = SALAD_REACTIONS.random()
        reactionVisible = true
        mood = TamagotchiMood.HAPPY
    }

    fun show() {
        isVisible = true
        reaction = "А вот и я!"
        reactionVisible = true
    }

    fun onReadyClicked() {
        isReady = false
        onReady()
    }
}

// ---- Composable ----

@Composable
fun rememberTamagotchiController(onReady: () -> Unit): TamagotchiController {
    return remember { TamagotchiController(onReady) }
}

@Composable
fun TamagotchiLoadingBanner(
    controller: TamagotchiController,
    url: String,
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
    // Tick every 2 seconds
    LaunchedEffect(controller.isVisible) {
        while (loading && !controller.isReady) {
            delay(2000)
            controller.tick()
        }
    }

    // Show tamagotchi after 10 seconds
    LaunchedEffect(loading) {
        if (loading) {
            controller.start()
            delay(10_000)
            if (loading && !controller.isReady) {
                controller.show()
            }
        }
    }

    // Hide reaction after 3 seconds
    LaunchedEffect(controller.reactionVisible) {
        if (controller.reactionVisible) {
            delay(2500)
            controller.reactionVisible = false
        }
    }

    // Reset bounce/nibble after animation
    LaunchedEffect(controller.showBounce) {
        if (controller.showBounce) {
            delay(400)
            controller.showBounce = false
        }
    }
    LaunchedEffect(controller.showNibble) {
        if (controller.showNibble) {
            delay(600)
            controller.showNibble = false
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(
            visible = controller.isVisible && !controller.isReady,
            enter = slideInVertically(tween(500)) { it },
        ) {
            TamagotchiCard(controller, url)
        }

        if (!controller.isVisible && !controller.isReady && loading) {
            ReaderPageLoadProgress(url = url)
        }
    }
}

@Composable
private fun TamagotchiCard(
    controller: TamagotchiController,
    url: String,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
        ),
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Stats bars
            StatBar("❤", controller.stats.health, Color(0xFFE57373))
            Spacer(Modifier.height(2.dp))
            StatBar("🍽", controller.stats.hunger, Color(0xFFFFB74D))
            Spacer(Modifier.height(2.dp))
            StatBar("⭐", controller.stats.attention, Color(0xFF64B5F6))

            Spacer(Modifier.height(8.dp))

            // Pet and feed buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Feed button on the left
                IconButton(
                    onClick = { controller.onFeed() },
                    enabled = controller.saladCount > 0,
                    modifier = Modifier.size(44.dp),
                ) {
                    Text("🥗", fontSize = 22.sp)
                }

                // Pet canvas
                TamagotchiCanvas(
                    mood = controller.mood,
                    showBounce = controller.showBounce,
                    showNibble = controller.showNibble,
                    onTap = { controller.onTapPet() },
                    modifier = Modifier.size(120.dp),
                )

                // Tap prompt on the right
                IconButton(
                    onClick = { controller.onTapPet() },
                    modifier = Modifier.size(44.dp),
                ) {
                    Text("👉", fontSize = 18.sp)
                }
            }

            // Salad count
            Text(
                "🥗 × ${controller.saladCount}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )

            // Reaction text
            if (controller.reactionVisible) {
                Text(
                    controller.reaction,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }

            // Loading mini info
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    url.take(60).ifBlank { "Загружаем..." },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun StatBar(label: String, value: Float, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label, fontSize = 12.sp, modifier = Modifier.width(20.dp))
        val barColor = when {
            value < 25 -> Color(0xFFEF5350)
            value < 50 -> Color(0xFFFFB74D)
            else -> color
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((value / 100f).coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(barColor.copy(alpha = 0.6f), barColor),
                        ),
                    ),
            )
        }
        Text(
            "${value.roundToInt()}%",
            fontSize = 10.sp,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun TamagotchiCanvas(
    mood: TamagotchiMood,
    showBounce: Boolean,
    showNibble: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pet_idle")
    val idleBob by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bob",
    )

    val bounceOffset by animateFloatAsState(
        targetValue = if (showBounce) -8f else idleBob * 3f,
        animationSpec = tween(200),
        label = "bounce",
    )

    val nibbleScale by animateFloatAsState(
        targetValue = if (showNibble) 1.15f else 1f,
        animationSpec = tween(150),
        label = "nibble",
    )

    val petColor = when (mood) {
        TamagotchiMood.HAPPY -> Color(0xFF81C784)
        TamagotchiMood.EXCITED -> Color(0xFFAED581)
        TamagotchiMood.HUNGRY -> Color(0xFFFFCC80)
        TamagotchiMood.SICK -> Color(0xFFB0BEC5)
        TamagotchiMood.SLEEPY -> Color(0xFFBCAAA4)
        TamagotchiMood.DEAD -> Color(0xFF90A4AE)
    }

    val eyeState = when (mood) {
        TamagotchiMood.HAPPY, TamagotchiMood.EXCITED -> EyeState.OPEN
        TamagotchiMood.SLEEPY -> EyeState.HALF
        TamagotchiMood.DEAD -> EyeState.CROSS
        else -> EyeState.OPEN
    }

    Canvas(
        modifier = modifier
            .offset { IntOffset(0, bounceOffset.roundToInt()) }
            .pointerInput(Unit) {
                detectTapGestures { onTap() }
            },
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val scale = nibbleScale / 1f

        drawCircle(
            color = petColor.copy(alpha = 0.3f),
            radius = w * 0.42f * scale,
            center = Offset(cx, cy + 6.dp.toPx()),
        )

        // Body — rounded blob
        val bodyWidth = w * 0.55f * scale
        val bodyHeight = h * 0.5f * scale
        drawRoundRect(
            color = petColor,
            topLeft = Offset(cx - bodyWidth / 2f, cy - bodyHeight / 2f),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(bodyHeight * 0.5f),
        )

        // Small crown — 3 little leaves on top (salad theme!)
        val leafColor = Color(0xFF66BB6A)
        val leafY = cy - bodyHeight / 2f - 4.dp.toPx()
        for (dx in listOf(-8f, 0f, 8f)) {
            drawCircle(
                color = leafColor,
                radius = 5.dp.toPx(),
                center = Offset(cx + dx * scale, leafY),
            )
        }

        // Eyes
        val eyeY = cy - bodyHeight * 0.12f
        val eyeSpacing = bodyWidth * 0.2f * scale
        val eyeRadius = 6.dp.toPx() * scale
        val pupilRadius = 3.dp.toPx() * scale

        when (eyeState) {
            EyeState.OPEN -> {
                for (side in listOf(-1f, 1f)) {
                    val ex = cx + side * eyeSpacing
                    // White
                    drawCircle(Color.White, eyeRadius + 1.dp.toPx(), Offset(ex, eyeY))
                    // Pupil
                    drawCircle(Color(0xFF263238), pupilRadius, Offset(ex, eyeY))
                }
            }
            EyeState.HALF -> {
                for (side in listOf(-1f, 1f)) {
                    val ex = cx + side * eyeSpacing
                    drawCircle(Color.White, eyeRadius, Offset(ex, eyeY))
                    drawCircle(Color(0xFF263238), pupilRadius, Offset(ex, eyeY + pupilRadius))
                }
            }
            EyeState.CROSS -> {
                for (side in listOf(-1f, 1f)) {
                    val ex = cx + side * eyeSpacing
                    val crossLen = 4.dp.toPx()
                    drawLine(
                        Color(0xFF424242),
                        Offset(ex - crossLen, eyeY - crossLen),
                        Offset(ex + crossLen, eyeY + crossLen),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        Color(0xFF424242),
                        Offset(ex + crossLen, eyeY - crossLen),
                        Offset(ex - crossLen, eyeY + crossLen),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }

        // Cheeks
        val cheekY = cy + bodyHeight * 0.05f
        for (side in listOf(-1f, 1f)) {
            drawCircle(
                color = Color(0xFFEF9A9A).copy(alpha = 0.5f),
                radius = 4.dp.toPx() * scale,
                Offset(cx + side * eyeSpacing * 1.5f, cheekY),
            )
        }

        // Mouth
        val mouthY = cy + bodyHeight * 0.18f
        val mouthWidth = eyeSpacing * 1.3f
        when {
            mood == TamagotchiMood.HAPPY || mood == TamagotchiMood.EXCITED -> {
                // Smile arc
                drawArc(
                    color = Color(0xFF424242),
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(cx - mouthWidth / 2f, mouthY - mouthWidth / 4f),
                    size = Size(mouthWidth, mouthWidth / 2f),
                    style = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            showNibble -> {
                // Open mouth — munching
                drawOval(
                    color = Color(0xFF424242),
                    topLeft = Offset(cx - 5.dp.toPx(), mouthY - 2.dp.toPx()),
                    size = Size(10.dp.toPx(), 8.dp.toPx()),
                )
            }
            mood == TamagotchiMood.SLEEPY -> {
                // Sleepy mouth — small o
                drawCircle(
                    color = Color(0xFF757575),
                    radius = 3.dp.toPx(),
                    center = Offset(cx, mouthY + 2.dp.toPx()),
                )
            }
            mood == TamagotchiMood.DEAD -> {
                // Sad line
                drawArc(
                    color = Color(0xFF424242),
                    startAngle = 0f,
                    sweepAngle = -180f,
                    useCenter = false,
                    topLeft = Offset(cx - mouthWidth / 2f, mouthY - mouthWidth / 4f),
                    size = Size(mouthWidth, mouthWidth / 2f),
                    style = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            else -> {
                // Neutral / worried line
                drawLine(
                    color = Color(0xFF757575),
                    start = Offset(cx - mouthWidth / 3f, mouthY),
                    end = Offset(cx + mouthWidth / 3f, mouthY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }

        // Little arms
        val armY = cy + bodyHeight * 0.05f
        for (side in listOf(-1f, 1f)) {
            val armX = cx + side * bodyWidth * 0.42f * scale
            val armEndX = armX + side * 10.dp.toPx() * scale
            drawLine(
                color = petColor,
                start = Offset(armX, armY),
                end = Offset(armEndX, armY - 4.dp.toPx() * (if (showBounce) 1.5f else 1f)),
                strokeWidth = 6.dp.toPx() * scale,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun TamagotchiReadyOverlay(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(300)) + scaleIn(tween(400)),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clickable { } // consume clicks, prevent interaction with page behind
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.95f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            ) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("🥗", fontSize = 48.sp)

                    Text(
                        "Страница загружена!",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        "Тамагочи сыт и счастлив. Можете открыть страницу.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )

                    Button(
                        onClick = onOpen,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Check, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Готово", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private enum class EyeState { OPEN, HALF, CROSS }

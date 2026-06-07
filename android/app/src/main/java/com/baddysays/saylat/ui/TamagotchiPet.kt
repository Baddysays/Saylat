package com.baddysays.saylat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baddysays.saylat.prefs.PetGrowth
import com.baddysays.saylat.prefs.PetProfile
import com.baddysays.saylat.prefs.PetSaladEconomy
import com.baddysays.saylat.prefs.PetWallet
import com.baddysays.saylat.ui.pet.HedgehogFrameVariant
import com.baddysays.saylat.ui.pet.HedgehogPetFrame
import com.baddysays.saylat.ui.pet.HedgehogPetColors
import com.baddysays.saylat.ui.pet.PET_HEDGEHOG_GRID
import com.baddysays.saylat.ui.pet.PetHedgehogEngine
import com.baddysays.saylat.ui.pet.PetAnim
import com.baddysays.saylat.ui.pet.PetCosmetics
import com.baddysays.saylat.prefs.AppLanguage
import com.baddysays.saylat.ui.pet.PetDialogue
import com.baddysays.saylat.ui.pet.PetDialogueMix
import com.baddysays.saylat.tamagotchi.DialogCategory
import com.baddysays.saylat.tamagotchi.PetSiteReactions
import com.baddysays.saylat.ui.pet.PetSpeechBubble
import com.baddysays.saylat.ui.pet.speechCharDelayMs
import com.baddysays.saylat.ui.pet.speechReadingMs
import com.baddysays.saylat.ui.strings.SaylatStrings
import com.baddysays.saylat.ui.pet.SpeechEvent
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random
import java.util.ArrayDeque

// ── Session model ─────────────────────────────────────────────────────────────

data class TamagotchiStats(
    val health: Float = 88f,
    val hunger: Float = 88f,
    val attention: Float = 88f,
)

enum class PetPhase { Hidden, Waiting, Active, PageReady, LoadFailed, Dismissed }

enum class PetMood { HAPPY, HUNGRY, SLEEPY, SICK, EXCITED }

private const val TICK_MS = 3_000L
private const val AUTONOMY_MIN_MS = 6_000L
private const val AUTONOMY_MAX_MS = 14_000L
private const val SLEEP_AUTONOMY_CHANCE = 0.42f
private const val FRAME_MS = 300L
private const val WALK_STEP_MS = 100L
private const val WALK_TRAVEL = 0.72f
private const val READY_CHIP_MS = 2_800L
private const val FAB_BOTTOM_PAD_DP = 76
private const val SPEECH_PAUSE_MS = 3_500L
private const val HEADER_FRAME_MS = 480L
private const val HEADER_ANIM_HOLD_CYCLES = 2
private const val IDLE_SPEECH_INTERVAL_MS = 12_000L

private fun isEggProfile(profile: PetProfile, sessionHatched: Boolean = false): Boolean =
    !profile.petHatched && !sessionHatched && profile.saladsEatenBytes <= 0L

private fun displayProfile(profile: PetProfile, sessionHatched: Boolean): PetProfile =
    if (sessionHatched && profile.isEgg) profile.copy(petHatched = true) else profile

class TamagotchiController(
    private val onAwardXp: (Int) -> Unit,
    private val onSpendSalad: () -> Boolean,
    private val onHatchEgg: () -> Unit,
) {
    var phase by mutableStateOf(PetPhase.Hidden)
    var stats by mutableStateOf(TamagotchiStats())
    var mood by mutableStateOf(PetMood.HAPPY)
    var speech by mutableStateOf<String?>(null)
    var speechVisibleChars by mutableIntStateOf(0)
    var speechSkipRequested by mutableStateOf(false)
    var speechPauseUntilMs by mutableLongStateOf(0L)
    private val pendingSpeechQueue = ArrayDeque<String>()
    var saladCount by mutableIntStateOf(0)
    var frameTick by mutableIntStateOf(0)
    var tapBurst by mutableIntStateOf(0)
    var eatBurst by mutableIntStateOf(0)
    var sessionXp by mutableIntStateOf(0)
    var currentAnim by mutableStateOf(PetAnim.IDLE)
    var walkOffset by mutableFloatStateOf(0f)
    var facingLeft by mutableStateOf(false)
    var isWalking by mutableStateOf(false)
    var cosmetics by mutableStateOf(PetCosmetics())
    var interactionExpanded by mutableStateOf(false)
    var sessionHatched by mutableStateOf(false)
    var uiLanguage by mutableStateOf(AppLanguage.RU)
    var suppressReadySpeech by mutableStateOf(false)

    private var loadKey: String = ""
    private var loadingSpeechIssuedForKey: String? = null
    private var waitingStartedAt: Long = 0L
    private val random = Random(System.nanoTime())

    val showFloatingPet: Boolean get() = when (phase) {
        PetPhase.Waiting -> waitingStartedAt > 0L
        PetPhase.Active -> !interactionExpanded
        else -> false
    }

    fun hasPendingSpeech(): Boolean = pendingSpeechQueue.isNotEmpty()
    val showPetCard: Boolean get() = phase == PetPhase.Active && interactionExpanded
    val showReadyChip: Boolean get() = phase == PetPhase.PageReady
    val showFailedChip: Boolean get() = phase == PetPhase.LoadFailed

    fun sync(
        loading: Boolean,
        enabled: Boolean,
        key: String,
        skipReadyGate: Boolean = false,
        loadFailed: Boolean = false,
        pageUrl: String = "",
    ) {
        if (!enabled) {
            phase = PetPhase.Hidden
            waitingStartedAt = 0L
            return
        }
        if (loading) {
            if (key != loadKey) {
                loadKey = key
                resetSession()
                enterWaiting()
            } else if (
                phase == PetPhase.Dismissed ||
                    phase == PetPhase.Hidden ||
                    phase == PetPhase.PageReady ||
                    phase == PetPhase.LoadFailed
            ) {
                resetSession()
                enterWaiting()
            }
        } else {
            when (phase) {
                PetPhase.Active -> finishActiveSession(skipReadyGate, loadFailed, pageUrl)
                PetPhase.Waiting -> {
                    waitingStartedAt = 0L
                    interactionExpanded = false
                    if (loadFailed) {
                        phase = PetPhase.LoadFailed
                        currentAnim = PetAnim.CRY
                        onLoadFailedSpeech()
                    } else {
                        phase = PetPhase.PageReady
                        currentAnim = PetAnim.CELEBRATE
                        onReadySpeech(pageUrl)
                    }
                }
                else -> {
                    if (loadFailed) {
                        phase = PetPhase.LoadFailed
                        currentAnim = PetAnim.CRY
                        onLoadFailedSpeech()
                    } else {
                        phase = PetPhase.Hidden
                    }
                }
            }
        }
    }

    private fun enterWaiting() {
        phase = PetPhase.Waiting
        waitingStartedAt = System.currentTimeMillis()
    }

    fun autonomyDelayMs(): Long =
        random.nextLong(AUTONOMY_MIN_MS, AUTONOMY_MAX_MS + 1)

    fun syncSalads(count: Int) {
        saladCount = count.coerceAtLeast(0)
    }

    fun syncCosmetics(profile: PetProfile) {
        cosmetics = PetCosmetics.from(profile)
    }

    fun hatchEggIfNeeded(profile: PetProfile): Boolean {
        if (profile.petHatched || sessionHatched) return false
        onHatchEgg()
        sessionHatched = true
        currentAnim = PetAnim.BOUNCE
        sayEvent(SpeechEvent.TAP)
        return true
    }

    fun openInteraction(profile: PetProfile) {
        val hatchedNow = hatchEggIfNeeded(profile)
        interactionExpanded = true
        if (phase == PetPhase.Waiting || phase == PetPhase.PageReady) {
            phase = PetPhase.Active
            currentAnim = when {
                hatchedNow -> PetAnim.BOUNCE
                isEggProfile(profile, sessionHatched) -> PetAnim.SLEEP
                else -> PetAnim.WAVE
            }
            sayEvent(SpeechEvent.SHOW_UP)
        }
    }

    fun collapseInteraction() {
        interactionExpanded = false
        if (phase == PetPhase.Active) {
            phase = PetPhase.Waiting
            currentAnim = PetAnim.SLEEP
        }
    }

    private fun finishActiveSession(
        skipReadyGate: Boolean,
        loadFailed: Boolean = false,
        pageUrl: String = "",
    ) {
        isWalking = false
        walkOffset = 0f
        interactionExpanded = false
        if (!loadFailed) {
            val bonus = (sessionXp + 8).coerceAtMost(40)
            onAwardXp(bonus)
        }
        if (skipReadyGate || loadFailed) {
            phase = if (loadFailed) PetPhase.LoadFailed else PetPhase.Dismissed
            if (loadFailed) {
                currentAnim = PetAnim.CRY
                onLoadFailedSpeech()
            } else {
                clearSpeech()
            }
            pendingSpeechQueue.clear()
            speechPauseUntilMs = 0L
            return
        }
        phase = PetPhase.PageReady
        currentAnim = PetAnim.CELEBRATE
        onReadySpeech(pageUrl)
    }

    fun runAutonomy(eggMode: Boolean = false) {
        if (phase != PetPhase.Active || isWalking) return
        if (eggMode) {
            currentAnim = PetAnim.SLEEP
            sayEvent(SpeechEvent.AUTONOMY_SLEEP)
            return
        }
        val anim = pickAutonomyAnim()
        currentAnim = anim
        val event = when (anim) {
            PetAnim.DANCE, PetAnim.DANCE_SPIN, PetAnim.DANCE_SHUFFLE -> SpeechEvent.AUTONOMY_DANCE
            PetAnim.WALK, PetAnim.WALK_SLOW, PetAnim.RUN -> SpeechEvent.AUTONOMY_WALK
            PetAnim.SLEEP, PetAnim.SLEEP_DEEP -> SpeechEvent.AUTONOMY_SLEEP
            PetAnim.READ -> SpeechEvent.AUTONOMY_READ
            PetAnim.PLAY -> SpeechEvent.AUTONOMY_PLAY
            PetAnim.YAWN -> SpeechEvent.AUTONOMY_YAWN
            else -> SpeechEvent.IDLE
        }
        if (anim == PetAnim.WALK || anim == PetAnim.WALK_SLOW) {
            startWalk(anim)
        } else {
            sayEvent(event)
        }
    }

    fun startWalk(anim: PetAnim = PetAnim.WALK) {
        if (phase != PetPhase.Active) return
        isWalking = true
        currentAnim = anim
        facingLeft = random.nextBoolean()
        sayEvent(SpeechEvent.AUTONOMY_WALK)
    }

    fun advanceWalk(progress: Float) {
        walkOffset = (progress * 2f - 1f) * WALK_TRAVEL
    }

    fun endWalk() {
        isWalking = false
        walkOffset = 0f
        currentAnim = PetAnim.IDLE
    }

    fun advanceFrame() {
        frameTick++
    }

    fun dismissReady() {
        if (phase != PetPhase.PageReady && phase != PetPhase.LoadFailed) return
        phase = PetPhase.Dismissed
        clearSpeech()
        pendingSpeechQueue.clear()
        speechPauseUntilMs = 0L
        isWalking = false
        walkOffset = 0f
    }

    fun tick(profile: PetProfile) {
        if (phase != PetPhase.Active) return
        frameTick++
        val h = (stats.hunger - 0.9f).coerceAtLeast(12f)
        val att = (stats.attention - 0.45f).coerceAtLeast(10f)
        val hp = when {
            h < 18f -> (stats.health - 0.6f).coerceAtLeast(8f)
            else -> (stats.health - 0.1f).coerceAtLeast(8f)
        }
        stats = TamagotchiStats(hp, h, att)
        val prev = mood
        mood = when {
            hp < 18f -> PetMood.SICK
            h < 22f -> PetMood.HUNGRY
            att < 22f -> PetMood.SLEEPY
            else -> PetMood.HAPPY
        }
        if (mood == PetMood.HUNGRY && prev != PetMood.HUNGRY && speech == null) {
            sayEvent(SpeechEvent.HUNGRY)
        }
        if (mood == PetMood.SLEEPY && speech == null && frameTick % 5 == 0) {
            currentAnim = PetAnim.YAWN
        }
        updateIdleAnim(eggMode = isEggProfile(profile, sessionHatched))
    }

    fun tap(profile: PetProfile) {
        if (phase != PetPhase.Active || !interactionExpanded) return
        if (speech != null || System.currentTimeMillis() < speechPauseUntilMs || pendingSpeechQueue.isNotEmpty()) {
            petTapSpeechControl()
            if (speech != null) {
                pendingSpeechQueue.addLast(PetDialogue.forEvent(SpeechEvent.TAP, uiLanguage, random))
                return
            }
        }
        tapBurst++
        isWalking = false
        walkOffset = 0f
        stats = stats.copy(attention = (stats.attention + 18f).coerceAtMost(100f))
        mood = PetMood.EXCITED
        currentAnim = PetAnim.LOVE
        sessionXp += 2
        sayEvent(SpeechEvent.TAP)
    }

    fun feed(profile: PetProfile): Boolean {
        if (phase != PetPhase.Active) return false
        if (profile.salads <= 0 || !onSpendSalad()) {
            sayEvent(SpeechEvent.FEED_EMPTY)
            currentAnim = PetAnim.CRY
            return false
        }
        eatBurst++
        isWalking = false
        walkOffset = 0f
        stats = TamagotchiStats(
            health = (stats.health + 8f).coerceAtMost(100f),
            hunger = (stats.hunger + 28f).coerceAtMost(100f),
            attention = (stats.attention + 6f).coerceAtMost(100f),
        )
        mood = PetMood.HAPPY
        currentAnim = if (eatBurst % 2 == 0) PetAnim.EAT else PetAnim.CHEW
        sessionXp += 4
        sayEvent(SpeechEvent.FEED)
        return true
    }

    fun onLoadingSpeech(key: String) {
        if (phase != PetPhase.Waiting || loadingSpeechIssuedForKey == key) return
        loadingSpeechIssuedForKey = key
        enqueueSpeech(PetDialogueMix.random(DialogCategory.LOADING, uiLanguage, random))
    }

    fun onReadySpeech(pageUrl: String = "") {
        if (suppressReadySpeech) {
            suppressReadySpeech = false
            currentAnim = PetAnim.CELEBRATE
            return
        }
        currentAnim = PetAnim.CELEBRATE
        val siteLine = PetSiteReactions.lineForUrl(pageUrl, uiLanguage)
        val line = siteLine ?: PetDialogueMix.random(DialogCategory.SUCCESS, uiLanguage, random)
        enqueueSpeech(line)
    }

    fun onLoadFailedSpeech() {
        currentAnim = PetAnim.CRY
        enqueueSpeech(PetDialogueMix.random(DialogCategory.ERROR, uiLanguage, random))
    }

    fun clearSpeech() {
        speech = null
        speechVisibleChars = 0
        speechSkipRequested = false
    }

    private fun startSpeech(line: String) {
        speech = line
        speechVisibleChars = 0
        speechSkipRequested = false
    }

    fun enqueueIdleSpeech(lang: AppLanguage) {
        enqueueSpeech(PetDialogueMix.randomIdle(lang, random))
    }

    fun sayBrowserLine(line: String) {
        enqueueSpeech(line)
    }

    private fun enqueueSpeech(line: String) {
        if (speech != null || System.currentTimeMillis() < speechPauseUntilMs) {
            pendingSpeechQueue.addLast(line)
            return
        }
        startSpeech(line)
    }

    fun finishSpeech(line: String) {
        if (speech != line) return
        clearSpeech()
        speechPauseUntilMs = System.currentTimeMillis() + SPEECH_PAUSE_MS
    }

    fun tryStartPendingSpeech() {
        if (speech != null) return
        if (System.currentTimeMillis() < speechPauseUntilMs) return
        val next = pendingSpeechQueue.pollFirst() ?: return
        startSpeech(next)
    }

    /** Тап по питомцу: досрочно закончить реплику или пропустить паузу до следующей. */
    fun petTapSpeechControl() {
        if (speech != null) {
            speechSkipRequested = true
            return
        }
        speechPauseUntilMs = 0L
        tryStartPendingSpeech()
    }

    private fun resetSession() {
        loadingSpeechIssuedForKey = null
        stats = TamagotchiStats()
        mood = PetMood.HAPPY
        clearSpeech()
        pendingSpeechQueue.clear()
        speechPauseUntilMs = 0L
        sessionXp = 0
        frameTick = 0
        tapBurst = 0
        eatBurst = 0
        currentAnim = PetAnim.IDLE
        walkOffset = 0f
        facingLeft = false
        isWalking = false
        interactionExpanded = false
        sessionHatched = false
    }

    private fun sayEvent(event: SpeechEvent) {
        enqueueSpeech(PetDialogue.forEvent(event, uiLanguage, random))
    }

    private fun updateIdleAnim(eggMode: Boolean = false) {
        if (isWalking || eatBurst > 0 && frameTick < eatBurst * 4) return
        if (eggMode) {
            currentAnim = PetAnim.SLEEP
            return
        }
        currentAnim = when (mood) {
            PetMood.SICK -> PetAnim.SIT
            PetMood.SLEEPY -> if (frameTick % 8 < 4) PetAnim.SLEEP else PetAnim.YAWN
            PetMood.HUNGRY -> PetAnim.THINK
            PetMood.EXCITED -> PetAnim.BOUNCE
            else -> if (frameTick % 8 < 3) PetAnim.SLEEP else if (frameTick % 6 < 2) PetAnim.IDLE_BREATHE else PetAnim.IDLE
        }
    }

    private fun pickAutonomyAnim(): PetAnim {
        val toys = PetAnim.toyAutonomy(cosmetics)
        if (toys.isNotEmpty() && random.nextFloat() < 0.38f) {
            return toys.random(random)
        }
        if (random.nextFloat() < SLEEP_AUTONOMY_CHANCE) {
            return when (random.nextInt(4)) {
                0 -> PetAnim.SLEEP_DEEP
                1 -> PetAnim.SLEEP
                2 -> PetAnim.YAWN
                else -> PetAnim.SIT
            }
        }
        return PetAnim.autonomyPool.random(random)
    }
}

/** Корутины сессии питомца: один sync/таймер, отмена при уходе с экрана. */
@Composable
private fun TamagotchiSessionEffects(
    controller: TamagotchiController,
    readerBusy: Boolean,
    enabled: Boolean,
    loadKey: String,
    pageUrl: String,
    skipReadyGate: Boolean,
    loadFailed: Boolean,
    profile: PetProfile,
    uiLanguage: AppLanguage,
    onSessionChange: (String, PetPhase) -> Unit,
) {
    LaunchedEffect(controller.phase, loadKey) {
        onSessionChange(loadKey, controller.phase)
    }

    LaunchedEffect(readerBusy, enabled, loadKey, pageUrl, skipReadyGate, loadFailed) {
        controller.sync(readerBusy, enabled, loadKey, skipReadyGate, loadFailed, pageUrl)
        if (!readerBusy || !enabled) return@LaunchedEffect
        if (controller.phase == PetPhase.Waiting) {
            controller.onLoadingSpeech(loadKey)
            controller.currentAnim = PetAnim.SLEEP
        }
    }

    LaunchedEffect(controller.phase) {
        when (controller.phase) {
            PetPhase.PageReady, PetPhase.LoadFailed -> {
                delay(READY_CHIP_MS)
                if (controller.phase == PetPhase.PageReady || controller.phase == PetPhase.LoadFailed) {
                    controller.dismissReady()
                }
            }
            else -> Unit
        }
    }

    LaunchedEffect(controller.phase) {
        while (isActive &&
            (controller.phase == PetPhase.Waiting || controller.phase == PetPhase.Active)
        ) {
            delay(FRAME_MS)
            controller.advanceFrame()
        }
    }

    LaunchedEffect(controller.phase, profile.petHatched, profile.saladsEatenBytes) {
        while (isActive && controller.phase == PetPhase.Active) {
            delay(TICK_MS)
            if (controller.phase != PetPhase.Active) break
            controller.tick(profile)
        }
    }

    LaunchedEffect(controller.phase) {
        while (isActive && controller.phase == PetPhase.Active) {
            delay(controller.autonomyDelayMs())
            if (controller.phase != PetPhase.Active) break
            controller.runAutonomy(
                eggMode = isEggProfile(profile, controller.sessionHatched),
            )
        }
    }

    LaunchedEffect(controller.isWalking, controller.phase) {
        if (!controller.isWalking || controller.phase != PetPhase.Active) return@LaunchedEffect
        val steps = 28
        for (i in 0..steps) {
            if (!controller.isWalking || controller.phase != PetPhase.Active) break
            controller.advanceWalk(i.toFloat() / steps)
            delay(WALK_STEP_MS)
        }
        if (controller.isWalking) controller.endWalk()
    }

    LaunchedEffect(controller.phase) {
        while (isActive && controller.phase == PetPhase.Active) {
            delay(IDLE_SPEECH_INTERVAL_MS)
            if (controller.phase != PetPhase.Active || controller.speech != null || controller.isWalking) continue
            controller.enqueueIdleSpeech(uiLanguage)
        }
    }

    LaunchedEffect(controller.speech, controller.speechSkipRequested) {
        val line = controller.speech ?: return@LaunchedEffect
        val charDelay = speechCharDelayMs(line.length)
        for (i in line.indices) {
            if (controller.speech != line) return@LaunchedEffect
            if (controller.speechSkipRequested) {
                controller.speechVisibleChars = line.length
                break
            }
            delay(charDelay)
            controller.speechVisibleChars = i + 1
        }
        controller.speechVisibleChars = line.length
        if (!controller.speechSkipRequested) {
            delay(speechReadingMs(line.length))
        }
        if (controller.speech == line) {
            controller.finishSpeech(line)
        }
    }

    LaunchedEffect(controller.speech, controller.speechPauseUntilMs) {
        if (controller.speech != null) return@LaunchedEffect
        val wait = (controller.speechPauseUntilMs - System.currentTimeMillis()).coerceAtLeast(0L)
        if (wait > 0L) delay(wait)
        controller.tryStartPendingSpeech()
    }
}

@Composable
fun rememberTamagotchiController(
    onAwardXp: (Int) -> Unit,
    onSpendSalad: () -> Boolean,
    onHatchEgg: () -> Unit,
    restorePhase: PetPhase? = null,
): TamagotchiController =
    remember(onAwardXp, onSpendSalad, onHatchEgg) {
        TamagotchiController(onAwardXp, onSpendSalad, onHatchEgg).apply {
            restorePhase?.let { phase = it }
        }
    }

/** Читалка: FAB у поиска, по тапу — карточка; после загрузки — «Готово». */
@Composable
fun TamagotchiReaderLayer(
    controller: TamagotchiController,
    loading: Boolean,
    enabled: Boolean,
    loadKey: String,
    url: String,
    profile: PetProfile,
    modifier: Modifier = Modifier,
    overlayGate: Boolean = false,
    readerBusy: Boolean = loading,
    skipReadyGate: Boolean = false,
    loadFailed: Boolean = false,
    onHatchEgg: () -> Unit = {},
    onSessionChange: (String, PetPhase) -> Unit = { _, _ -> },
    onSkipReadyGateChange: (Boolean) -> Unit = {},
    suppressReadySpeech: Boolean = false,
    uiLanguage: AppLanguage = AppLanguage.RU,
) {
    val view = LocalView.current
    val shown = displayProfile(profile, controller.sessionHatched)
    val eggMode = isEggProfile(shown, controller.sessionHatched)

    LaunchedEffect(uiLanguage) {
        controller.uiLanguage = uiLanguage
    }

    LaunchedEffect(suppressReadySpeech) {
        if (suppressReadySpeech) controller.suppressReadySpeech = true
    }

    LaunchedEffect(
        profile.salads,
        profile.petHatched,
        profile.stage,
        profile.growthLevel,
        profile.equippedHatId,
        profile.ballEquipped,
        profile.chairEquipped,
    ) {
        controller.syncSalads(profile.salads)
        controller.syncCosmetics(profile)
        if (profile.petHatched) controller.sessionHatched = true
    }

    TamagotchiSessionEffects(
        controller = controller,
        readerBusy = readerBusy,
        enabled = enabled,
        loadKey = loadKey,
        pageUrl = url,
        skipReadyGate = skipReadyGate,
        loadFailed = loadFailed,
        profile = profile,
        uiLanguage = uiLanguage,
        onSessionChange = onSessionChange,
    )

    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = controller.showPetCard,
            enter = slideInVertically(tween(280)) { it } + fadeIn(tween(240)),
            exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(160)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = FAB_BOTTOM_PAD_DP.dp + 8.dp)
                .padding(horizontal = 8.dp)
                .widthIn(max = 420.dp),
        ) {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    FilledTonalButton(
                        onClick = { controller.collapseInteraction() },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(SaylatStrings.collapse(uiLanguage))
                    }
                }
                PixelPetCard(
                    controller = controller,
                    url = url,
                    profile = shown,
                    eggMode = eggMode,
                    uiLanguage = uiLanguage,
                )
            }
        }

        AnimatedVisibility(
            visible = controller.showFloatingPet && !controller.showReadyChip && !controller.showFailedChip,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 12.dp, bottom = FAB_BOTTOM_PAD_DP.dp),
        ) {
            PetLoadingFab(
                controller = controller,
                profile = shown,
                eggMode = eggMode,
                frame = controller.frameTick,
                loading = readerBusy,
                uiLanguage = uiLanguage,
                onClick = {
                    if (controller.speech != null ||
                        controller.hasPendingSpeech() ||
                        System.currentTimeMillis() < controller.speechPauseUntilMs
                    ) {
                        controller.petTapSpeechControl()
                    }
                    controller.openInteraction(profile)
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                },
            )
        }

        AnimatedVisibility(
            visible = controller.showReadyChip,
            enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 2 },
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 12.dp, bottom = FAB_BOTTOM_PAD_DP.dp),
        ) {
            PetReadyChip(
                profile = shown,
                sessionXp = controller.sessionXp,
                uiLanguage = uiLanguage,
                onDismiss = controller::dismissReady,
            )
        }

        AnimatedVisibility(
            visible = controller.showFailedChip,
            enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 2 },
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 12.dp, bottom = FAB_BOTTOM_PAD_DP.dp),
        ) {
            PetFailedChip(
                profile = shown,
                uiLanguage = uiLanguage,
                onDismiss = controller::dismissReady,
            )
        }
    }
}

@Composable
private fun PetLoadingFab(
    controller: TamagotchiController,
    profile: PetProfile,
    eggMode: Boolean,
    frame: Int,
    loading: Boolean,
    uiLanguage: AppLanguage,
    onClick: () -> Unit,
) {
    val bounce by rememberInfiniteTransition(label = "fabBounce").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "fabOffset",
    )
    val anim = if (eggMode) {
        PetAnim.SLEEP
    } else {
        when ((frame / 12) % 3) {
            0 -> PetAnim.IDLE_BREATHE
            1 -> PetAnim.WALK_SLOW
            else -> PetAnim.SLEEP
        }
    }
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        controller.speech?.let { line ->
            PetSpeechBubble(
                fullText = line,
                visibleChars = controller.speechVisibleChars,
                showCursor = controller.speechVisibleChars < line.length,
                modifier = Modifier.widthIn(max = 220.dp),
            )
        }
        if (loading) {
            Text(
                SaylatStrings.tapToOpenPet(uiLanguage),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }
        Box(
            modifier = Modifier
                .semantics { contentDescription = SaylatStrings.petFabContentDescription(uiLanguage) }
                .offset(y = (-4 * bounce).dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.runtime.key(profile.stage, profile.growthLevel, eggMode) {
                PixelPetPreview(
                    stage = profile.stage,
                    mood = PetMood.HAPPY,
                    anim = anim,
                    frame = frame,
                    cosmetics = PetCosmetics.from(profile),
                    phase = if (loading) PetPhase.Waiting else null,
                    frameVariant = HedgehogFrameVariant.Fab,
                    modifier = Modifier.size(92.dp),
                )
            }
        }
    }
}

@Composable
private fun PetFailedChip(
    profile: PetProfile,
    uiLanguage: AppLanguage,
    onDismiss: () -> Unit,
) {
    Surface(
        onClick = onDismiss,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shadowElevation = 4.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PixelPetPreview(
                stage = profile.stage,
                mood = PetMood.SICK,
                anim = PetAnim.CRY,
                frame = 1,
                cosmetics = PetCosmetics.from(profile),
                loadFailed = true,
                frameVariant = HedgehogFrameVariant.Chip,
                modifier = Modifier.size(44.dp),
            )
            Column {
                Text(
                    SaylatStrings.pageLoadFailedTitle(uiLanguage),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    SaylatStrings.pageLoadFailedSubtitle(uiLanguage),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun PetReadyChip(
    profile: PetProfile,
    sessionXp: Int,
    uiLanguage: AppLanguage,
    onDismiss: () -> Unit,
) {
    Surface(
        onClick = onDismiss,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 4.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PixelPetPreview(
                stage = profile.stage,
                mood = PetMood.HAPPY,
                anim = PetAnim.CELEBRATE,
                frame = 1,
                cosmetics = PetCosmetics.from(profile),
                phase = PetPhase.PageReady,
                frameVariant = HedgehogFrameVariant.Chip,
                modifier = Modifier.size(44.dp),
            )
            Column {
                Text(
                    SaylatStrings.pageReadyTitle(uiLanguage),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    SaylatStrings.pageReadySubtitle(uiLanguage),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                )
                if (sessionXp > 0) {
                    Text(
                        "+$sessionXp XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

// ── Header slot (home top bar) ───────────────────────────────────────────────

/** В шапке (не яйцо): медленная смена «танцует / читает» — без сна. */
private val HEADER_IDLE_ANIMS = listOf(
    PetAnim.DANCE_SHUFFLE,
    PetAnim.READ,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderPetSlot(
    tamagotchiEnabled: Boolean,
    profile: PetProfile,
    uiLanguage: AppLanguage = AppLanguage.RU,
    modifier: Modifier = Modifier,
) {
    if (!tamagotchiEnabled) {
        SaylatBrandMark(
            modifier = modifier,
            iconSize = 28.dp,
            showWordmark = false,
        )
        return
    }

    val isEgg = profile.isEgg
    var animIndex by remember { mutableIntStateOf(0) }
    var frame by remember { mutableIntStateOf(0) }
    var showStats by remember { mutableStateOf(false) }
    val headerAnim = if (isEgg) PetAnim.SLEEP else HEADER_IDLE_ANIMS[animIndex]
    val cosmetics = remember(
        profile.stage,
        profile.growthLevel,
        profile.equippedHatId,
        profile.ballEquipped,
        profile.chairEquipped,
    ) {
        PetCosmetics.from(profile)
    }
    val view = LocalView.current

    LaunchedEffect(profile.stage, profile.growthLevel, isEgg) {
        frame = 0
        animIndex = 0
    }

    LaunchedEffect(tamagotchiEnabled, isEgg) {
        if (!tamagotchiEnabled || isEgg) return@LaunchedEffect
        var cyclesAtAnim = 0
        while (isActive) {
            delay(HEADER_FRAME_MS)
            val current = HEADER_IDLE_ANIMS[animIndex]
            frame++
            if (frame >= current.frames) {
                frame = 0
                cyclesAtAnim++
                if (cyclesAtAnim >= HEADER_ANIM_HOLD_CYCLES) {
                    cyclesAtAnim = 0
                    animIndex = (animIndex + 1) % HEADER_IDLE_ANIMS.size
                }
            }
        }
    }

    LaunchedEffect(tamagotchiEnabled, isEgg) {
        if (!tamagotchiEnabled || !isEgg) return@LaunchedEffect
        while (isActive) {
            delay(HEADER_FRAME_MS)
            frame = (frame + 1) % PetAnim.SLEEP.frames
        }
    }

    if (showStats) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showStats = false },
            sheetState = sheetState,
        ) {
            PetProfileStatsBody(
                profile = profile,
                uiLanguage = uiLanguage,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.runtime.key(profile.stage, profile.growthLevel, cosmetics) {
                PixelPetPreview(
                    stage = profile.stage,
                    mood = PetMood.HAPPY,
                    anim = headerAnim,
                    frame = frame,
                    cosmetics = cosmetics,
                    frameVariant = HedgehogFrameVariant.Mini,
                    modifier = Modifier
                        .size(36.dp)
                        .pointerInput(profile.stage, profile.growthLevel) {
                            detectTapGestures {
                                showStats = true
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            }
                        },
                )
            }
        }
        Column(
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures { showStats = true }
            },
        ) {
            Text(
                profile.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                when {
                    isEgg -> SaylatStrings.headerEggHint(uiLanguage)
                    headerAnim == PetAnim.DANCE_SHUFFLE -> SaylatStrings.headerDance(uiLanguage)
                    headerAnim == PetAnim.READ -> SaylatStrings.headerRead(uiLanguage)
                    else -> profile.stageTitle
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
fun PetProfileStatsBody(
    profile: PetProfile,
    uiLanguage: AppLanguage = AppLanguage.RU,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            profile.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            profile.stageTitle,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        if (profile.isEgg) {
            Text(
                SaylatStrings.petEggStatsHint(uiLanguage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            )
        }
        Text(
            "Рост: ${PetGrowth.formatEatenMb(profile.saladsEatenBytes)} / 100 МБ",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (profile.growthLevel < PetGrowth.LEVEL_COUNT) {
            Text(
                "До ${PetGrowth.titleForLevel(profile.growthLevel + 1)}: ${PetGrowth.formatEatenMb(PetGrowth.bytesUntilNextLevel(profile.saladsEatenBytes))}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
        }
        Text(
            "Кошелёк ${PetWallet.formatWallet(profile)} · салатики ${profile.salads}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (profile.streak > 1) {
            Text("Серия: ${profile.streak} дн.", style = MaterialTheme.typography.labelMedium)
        }
        Text(
            "Saylat ${PET_HEDGEHOG_GRID}×${PET_HEDGEHOG_GRID} px · пиксельный ёжик",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        )
    }
}

// ── Pet card ──────────────────────────────────────────────────────────────────

@Composable
private fun PixelPetCard(
    controller: TamagotchiController,
    url: String,
    profile: PetProfile,
    eggMode: Boolean = false,
    uiLanguage: AppLanguage = AppLanguage.RU,
) {
    val view = LocalView.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(
            2.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
        ),
    ) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    profile.name,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    profile.stageTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                )
            }
            if (profile.growthLevel < PetGrowth.LEVEL_COUNT) {
                Text(
                    SaylatStrings.growthLine(
                        uiLanguage,
                        PetGrowth.formatEatenMb(profile.saladsEatenBytes),
                        PetGrowth.titleForLevel(profile.growthLevel + 1),
                        PetGrowth.formatEatenMb(PetGrowth.bytesUntilNextLevel(profile.saladsEatenBytes)),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    SaylatStrings.growthMax(uiLanguage, PetGrowth.formatEatenMb(profile.saladsEatenBytes)),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PixelStatBar(SaylatStrings.statHp(uiLanguage), controller.stats.health, PixelColors.hp, Modifier.weight(1f))
                PixelStatBar(SaylatStrings.statFood(uiLanguage), controller.stats.hunger, PixelColors.hunger, Modifier.weight(1f))
                PixelStatBar(SaylatStrings.statJoy(uiLanguage), controller.stats.attention, PixelColors.joy, Modifier.weight(1f))
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                val density = LocalDensity.current
                val walkPx = with(density) { (controller.walkOffset * 120.dp.toPx()).toInt() }
                androidx.compose.runtime.key(
                    profile.stage,
                    profile.growthLevel,
                    controller.cosmetics,
                ) {
                PixelPetPreview(
                    stage = profile.stage,
                    mood = controller.mood,
                    anim = if (eggMode) PetAnim.SLEEP else controller.currentAnim,
                    frame = controller.frameTick + controller.tapBurst + controller.eatBurst * 2,
                    flipX = controller.facingLeft,
                    cosmetics = controller.cosmetics,
                    phase = controller.phase,
                    frameVariant = HedgehogFrameVariant.Stage,
                    modifier = Modifier
                        .size(156.dp)
                        .offset { IntOffset(walkPx, 0) }
                        .pointerInput(Unit) {
                            detectTapGestures {
                                controller.tap(profile)
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            }
                        },
                )
                }
                controller.speech?.let { line ->
                    val typing = controller.speechVisibleChars < line.length
                    PetSpeechBubble(
                        fullText = line,
                        visibleChars = controller.speechVisibleChars,
                        showCursor = typing,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp),
                    )
                }
            }
            Text(
                SaylatStrings.moodLabel(controller.mood, uiLanguage),
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = {
                        if (controller.feed(profile)) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        }
                    },
                    enabled = profile.salads > 0,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(SaylatStrings.petSaladButton(uiLanguage, profile.salads), fontFamily = FontFamily.Monospace)
                }
                FilledTonalButton(
                    onClick = {
                        controller.tap(profile)
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(SaylatStrings.petPetButton(uiLanguage), fontFamily = FontFamily.Monospace)
                }
            }
            Text(
                if (profile.salads > 0) {
                    SaylatStrings.petSaladProgress(
                        uiLanguage,
                        PetSaladEconomy.formatKb(profile.bytesUntilNextSalad),
                    )
                } else {
                    SaylatStrings.petSaladHint(
                        uiLanguage,
                        PetSaladEconomy.formatKb(profile.bytesUntilNextSalad),
                    )
                },
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PulsingPixelDot()
                Text(
                    url.removePrefix("https://").removePrefix("http://").take(42)
                        .ifBlank { SaylatStrings.loadingUrl(uiLanguage) },
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun TamagotchiReadyOverlay(
    profile: PetProfile,
    sessionXp: Int,
    onOpen: () -> Unit,
    skipReadyGate: Boolean = false,
    onSkipReadyGateChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
    ) {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            androidx.compose.runtime.key(profile.stage, profile.growthLevel) {
                PixelPetPreview(
                    stage = profile.stage,
                    mood = PetMood.HAPPY,
                    anim = PetAnim.CELEBRATE,
                    frame = 2,
                    cosmetics = PetCosmetics.from(profile),
                    phase = PetPhase.PageReady,
                    frameVariant = HedgehogFrameVariant.Chip,
                    modifier = Modifier.size(48.dp),
                )
            }
            Text(
                "Страница готова!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                "${profile.name} дождался. +$sessionXp XP за уход.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Checkbox(
                    checked = skipReadyGate,
                    onCheckedChange = onSkipReadyGateChange,
                )
                Text(
                    "Больше не показывать",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Button(
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Читать", fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// ── Пиксельный ёжик Saylat (48×48) ───────────────────────────────────────────

private object PixelColors {
    val hp = Color(0xFFE85D5D)
    val hunger = Color(0xFFF5A623)
    val joy = Color(0xFF4ECDC4)
    val outline = Color(0xFF2D3436)
    val body = HedgehogPetColors.spine
}

@Composable
fun PixelPetPreview(
    stage: Int,
    mood: PetMood,
    anim: PetAnim,
    frame: Int,
    flipX: Boolean = false,
    cosmetics: PetCosmetics = PetCosmetics(),
    phase: PetPhase? = null,
    loadFailed: Boolean = false,
    frameVariant: HedgehogFrameVariant = HedgehogFrameVariant.Stage,
    modifier: Modifier = Modifier,
) {
    val eggMode = stage <= 0
    val loadingGlow = phase == PetPhase.Waiting
    HedgehogPetFrame(
        modifier = modifier,
        variant = frameVariant,
        loadingGlow = loadingGlow,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            with(PetHedgehogEngine) {
                draw(
                    mood = mood,
                    anim = anim,
                    frame = frame,
                    eggMode = eggMode,
                    stage = stage,
                    cosmetics = cosmetics,
                    flipX = flipX,
                    phase = phase,
                    loadFailed = loadFailed,
                )
            }
        }
    }
}

@Composable
private fun PixelStatBar(label: String, value: Float, fill: Color, modifier: Modifier = Modifier) {
    val ratio by animateFloatAsState((value / 100f).coerceIn(0f, 1f), tween(400), label = "stat")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, PixelColors.outline.copy(alpha = 0.3f), RoundedCornerShape(2.dp)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(ratio)
                    .height(8.dp)
                    .background(fill),
            )
        }
    }
}

@Composable
private fun PulsingPixelDot() {
    val t = rememberInfiniteTransition(label = "pd")
    val a by t.animateFloat(
        0.3f, 1f,
        infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "pa",
    )
    Box(
        Modifier
            .size(6.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(PixelColors.body.copy(alpha = a)),
    )
}


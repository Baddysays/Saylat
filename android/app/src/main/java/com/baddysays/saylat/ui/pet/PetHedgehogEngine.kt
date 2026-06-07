package com.baddysays.saylat.ui.pet

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.baddysays.saylat.tamagotchi.PetEmotion
import com.baddysays.saylat.tamagotchi.PetRenderer
import com.baddysays.saylat.tamagotchi.shop.PetItemRenderer
import com.baddysays.saylat.ui.PetMood
import com.baddysays.saylat.ui.PetPhase
import kotlin.math.floor

const val PET_HEDGEHOG_GRID = 48

/** Пиксельный ёжик Saylat 48×48. */
object PetHedgehogEngine {

    fun DrawScope.draw(
        mood: PetMood,
        anim: PetAnim,
        frame: Int,
        eggMode: Boolean,
        stage: Int = 0,
        cosmetics: PetCosmetics = PetCosmetics(),
        flipX: Boolean = false,
        phase: PetPhase? = null,
        loadFailed: Boolean = false,
    ) {
        val pixel = floor(size.minDimension / PET_HEDGEHOG_GRID).coerceAtLeast(1f)
        val drawSize = pixel * PET_HEDGEHOG_GRID
        val ox = (size.width - drawSize) / 2f
        val oy = (size.height - drawSize) / 2f
        val pivot = Offset(size.width / 2f, size.height / 2f)
        val growthScale = if (eggMode) {
            1f
        } else {
            0.9f + stage.coerceIn(0, 5) * 0.02f
        }

        fun drawHedgehog(ps: Float) {
            if (eggMode) {
                scale(0.52f, 0.52f, pivot = Offset(drawSize / 2f, drawSize * 0.58f)) {
                    with(PetRenderer) {
                        drawPet(PetEmotion.SLEEPING, frame, ps)
                    }
                }
                drawEggShell(ps)
                return
            }
            val base = PetEmotionMapper.resolve(mood, anim, eggMode = false, phase, loadFailed)
            val emotion = PetEmotionMapper.applyShopHat(cosmetics.hatId, base)
            scale(growthScale, growthScale, pivot = Offset(drawSize / 2f, drawSize * 0.62f)) {
                with(PetRenderer) {
                    drawPet(emotion, frame, ps)
                }
                val overlay = PetEmotionMapper.shopAccessoryOverlay(cosmetics.hatId)
                if (overlay != null) {
                    with(PetItemRenderer) {
                        drawAccessory(overlay, frame, ps)
                    }
                }
            }
        }

        translate(ox, oy) {
            if (flipX) {
                scale(scaleX = -1f, scaleY = 1f, pivot = Offset(drawSize / 2f, drawSize / 2f)) {
                    drawHedgehog(pixel)
                }
            } else {
                drawHedgehog(pixel)
            }
        }
    }

    fun DrawScope.drawShopPreview(itemId: String, frame: Int) {
        val pixel = floor(size.minDimension / PET_HEDGEHOG_GRID).coerceAtLeast(1f)
        val drawSize = pixel * PET_HEDGEHOG_GRID
        val ox = (size.width - drawSize) / 2f
        val oy = (size.height - drawSize) / 2f
        translate(ox, oy) {
            val emotion = PetEmotionMapper.shopPreviewEmotion(itemId)
            with(PetRenderer) {
                drawPet(emotion, frame, pixel)
            }
            val overlay = PetEmotionMapper.shopAccessoryOverlay(itemId)
            if (overlay != null) {
                with(PetItemRenderer) {
                    drawAccessory(overlay, frame, pixel)
                }
            }
        }
    }

    private fun DrawScope.drawEggShell(ps: Float) {
        val shell = Color(0xFFFFF9E6)
        val spot = Color(0xFFFFEAA7)
        val outline = Color(0xFF8B7355)
        val speckle = Color(0xFFE8C878)
        fun ell(cx: Float, cy: Float, rx: Float, ry: Float, color: Color) {
            val x0 = floor(cx - rx).toInt()
            val x1 = floor(cx + rx).toInt()
            val y0 = floor(cy - ry).toInt()
            val y1 = floor(cy + ry).toInt()
            for (y in y0..y1) {
                for (x in x0..x1) {
                    val dx = (x - cx) / rx
                    val dy = (y - cy) / ry
                    if (dx * dx + dy * dy <= 1f) {
                        drawRect(
                            color = color,
                            topLeft = Offset(x * ps, y * ps),
                            size = androidx.compose.ui.geometry.Size(ps, ps),
                        )
                    }
                }
            }
        }
        ell(24f, 30f, 14f, 16f, shell)
        ell(24f, 31f, 12f, 14f, spot.copy(alpha = 0.4f))
        listOf(18 to 26, 28 to 24, 22 to 34, 30 to 32).forEach { (x, y) ->
            drawRect(speckle.copy(alpha = 0.45f), Offset(x * ps, y * ps), androidx.compose.ui.geometry.Size(ps, ps))
        }
        ell(24f, 30f, 14f, 16f, outline.copy(alpha = 0.18f))
    }
}

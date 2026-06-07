package com.baddysays.saylat.tamagotchi.shop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Рендерит аксессуары поверх ёжика.
 * Вызывать после PetRenderer.drawPet() в том же Canvas:
 *
 *   Canvas(modifier = Modifier.size(240.dp)) {
 *       with(PetRenderer) { drawPet(emotion, frame, pixelSize) }
 *       with(PetItemRenderer) { drawAccessory(accessoryType, frame, pixelSize) }
 *   }
 */
object PetItemRenderer {

    private fun DrawScope.px(x: Int, y: Int, col: Color, ps: Float) {
        if (x < 0 || x >= 48 || y < 0 || y >= 48) return
        drawRect(col, topLeft = Offset(x * ps, y * ps),
            size = androidx.compose.ui.geometry.Size(ps, ps))
    }
    private fun DrawScope.rect(x: Int, y: Int, w: Int, h: Int, col: Color, ps: Float) {
        drawRect(col, topLeft = Offset(x * ps, y * ps),
            size = androidx.compose.ui.geometry.Size(w * ps, h * ps))
    }
    private fun DrawScope.ell(cx: Float, cy: Float, rx: Float, ry: Float, col: Color, ps: Float) {
        val x0 = (cx - rx).toInt(); val x1 = (cx + rx).toInt()
        val y0 = (cy - ry).toInt(); val y1 = (cy + ry).toInt()
        for (y in y0..y1) for (x in x0..x1) {
            if ((x-cx)*(x-cx)/(rx*rx) + (y-cy)*(y-cy)/(ry*ry) <= 1f) px(x, y, col, ps)
        }
    }

    fun DrawScope.drawAccessory(
        type: AccessoryOverlayType?,
        frame: Int,
        ps: Float,
    ) {
        type ?: return
        when (type) {
            AccessoryOverlayType.CHEF_HAT     -> drawChefHat(ps)
            AccessoryOverlayType.HEADPHONES   -> drawHeadphones(ps)
            AccessoryOverlayType.PILOT_GOGGLES -> drawPilotGoggles(ps)
            AccessoryOverlayType.BOWTIE       -> drawBowtie(ps)
            AccessoryOverlayType.SCARF        -> drawScarf(ps)
            AccessoryOverlayType.CAPE         -> drawCape(frame, ps)
            AccessoryOverlayType.WIZARD_HAT   -> drawWizardHat(frame, ps)
            AccessoryOverlayType.COWBOY_HAT   -> drawCowboyHat(ps)
        }
    }

    // ─────────────────────────────────────────────────────
    // CHEF HAT — белая высокая шляпа над головой
    // ─────────────────────────────────────────────────────
    private fun DrawScope.drawChefHat(ps: Float) {
        // Тулья (высокая белая часть)
        ell(23f, 10f, 7f, 8f, Color(0xFFFFFFFF), ps)
        ell(23f, 10f, 6f, 7f, Color(0xFFF5F5F5), ps)
        // Поля
        rect(14, 15, 18, 2, Color(0xFFEEEEEE), ps)
        rect(13, 14, 20, 2, Color(0xFFDDDDDD), ps)
        // Декоративная полоса
        rect(16, 13, 14, 1, Color(0xFFCCCCCC), ps)
        // Блик
        px(19, 7, Color(0xFFFFFFFF), ps)
        px(20, 6, Color(0xFFF0F0F0), ps)
    }

    // ─────────────────────────────────────────────────────
    // HEADPHONES — тёмные наушники с синими чашками
    // ─────────────────────────────────────────────────────
    private fun DrawScope.drawHeadphones(ps: Float) {
        // Дуга над головой
        val dark = Color(0xFF222222)
        for (x in 10..36) px(x, 13, dark, ps)
        px(10, 14, dark, ps); px(10, 15, dark, ps)
        px(36, 14, dark, ps); px(36, 15, dark, ps)
        // Чашки
        ell(10f, 18f, 4f, 5f, dark, ps)
        ell(36f, 18f, 4f, 5f, dark, ps)
        ell(10f, 18f, 3f, 4f, Color(0xFF3366FF), ps)
        ell(36f, 18f, 3f, 4f, Color(0xFF3366FF), ps)
        // Блики
        px(9, 16, Color(0xFF88AAFF), ps)
        px(35, 16, Color(0xFF88AAFF), ps)
    }

    // ─────────────────────────────────────────────────────
    // PILOT GOGGLES — коричневые очки с прозрачными стёклами
    // ─────────────────────────────────────────────────────
    private fun DrawScope.drawPilotGoggles(ps: Float) {
        val leather = Color(0xFF8B5E3C)
        val glass   = Color(0xAADDEEFF)
        val dark    = Color(0xFF5A3A18)
        // Ремень
        rect(3, 21, 8, 2, leather, ps)
        rect(35, 21, 8, 2, leather, ps)
        // Левая линза
        ell(14f, 22f, 5f, 5f, leather, ps)
        ell(14f, 22f, 4f, 4f, glass, ps)
        ell(14f, 22f, 2f, 2f, Color(0x55FFFFFF), ps)
        // Правая линза
        ell(32f, 22f, 5f, 5f, leather, ps)
        ell(32f, 22f, 4f, 4f, glass, ps)
        ell(32f, 22f, 2f, 2f, Color(0x55FFFFFF), ps)
        // Перемычка
        rect(19, 21, 8, 2, dark, ps)
        // Блики
        px(12, 20, Color(0xFFFFFFFF), ps)
        px(30, 20, Color(0xFFFFFFFF), ps)
    }

    // ─────────────────────────────────────────────────────
    // BOWTIE — красная бабочка на шее
    // ─────────────────────────────────────────────────────
    private fun DrawScope.drawBowtie(ps: Float) {
        val red   = Color(0xFFCC2222)
        val dark  = Color(0xFF991111)
        val light = Color(0xFFFF4444)
        // Левое крыло
        rect(12, 35, 8, 6, red, ps)
        px(12, 35, dark, ps); px(12, 40, dark, ps)
        px(19, 35, dark, ps); px(19, 40, dark, ps)
        // Правое крыло
        rect(22, 35, 8, 6, red, ps)
        px(22, 35, dark, ps); px(22, 40, dark, ps)
        px(29, 35, dark, ps); px(29, 40, dark, ps)
        // Узел посередине
        rect(20, 36, 3, 4, dark, ps)
        rect(20, 37, 3, 2, light, ps)
    }

    // ─────────────────────────────────────────────────────
    // SCARF — полосатый шарф на шее с хвостом вбок
    // ─────────────────────────────────────────────────────
    private fun DrawScope.drawScarf(ps: Float) {
        val red   = Color(0xFFCC2222)
        val white = Color(0xFFFFFFFF)
        val blue  = Color(0xFF2244CC)
        val cols  = listOf(red, white, blue)
        // Обмотка
        for (i in 0..2) rect(10, 33 + i, 24, 1, cols[i % 3], ps)
        for (i in 0..2) rect(10, 36 + i, 24, 1, cols[(i+1) % 3], ps)
        // Хвост свисает справа
        for (i in 0..8) rect(32, 35 + i, 5, 1, cols[i % 3], ps)
        // Кончик
        rect(33, 43, 3, 1, red, ps)
    }

    // ─────────────────────────────────────────────────────
    // CAPE — красный плащ сзади с развевающимися краями
    // ─────────────────────────────────────────────────────
    private fun DrawScope.drawCape(frame: Int, ps: Float) {
        val red   = Color(0xFFCC0000)
        val dark  = Color(0xFF880000)
        val gold  = Color(0xFFFFDD00)
        val wave  = if (frame % 40 < 20) 1 else -1
        // Застёжка-воротник
        rect(17, 18, 12, 2, gold, ps)
        // Плащ (рисуется "позади" ёжика, но в canvas сверху)
        rect(8, 20, 3, 20, red, ps)
        rect(35, 20, 3, 20, red, ps)
        rect(8, 20, 30, 1, dark, ps)
        // Развевающийся низ
        for (i in 0..5) {
            val xOff = if (i % 2 == 0) wave else -wave
            rect(8 + xOff, 38 + i, 30, 1, if (i % 2 == 0) red else dark, ps)
        }
        // Подкладка (золотая кромка)
        rect(8, 20, 1, 18, gold, ps)
        rect(37, 20, 1, 18, gold, ps)
    }

    // ─────────────────────────────────────────────────────
    // WIZARD HAT — фиолетовая остроконечная шляпа со звёздами
    // ─────────────────────────────────────────────────────
    private fun DrawScope.drawWizardHat(frame: Int, ps: Float) {
        val purple = Color(0xFF6622CC)
        val dark   = Color(0xFF3300AA)
        val gold   = Color(0xFFFFD700)
        val t      = frame * 0.06f
        // Конус шляпы
        for (i in 0..11) {
            val w = i * 2 + 2
            rect(23 - i, 10 - i, w, 1, if (i % 2 == 0) purple else dark, ps)
        }
        // Поля
        rect(9, 21, 28, 2, dark, ps)
        rect(8, 22, 30, 2, purple, ps)
        // Пряжка-звезда
        rect(20, 22, 6, 1, gold, ps)
        // Звёзды на конусе
        val starPos = listOf(Triple(18, 14, 0f), Triple(25, 12, 1.5f), Triple(21, 17, 3f))
        starPos.forEach { (x, y, phase) ->
            val pulse = if (kotlin.math.sin(t + phase) > 0) gold else Color(0xFFFFEE88)
            px(x, y, pulse, ps); px(x + 1, y, pulse, ps)
            px(x, y + 1, pulse, ps)
        }
    }

    // ─────────────────────────────────────────────────────
    // COWBOY HAT — коричневая широкополая шляпа
    // ─────────────────────────────────────────────────────
    private fun DrawScope.drawCowboyHat(ps: Float) {
        val brown  = Color(0xFF8B5E3C)
        val dark   = Color(0xFF6B4020)
        val darker = Color(0xFF4A2A0A)
        val band   = Color(0xFFCC8844)
        // Поля (широкие)
        rect(5, 20, 36, 3, dark, ps)
        rect(4, 20, 38, 2, brown, ps)
        // Тулья
        rect(14, 10, 18, 11, brown, ps)
        rect(15, 9, 16, 2, dark, ps)
        // Вмятина сверху
        rect(16, 9, 14, 1, darker, ps)
        // Лента
        rect(14, 18, 18, 2, band, ps)
        // Блики
        px(17, 12, Color(0xFFC09060), ps)
        px(26, 11, Color(0xFFC09060), ps)
        // Тени
        rect(5, 21, 3, 2, darker, ps)
        rect(38, 21, 3, 2, darker, ps)
    }
}

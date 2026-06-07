package com.baddysays.saylat.tamagotchi

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Рендерер пиксельного ёжика 48×48.
 * Вызывай drawPet(drawScope, emotion, frame, pixelSize) внутри Canvas {}.
 *
 * pixelSize — размер одного пикселя в dp (обычно 5f → 240dp итого).
 */
object PetRenderer {

    private const val W = 48
    private const val H = 48

    // ─────────────────────────────────────────────────────
    // PALETTE
    // ─────────────────────────────────────────────────────
    private val SpineDark   = Color(0xFF140802)
    private val SpineMid1   = Color(0xFF251005)
    private val SpineMid2   = Color(0xFF3D1A08)
    private val SpineMid3   = Color(0xFF5A2A10)
    private val SpineMid4   = Color(0xFF7A3E18)
    private val SpineTip    = Color(0xFFB87840)
    private val BodyDark    = Color(0xFFC07838)
    private val BodyMid     = Color(0xFFD09048)
    private val BodyLight   = Color(0xFFD89A50)
    private val BodyHigh    = Color(0xFFE4A858)
    private val BellyMain   = Color(0xFFF8E8B8)
    private val BellyLight  = Color(0xFFFFF5D0)
    private val BellyBright = Color(0xFFFFFAE0)
    private val FaceDark    = Color(0xFFECC898)
    private val FaceMid     = Color(0xFFF4D8A8)
    private val FaceLight   = Color(0xFFF8E0B0)
    private val EyeWhite    = Color(0xFFFFFFFF)
    private val EyeBlack    = Color(0xFF0A0A20)
    private val EyeIris     = Color(0xFF3070D8)
    private val EyeIrisL    = Color(0xFF60A0FF)
    private val NoseDark    = Color(0xFFC03050)
    private val NoseMid     = Color(0xFFE06080)
    private val NoseLight   = Color(0xFFFF9AB0)
    private val MouthDark   = Color(0xFF601010)
    private val MouthMid    = Color(0xFF902828)
    private val MouthLight  = Color(0xFFC05050)
    private val Tongue      = Color(0xFFFF4466)
    private val Brow        = Color(0xFF3A1A08)
    private val Cheek       = Color(0x88FFAABB)
    private val LegDark     = Color(0xFF8A5018)
    private val LegMid      = Color(0xFFA86828)

    // ─────────────────────────────────────────────────────
    // CORE DRAW PRIMITIVES
    // ─────────────────────────────────────────────────────

    private fun DrawScope.px(x: Int, y: Int, color: Color, ps: Float) {
        if (x < 0 || x >= W || y < 0 || y >= H) return
        drawRect(color = color, topLeft = Offset(x * ps, y * ps), size = androidx.compose.ui.geometry.Size(ps, ps))
    }

    private fun DrawScope.fillRect(x: Int, y: Int, w: Int, h: Int, color: Color, ps: Float) {
        drawRect(color = color, topLeft = Offset(x * ps, y * ps),
            size = androidx.compose.ui.geometry.Size(w * ps, h * ps))
    }

    private fun DrawScope.ellipse(cx: Float, cy: Float, rx: Float, ry: Float, color: Color, ps: Float) {
        val x0 = floor(cx - rx).toInt(); val x1 = (cx + rx + 0.5f).toInt()
        val y0 = floor(cy - ry).toInt(); val y1 = (cy + ry + 0.5f).toInt()
        for (y in y0..y1) for (x in x0..x1) {
            val dx = (x - cx) / rx; val dy = (y - cy) / ry
            if (dx * dx + dy * dy <= 1f) px(x, y, color, ps)
        }
    }

    private fun DrawScope.line(x0: Int, y0: Int, x1: Int, y1: Int, color: Color, ps: Float) {
        val dx = abs(x1 - x0); val dy = abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1; val sy = if (y0 < y1) 1 else -1
        var err = dx - dy; var x = x0; var y = y0
        repeat(120) {
            px(x, y, color, ps)
            if (x == x1 && y == y1) return
            val e2 = 2 * err
            if (e2 > -dy) { err -= dy; x += sx }
            if (e2 < dx)  { err += dx; y += sy }
        }
    }

    // ─────────────────────────────────────────────────────
    // BASE BODY
    // ─────────────────────────────────────────────────────

    private fun DrawScope.drawBase(ps: Float) {
        // Shadow
        ellipse(23f, 46f, 11f, 1.5f, Color(0x1F000000), ps)
        // Spine mass (dark silhouette layers)
        ellipse(23f, 27f, 18f, 17f, SpineDark, ps)
        ellipse(23f, 27f, 17f, 16f, SpineMid1, ps)
        ellipse(23f, 28f, 15f, 14f, SpineMid2, ps)
        ellipse(23f, 28f, 14f, 13f, SpineMid3, ps)
        ellipse(23f, 29f, 12f, 12f, SpineMid4, ps)
        // Individual spine tips
        val spines = listOf(
            Triple(14, 6,  0x1A0804), Triple(17, 3,  0x200C06), Triple(20, 1,  0x250E06),
            Triple(23, 0,  0x280F07), Triple(26, 1,  0x250E06), Triple(29, 3,  0x200C06),
            Triple(32, 6,  0x1A0804), Triple(12, 10, 0x2A1008), Triple(15, 7,  0x321408),
            Triple(18, 4,  0x381608), Triple(21, 2,  0x3C1808), Triple(24, 1,  0x3E1908),
            Triple(27, 3,  0x3C1808), Triple(30, 6,  0x381608), Triple(33, 10, 0x321408),
            Triple(35, 13, 0x2A1008),
        )
        spines.forEach { (x, y, col) ->
            px(x, y,   Color(0xFF000000 or col.toLong()),  ps)
            px(x, y+1, Color(0xFF4A2010), ps)
            px(x, y+2, Color(0xFF6A3A18), ps)
            px(x, y+3, Color(0xFF8A5020), ps)
        }
        // Spine tip highlights
        listOf(14 to 5, 17 to 2, 20 to 0, 23 to 0, 26 to 0, 29 to 2, 32 to 5)
            .forEach { (x, y) -> px(x, y, SpineTip, ps) }
        // Main body
        ellipse(23f, 30f, 14f, 12f, BodyDark, ps)
        ellipse(23f, 29f, 13f, 11f, BodyMid, ps)
        ellipse(22f, 28f, 11f, 9f,  BodyLight, ps)
        ellipse(21f, 27f, 8f,  6f,  BodyHigh, ps)
        // Belly
        ellipse(23f, 32f, 12f, 11f, BellyMain, ps)
        ellipse(22f, 31f, 11f, 10f, BellyLight, ps)
        ellipse(22f, 31f, 9f,  8f,  BellyBright, ps)
        // Face
        ellipse(22f, 27f, 11f, 10f, FaceDark, ps)
        ellipse(22f, 27f, 10f, 9f,  FaceMid, ps)
        ellipse(22f, 27f, 9f,  8f,  FaceLight, ps)
        // Ear hints
        ellipse(11f, 19f, 3f, 2f, BodyMid, ps)
        ellipse(10f, 18f, 2f, 1.5f, BodyLight, ps)
        px(10, 17, BodyHigh, ps)
        // Legs
        ellipse(15f, 43f, 5f, 3f, LegMid, ps)
        ellipse(31f, 43f, 5f, 3f, LegMid, ps)
        ellipse(14f, 44f, 4f, 2f, LegDark, ps)
        ellipse(30f, 44f, 4f, 2f, LegDark, ps)
        ellipse(13f, 45f, 3f, 1.5f, Color(0xFF6A3810), ps)
        ellipse(29f, 45f, 3f, 1.5f, Color(0xFF6A3810), ps)
    }

    // ─────────────────────────────────────────────────────
    // EYE DRAWING
    // ─────────────────────────────────────────────────────

    private val LX = 17; private val LY = 23
    private val RX = 28; private val RY = 23

    private fun DrawScope.eyeNormal(x: Int, y: Int, ps: Float) {
        ellipse(x.toFloat(), y.toFloat(), 3.5f, 3.5f, EyeWhite, ps)
        ellipse(x.toFloat(), y.toFloat(), 2.5f, 2.5f, EyeIris, ps)
        ellipse(x.toFloat(), y.toFloat(), 1.2f, 1.2f, EyeBlack, ps)
        px(x-1, y-1, EyeWhite, ps)
    }
    private fun DrawScope.eyeHappy(x: Int, y: Int, ps: Float) {
        for (dx in -3..3) px(x+dx, y+1+abs(dx)/2, EyeBlack, ps)
    }
    private fun DrawScope.eyeSad(x: Int, y: Int, ps: Float) {
        for (dx in -3..3) px(x+dx, y-abs(dx)/2, EyeBlack, ps)
    }
    private fun DrawScope.eyeClosed(x: Int, y: Int, ps: Float) {
        for (dx in -3..3) px(x+dx, y, Brow, ps)
        px(x-2, y-1, Color(0xFF5A2A10), ps); px(x+2, y-1, Color(0xFF5A2A10), ps)
    }
    private fun DrawScope.eyeX(x: Int, y: Int, ps: Float) {
        val c = Color(0xFFCC2222)
        line(x-2, y-2, x+2, y+2, c, ps); line(x+2, y-2, x-2, y+2, c, ps)
        px(x, y, Color(0xFFFF4444), ps)
    }
    private fun DrawScope.eyeHeart(x: Int, y: Int, ps: Float) {
        val h = Color(0xFFFF2244)
        listOf(0 to -1, 1 to -2, -1 to -2, 2 to -1, -2 to -1,
               2 to 0, -2 to 0, 1 to 1, -1 to 1, 0 to 2)
            .forEach { (dx, dy) -> px(x+dx, y+dy, h, ps) }
    }
    private fun DrawScope.eyeStar(x: Int, y: Int, ps: Float) {
        val s = Color(0xFFFFD700)
        listOf(0 to -3, 0 to 3, -3 to 0, 3 to 0, 2 to -2, -2 to -2,
               2 to 2, -2 to 2, 0 to -2, 0 to 2, -2 to 0, 2 to 0,
               0 to -1, 0 to 1, -1 to 0, 1 to 0, 0 to 0)
            .forEach { (dx, dy) -> px(x+dx, y+dy, s, ps) }
    }
    private fun DrawScope.eyeSpiral(x: Int, y: Int, ps: Float) {
        val s = Color(0xFF8844CC)
        listOf(0 to 0, 1 to 0, 1 to -1, 0 to -1, -1 to -1, -1 to 0, -1 to 1,
               0 to 1, 1 to 1, 2 to 1, 2 to 0, 2 to -1, 2 to -2, 1 to -2, 0 to -2)
            .forEach { (dx, dy) -> px(x+dx-1, y+dy+1, s, ps) }
    }
    private fun DrawScope.eyeTired(x: Int, y: Int, ps: Float) {
        ellipse(x.toFloat(), y+1f, 3.5f, 2.5f, EyeWhite, ps)
        ellipse(x.toFloat(), y+1f, 2.5f, 1.5f, EyeIris, ps)
        ellipse(x.toFloat(), y+1f, 1f, 1f, EyeBlack, ps)
        for (dx in -3..3) px(x+dx, y-1, Brow, ps)
    }
    private fun DrawScope.eyeSide(x: Int, y: Int, ps: Float) {
        ellipse(x+1f, y.toFloat(), 2.5f, 2.5f, EyeWhite, ps)
        ellipse(x+2f, y.toFloat(), 1.5f, 1.5f, EyeIris, ps)
        ellipse(x+2f, y.toFloat(), 0.8f, 0.8f, EyeBlack, ps)
        px(x+1, y-1, EyeWhite, ps)
    }
    private fun DrawScope.eyeBig(x: Int, y: Int, ps: Float) {
        ellipse(x.toFloat(), y.toFloat(), 4f, 4f, EyeWhite, ps)
        ellipse(x.toFloat(), y.toFloat(), 3f, 3f, EyeIris, ps)
        ellipse(x.toFloat(), y.toFloat(), 1.5f, 1.5f, EyeBlack, ps)
        px(x-1, y-2, EyeWhite, ps); px(x+1, y-1, EyeWhite, ps)
    }
    private fun DrawScope.eyeLoading(x: Int, y: Int, frame: Int, ps: Float) {
        ellipse(x.toFloat(), y.toFloat(), 3.5f, 3.5f, EyeWhite, ps)
        val a = (frame * 0.08f) % (2 * Math.PI.toFloat())
        for (i in 0..5) {
            val ang = a + i * Math.PI.toFloat() / 3f
            val dx = round(cos(ang) * 2.5f).toInt()
            val dy = round(sin(ang) * 2.5f).toInt()
            px(x+dx, y+dy, if (i < 2) EyeIris else Color(0xFFCCDDFF), ps)
        }
    }
    private fun DrawScope.eyeRainbow(x: Int, y: Int, ps: Float) {
        val cols = listOf(Color(0xFFFF0000), Color(0xFFFF8800), Color(0xFFFFD700),
                          Color(0xFF00CC44), Color(0xFF2288FF), Color(0xFF9922CC))
        cols.forEachIndexed { i, c ->
            val r = 3f - i * 0.3f
            ellipse(x.toFloat(), y.toFloat(), r+0.5f, r+0.5f, c, ps)
        }
        px(x-1, y-1, EyeWhite, ps)
    }
    private fun DrawScope.eyeSunglasses(ps: Float) {
        val dark = Color(0xFF222222); val frame_ = Color(0xFF444444)
        fillRect(LX-3, LY-2, 6, 5, dark, ps); fillRect(RX-3, RY-2, 6, 5, dark, ps)
        for (dx in 0..3) px(LX+3+dx, LY, EyeBlack, ps)
        fillRect(LX-3, LY-3, 7, 1, frame_, ps); fillRect(RX-3, RY-3, 7, 1, frame_, ps)
        px(LX-1, LY-1, Color(0xFFAAAAFF), ps); px(RX-1, RY-1, Color(0xFFAAAAFF), ps)
    }
    private fun DrawScope.eyeGlasses(ps: Float) {
        eyeNormal(LX, LY, ps); eyeNormal(RX, RY, ps)
        val fr = Color(0xFF8B6914)
        for (i in 0..3) {
            val a = i * Math.PI.toFloat() / 2f
            px(LX + round(cos(a)*3.5f).toInt(), LY + round(sin(a)*3.5f).toInt(), fr, ps)
            px(RX + round(cos(a)*3.5f).toInt(), RY + round(sin(a)*3.5f).toInt(), fr, ps)
        }
        for (dx in 0..3) px(LX+3+dx, LY, fr, ps)
    }
    private fun DrawScope.eyeSearching(ps: Float) {
        eyeNormal(LX, LY, ps)
        val fr = Color(0xFF5A3A10)
        for (i in 0..7) {
            val a = i * Math.PI.toFloat() / 4f
            px(RX + round(cos(a)*4f).toInt(), RY + round(sin(a)*4f).toInt(), fr, ps)
        }
        line(RX+3, RY+3, RX+5, RY+5, fr, ps)
    }
    private fun DrawScope.eyeAngry(x: Int, y: Int, side: Int, ps: Float) {
        ellipse(x.toFloat(), y.toFloat(), 3f, 3f, EyeWhite, ps)
        ellipse(x.toFloat(), y.toFloat(), 2f, 2f, Color(0xFFD03030), ps)
        ellipse(x.toFloat(), y.toFloat(), 1f, 1f, EyeBlack, ps)
        if (side == 0) line(x-3, y-4, x+1, y-2, Brow, ps)
        else           line(x-1, y-2, x+3, y-4, Brow, ps)
    }
    private fun DrawScope.eyeWorried(x: Int, y: Int, ps: Float) {
        eyeNormal(x, y, ps)
        px(x-1, y-4, Brow, ps); px(x, y-4, Brow, ps); px(x-2, y-3, Brow, ps)
    }
    private fun DrawScope.eyeFocused(x: Int, y: Int, ps: Float) {
        ellipse(x.toFloat(), y.toFloat(), 3.5f, 3f, EyeWhite, ps)
        ellipse(x.toFloat(), y.toFloat(), 2.5f, 2f, EyeIris, ps)
        ellipse(x.toFloat(), y.toFloat(), 1.2f, 1.2f, EyeBlack, ps)
        for (dx in -3..3) px(x+dx, y-3, Brow, ps)
    }
    private fun DrawScope.eyeLookingUp(x: Int, y: Int, ps: Float) {
        ellipse(x.toFloat(), y.toFloat(), 3.5f, 3.5f, EyeWhite, ps)
        ellipse(x.toFloat(), y-1f, 2f, 2f, EyeIris, ps)
        ellipse(x.toFloat(), y-1f, 1f, 1f, EyeBlack, ps)
        px(x-1, y-2, EyeWhite, ps)
    }
    private fun DrawScope.eyeDetermined(x: Int, y: Int, ps: Float) {
        ellipse(x.toFloat(), y.toFloat(), 3f, 3f, EyeWhite, ps)
        ellipse(x.toFloat(), y.toFloat(), 2f, 2f, Color(0xFFD04020), ps)
        ellipse(x.toFloat(), y.toFloat(), 1f, 1f, EyeBlack, ps)
        for (dx in -3..3) px(x+dx, y-4, Brow, ps)
        for (dx in -2..2) px(x+dx, y-3, Brow, ps)
    }

    // ─────────────────────────────────────────────────────
    // MOUTH DRAWING
    // ─────────────────────────────────────────────────────

    private val MX = 23; private val MY = 34

    private fun DrawScope.mouthSmile(small: Boolean = false, ps: Float) {
        val w = if (small) 2 else 3
        for (dx in -w..w) px(MX+dx, MY + (dx*dx/(w+1)), MouthMid, ps)
    }
    private fun DrawScope.mouthBigSmile(ps: Float) {
        for (dx in -5..5) px(MX+dx, MY + dx*dx/6, MouthDark, ps)
        ellipse(MX.toFloat(), MY+3f, 4f, 2.5f, MouthLight, ps)
        ellipse(MX.toFloat(), MY+3f, 3f, 2f, EyeWhite, ps)
        px(MX-1, MY+3, EyeWhite, ps); px(MX+1, MY+3, EyeWhite, ps)
    }
    private fun DrawScope.mouthGrin(ps: Float) {
        for (dx in -5..5) px(MX+dx, MY + dx*dx/6, MouthDark, ps)
        fillRect(MX-4, MY+1, 3, 3, EyeWhite, ps)
        fillRect(MX+2, MY+1, 3, 3, EyeWhite, ps)
        fillRect(MX-1, MY+1, 2, 3, EyeWhite, ps)
        px(MX-4, MY+4, MouthDark, ps); px(MX+4, MY+4, MouthDark, ps)
    }
    private fun DrawScope.mouthFrown(ps: Float) {
        for (dx in -3..3) px(MX+dx, MY - dx*dx/4, MouthDark, ps)
    }
    private fun DrawScope.mouthO(ps: Float) {
        ellipse(MX.toFloat(), MY.toFloat(), 3f, 3f, MouthDark, ps)
        ellipse(MX.toFloat(), MY.toFloat(), 2f, 2f, Tongue, ps)
    }
    private fun DrawScope.mouthFlat(ps: Float) {
        for (dx in -3..3) px(MX+dx, MY, MouthMid, ps)
    }
    private fun DrawScope.mouthWavy(ps: Float) {
        val ys = listOf(0,-1,0,1,0,-1,0)
        for (i in 0..6) px(MX-3+i, MY+ys[i], MouthMid, ps)
    }
    private fun DrawScope.mouthYawn(ps: Float) {
        ellipse(MX.toFloat(), MY+1f, 6f, 5f, MouthDark, ps)
        ellipse(MX.toFloat(), MY+2f, 5f, 4f, Tongue, ps)
        fillRect(MX-3, MY+1, 7, 2, EyeWhite, ps)
    }
    private fun DrawScope.mouthSmirk(ps: Float) {
        for (dx in 0..4) px(MX+dx, MY + dx*dx/5, MouthMid, ps)
        for (dx in -2..0) px(MX+dx, MY, MouthMid, ps)
    }
    private fun DrawScope.mouthEating(ps: Float) {
        ellipse(MX.toFloat(), MY.toFloat(), 4f, 3f, MouthDark, ps)
        ellipse(MX.toFloat(), MY.toFloat(), 3f, 2f, Color(0xFFFFA040), ps)
    }
    private fun DrawScope.mouthChattering(ps: Float) {
        for (dx in -3..3) px(MX+dx, MY, MouthMid, ps)
        for (dx in -2..2) px(MX+dx, MY+2, MouthMid, ps)
        fillRect(MX-2, MY, 5, 2, EyeWhite, ps)
    }
    private fun DrawScope.mouthDead(ps: Float) {
        mouthFlat(ps)
        px(MX-2, MY, Color(0xFF666666), ps); px(MX+2, MY, Color(0xFF666666), ps)
    }
    private fun DrawScope.mouthDetermined(ps: Float) {
        for (dx in -3..3) px(MX+dx, MY, MouthDark, ps)
        for (dx in -2..2) px(MX+dx, MY+1, MouthLight, ps)
    }

    // ─────────────────────────────────────────────────────
    // BROWS
    // ─────────────────────────────────────────────────────

    private fun DrawScope.browNormal(ps: Float) {
        line(LX-2, LY-5, LX+2, LY-4, Brow, ps)
        line(RX-2, RY-4, RX+2, RY-5, Brow, ps)
    }
    private fun DrawScope.browAngry(ps: Float) {
        line(LX-3, LY-5, LX+2, LY-3, Brow, ps)
        line(RX-2, RY-3, RX+3, RY-5, Brow, ps)
    }
    private fun DrawScope.browWorried(ps: Float) {
        line(LX-2, LY-5, LX+3, LY-4, Brow, ps)
        line(RX-3, RY-4, RX+2, RY-5, Brow, ps)
    }
    private fun DrawScope.browFocused(ps: Float) {
        for (dx in -2..2) { px(LX+dx, LY-4, Brow, ps); px(RX+dx, RY-4, Brow, ps) }
    }

    // ─────────────────────────────────────────────────────
    // NOSE
    // ─────────────────────────────────────────────────────

    private val NX = 23; private val NY = 30

    private fun DrawScope.drawNose(ps: Float) {
        ellipse(NX.toFloat(), NY.toFloat(), 2.5f, 1.5f, NoseMid, ps)
        ellipse(NX.toFloat(), NY.toFloat(), 1.5f, 1f, NoseDark, ps)
        px(NX-1, NY-1, NoseLight, ps)
    }

    // ─────────────────────────────────────────────────────
    // ACCESSORIES
    // ─────────────────────────────────────────────────────

    private fun DrawScope.accZZZ(frame: Int, ps: Float) {
        val off = (frame / 25) % 4
        val zColor = Color(0xFF8888CC)
        for (s in 1..3) {
            val bx = 32 + off + s; val by = 14 - s * 3 - off
            for (dx in 0..s*2) px(bx+dx, by, zColor, ps)
            for (dy in 1 until s) px(bx + s*2 - (dy*2/s.coerceAtLeast(1)), by+dy, zColor, ps)
            for (dx in 0..s*2) px(bx+dx, by+s, zColor, ps)
        }
    }
    private fun DrawScope.accTears(frame: Int, ps: Float) {
        val drip = (frame / 8) % 7
        for (i in 0 until drip) {
            px(16, 32+i, Color(0xFF66AAFF), ps)
            px(28, 32+i, Color(0xFF99CCFF), ps)
        }
    }
    private fun DrawScope.accHeartsFloat(frame: Int, ps: Float) {
        val t = frame * 0.05f
        val h = Color(0xFFFF2244)
        listOf(Triple(8, 14, 0f), Triple(36, 12, Math.PI.toFloat()), Triple(5, 22, Math.PI.toFloat()/2f))
            .forEach { (bx, by, phase) ->
                val x = (bx + round(sin(t + phase))).toInt()
                val y = (by + round(cos(t + phase)) - if (frame % 30 > 15) 1 else 0).toInt()
                px(x, y, h, ps); px(x + 1, y, h, ps); px(x + 2, y, h, ps)
                px(x, y + 1, h, ps); px(x + 1, y + 1, Color(0xFFFF6666), ps); px(x + 2, y + 1, h, ps)
                px(x + 1, y + 2, h, ps)
            }
    }
    private fun DrawScope.accStarsFloat(frame: Int, ps: Float) {
        val t = frame * 0.06f
        listOf(Triple(7, 10, 0f), Triple(38, 8, 1f), Triple(5, 20, 2f), Triple(40, 18, 3f))
            .forEach { (bx, by, phase) ->
                val x = (bx + round(sin(t + phase))).toInt()
                val y = (by + round(cos(t * 0.7f + phase))).toInt()
                val c = if ((frame + (phase * 10).toInt()) % 20 > 10) Color(0xFFFFD700) else Color(0xFFFFA500)
                px(x, y, c, ps); px(x + 1, y - 1, c, ps); px(x + 2, y, c, ps); px(x + 1, y + 1, c, ps)
            }
    }
    private fun DrawScope.accMusicNotes(frame: Int, ps: Float) {
        val t = frame * 0.05f
        val c = Color(0xFF9922CC)
        listOf(8f to 14f, 36f to 12f).forEachIndexed { idx, (bx, by) ->
            val x = (bx + sin(t + idx * 1.5f)).toInt()
            val y = by.toInt() - ((frame * 0.04f + idx * 8f) % 8f).toInt()
            px(x+2, y, c, ps); px(x+3, y, c, ps); px(x+4, y, c, ps)
            px(x+4, y+1, c, ps); px(x+4, y+2, c, ps)
            ellipse(x+2f, y+3f, 1.5f, 1f, c, ps)
        }
    }
    private fun DrawScope.accCoffeeSteam(frame: Int, ps: Float) {
        for (i in 0..4) {
            val y = 12 - i * 2
            val x = 22 + round(sin(frame * 0.1f + i) * 1.5f).toInt()
            px(x, y, Color(0x44AACCFF), ps); px(x, y+1, Color(0x3388AAEE), ps)
        }
    }
    private fun DrawScope.accSpeedLines(ps: Float) {
        val c = Color(0x22EEEEEE)
        for (i in 0..4) {
            val y = 20 + i * 3
            line(0, y, 8, y, c, ps); line(38, y, 46, y, c, ps)
        }
    }
    private fun DrawScope.accThoughtBubble(ps: Float) {
        ellipse(36f, 12f, 5f, 3f, EyeWhite, ps)
        ellipse(36f, 12f, 4f, 2.5f, Color(0xFFEEEEEE), ps)
        px(30, 16, EyeWhite, ps); px(28, 18, EyeWhite, ps); px(26, 20, EyeWhite, ps)
    }
    private fun DrawScope.accSweatDrops(frame: Int, ps: Float) {
        val drip = (frame / 12) % 4
        val c = Color(0xFFAADDFF)
        listOf(14 to 16, 30 to 15).forEach { (x, y) ->
            px(x, y+drip, c, ps); px(x, y+drip+1, Color(0xFF88BBEE), ps)
        }
    }
    private fun DrawScope.accConfetti(frame: Int, ps: Float) {
        val cols = listOf(Color(0xFFFF2244), Color(0xFFFFD700), Color(0xFF00CC44),
                          Color(0xFF2288FF), Color(0xFFFF8800), Color(0xFFCC22CC))
        for (i in 0..19) {
            val x = ((i * 7 + frame * 0.1f * (if (i % 2 == 0) 1 else -1)) % 48).toInt()
            val y = ((i * 3 + sin(frame * 0.05f + i) * 3 + frame * 0.05f) % 20 + 5).toInt()
            px(x.coerceIn(0, 47), y.coerceIn(0, 47), cols[i % cols.size], ps)
        }
    }
    private fun DrawScope.accFireworks(frame: Int, ps: Float) {
        val t = frame * 0.08f
        val cols = listOf(Color(0xFFFF2244), Color(0xFFFFD700), Color(0xFF00CC44),
                          Color(0xFF2288FF), Color(0xFFFF8800))
        listOf(Triple(8, 8, 0f), Triple(38, 6, 2f), Triple(24, 4, 4f))
            .forEachIndexed { j, (bx, by, _) ->
                val phase = t + j * 2f
                for (i in 0..7) {
                    val a = i * Math.PI.toFloat() / 4f + phase
                    val r = 3f + sin(phase) * 1.5f
                    val x = bx + round(cos(a) * r).toInt()
                    val y = by + round(sin(a) * r).toInt()
                    px(x, y, cols[i % cols.size], ps)
                }
            }
    }
    private fun DrawScope.accProgressBar(frame: Int, ps: Float) {
        val pct = (frame % 120) / 120f
        fillRect(9, 34, 28, 5, Color(0xFFDDDDDD), ps)
        fillRect(10, 35, (26 * pct).toInt(), 3, Color(0xFF2288FF), ps)
    }
    private fun DrawScope.accGlitchBars(frame: Int, ps: Float) {
        for (i in 0..2) {
            val y = 18 + i * 7 + frame % 3
            if (y in 15..42) {
                for (x in 0 until W) px(x, y, if (x % 2 == 0) Color(0xFF00FF44) else Color(0xFFFF0088), ps)
            }
        }
    }
    private fun DrawScope.accRainbowArc(ps: Float) {
        val cols = listOf(Color(0xFFFF0000), Color(0xFFFF8800), Color(0xFFFFD700),
                          Color(0xFF00CC44), Color(0xFF2288FF), Color(0xFF9922CC))
        cols.forEachIndexed { i, c ->
            val r = (6 - i).toFloat()
            var a = -Math.PI.toFloat()
            while (a < 0f) {
                val x = round(23 + cos(a) * r).toInt()
                val y = round(10 + sin(a) * r).toInt()
                px(x, y, c, ps)
                a += 0.15f
            }
        }
    }
    private fun DrawScope.accUnicornHorn(ps: Float) {
        val cols = listOf(Color(0xFFFFD700), Color(0xFFFF8800), Color(0xFFFF2244),
                          Color(0xFF9922CC), Color(0xFF2288FF))
        for (i in 0..7) px(23, 14-i, cols[i % cols.size], ps)
    }
    private fun DrawScope.accIceCrystals(frame: Int, ps: Float) {
        val w = (frame / 20) % 2
        val c = Color(0xFFAADDFF)
        listOf(10 to 20, 35 to 18, 7 to 28, 40 to 26).forEach { (x, y) ->
            px(x, y+w, c, ps); px(x-1, y+1+w, c, ps); px(x+1, y+1+w, c, ps); px(x, y+2+w, c, ps)
        }
    }
    private fun DrawScope.accFlash(ps: Float) {
        val c = Color(0xFFFFD700)
        listOf(38 to 8, 6 to 14).forEach { (x, y) ->
            listOf(0 to -3, 1 to -2, 0 to -1, 1 to 0, 0 to 1, 1 to 2, 0 to 3)
                .forEach { (dx, dy) -> px(x+dx, y+dy, c, ps) }
        }
    }
    private fun DrawScope.accCodeGlow(ps: Float) {
        val c = Color(0xFF22DD44)
        listOf(6 to 22, 4 to 26, 4 to 30, 37 to 22, 37 to 26, 37 to 30)
            .forEach { (x, y) -> px(x, y, c, ps); px(x+1, y, c, ps) }
    }
    private fun DrawScope.accDizzyStars(frame: Int, ps: Float) {
        val t = frame * 0.12f
        for (i in 0..2) {
            val a = t + i * 2.1f
            val x = round(23 + cos(a) * 10f).toInt()
            val y = round(24 + sin(a) * 8f - 4f).toInt()
            px(x, y, Color(0xFFFFD700), ps); px(x+1, y, Color(0xFFFF8800), ps)
            px(x, y+1, Color(0xFFFFD700), ps)
        }
    }
    private fun DrawScope.accQuestionMark(ps: Float) {
        val c = Color(0xFF9944CC)
        listOf(22 to 10, 23 to 10, 24 to 10, 25 to 11, 25 to 12, 24 to 13, 23 to 14, 23 to 16, 23 to 18)
            .forEach { (x, y) -> px(x, y, c, ps) }
    }
    private fun DrawScope.accPartyHat(ps: Float) {
        val cols = listOf(Color(0xFFFF2222), Color(0xFFFF8800), Color(0xFFFFD700),
                          Color(0xFF00CC44), Color(0xFF2288FF), Color(0xFF9922CC))
        for (i in 0..6) {
            val y = 17 - i * 2; val x = 22 - i; val w = 2 + i * 2
            fillRect(x, y, w, 2, cols[i % cols.size], ps)
        }
        ellipse(23f, 13f, 2f, 2f, Color(0xFFFFD700), ps)
        fillRect(16, 29, 13, 2, Color(0xFFFF8800), ps)
    }
    private fun DrawScope.accHeadphones(ps: Float) {
        for (dx in 0..14) px(9+dx, 17, Color(0xFF333333), ps)
        ellipse(9f, 19f, 3f, 4f, Color(0xFF222222), ps); ellipse(33f, 19f, 3f, 4f, Color(0xFF222222), ps)
        ellipse(9f, 19f, 2f, 3f, Color(0xFF4488FF), ps); ellipse(33f, 19f, 2f, 3f, Color(0xFF4488FF), ps)
    }
    private fun DrawScope.accCrown(ps: Float) {
        fillRect(14, 16, 19, 2, Color(0xFFFFD700), ps)
        listOf(14 to 14, 19 to 13, 24 to 14, 29 to 13, 33 to 14)
            .forEach { (x, y) -> fillRect(x, y, 2, 3, Color(0xFFFFD700), ps) }
        listOf(15, 20, 25, 30).forEach { x -> px(x, 16, Color(0xFFFF2244), ps) }
    }

    // ─────────────────────────────────────────────────────
    // MASTER DRAW
    // ─────────────────────────────────────────────────────

    fun DrawScope.drawPet(emotion: PetEmotion, frame: Int, pixelSize: Float) {
        // Body tint overlay applied after drawing if needed
        drawBase(pixelSize)

        // Body tint
        emotion.bodyTint?.let { tint ->
            ellipse(23f, 30f, 14f, 12f, tint.copy(alpha = 0.35f), pixelSize)
        }

        // Eyes
        when (emotion.eyeType) {
            EyeType.NORMAL     -> { eyeNormal(LX, LY, pixelSize); eyeNormal(RX, RY, pixelSize)
                                    if (frame % 90 > 87) { eyeClosed(LX, LY, pixelSize); eyeClosed(RX, RY, pixelSize) } }
            EyeType.HAPPY      -> { eyeHappy(LX, LY, pixelSize); eyeHappy(RX, RY, pixelSize) }
            EyeType.SAD        -> { eyeSad(LX, LY, pixelSize); eyeSad(RX, RY, pixelSize) }
            EyeType.BIG        -> { eyeBig(LX, LY, pixelSize); eyeBig(RX, RY, pixelSize) }
            EyeType.CLOSED     -> { eyeClosed(LX, LY, pixelSize); eyeClosed(RX, RY, pixelSize) }
            EyeType.X_EYES     -> { eyeX(LX, LY, pixelSize); eyeX(RX, RY, pixelSize) }
            EyeType.HEART      -> { eyeHeart(LX, LY, pixelSize); eyeHeart(RX, RY, pixelSize) }
            EyeType.STAR       -> { eyeStar(LX, LY, pixelSize); eyeStar(RX, RY, pixelSize) }
            EyeType.SPIRAL     -> { eyeSpiral(LX, LY, pixelSize); eyeSpiral(RX, RY, pixelSize) }
            EyeType.SIDE_EYE   -> { eyeSide(LX, LY, pixelSize); eyeSide(RX, RY, pixelSize) }
            EyeType.TIRED      -> { eyeTired(LX, LY, pixelSize); eyeTired(RX, RY, pixelSize) }
            EyeType.ANGRY      -> { eyeAngry(LX, LY, 0, pixelSize); eyeAngry(RX, RY, 1, pixelSize) }
            EyeType.WORRIED    -> { eyeWorried(LX, LY, pixelSize); eyeWorried(RX, RY, pixelSize) }
            EyeType.RAINBOW    -> { eyeRainbow(LX, LY, pixelSize); eyeRainbow(RX, RY, pixelSize) }
            EyeType.LOADING    -> { eyeLoading(LX, LY, frame, pixelSize); eyeLoading(RX, RY, frame+10, pixelSize) }
            EyeType.FOCUSED    -> { eyeFocused(LX, LY, pixelSize); eyeFocused(RX, RY, pixelSize) }
            EyeType.LOOKING_UP -> { eyeLookingUp(LX, LY, pixelSize); eyeLookingUp(RX, RY, pixelSize) }
            EyeType.SUNGLASSES -> eyeSunglasses(pixelSize)
            EyeType.GLASSES    -> eyeGlasses(pixelSize)
            EyeType.SEARCHING  -> eyeSearching(pixelSize)
            EyeType.DETERMINED -> { eyeDetermined(LX, LY, pixelSize); eyeDetermined(RX, RY, pixelSize) }
            EyeType.WINK       -> { eyeNormal(LX, LY, pixelSize); eyeClosed(RX, RY, pixelSize) }
        }

        // Brows
        when (emotion.browType) {
            BrowType.NORMAL  -> browNormal(pixelSize)
            BrowType.ANGRY   -> browAngry(pixelSize)
            BrowType.WORRIED -> browWorried(pixelSize)
            BrowType.FOCUSED -> browFocused(pixelSize)
            BrowType.NONE    -> {}
        }

        // Nose
        drawNose(pixelSize)

        // Mouth
        when (emotion.mouthType) {
            MouthType.SMILE       -> mouthSmile(ps = pixelSize)
            MouthType.TINY        -> mouthSmile(small = true, ps = pixelSize)
            MouthType.BIG_SMILE   -> mouthBigSmile(pixelSize)
            MouthType.GRIN        -> mouthGrin(pixelSize)
            MouthType.FROWN       -> mouthFrown(pixelSize)
            MouthType.O_MOUTH     -> mouthO(pixelSize)
            MouthType.FLAT        -> mouthFlat(pixelSize)
            MouthType.WAVY        -> mouthWavy(pixelSize)
            MouthType.YAWN        -> mouthYawn(pixelSize)
            MouthType.TONGUE      -> mouthSmile(ps = pixelSize)
            MouthType.SMIRK       -> mouthSmirk(pixelSize)
            MouthType.EATING      -> mouthEating(pixelSize)
            MouthType.CHATTERING  -> mouthChattering(pixelSize)
            MouthType.DEAD        -> mouthDead(pixelSize)
            MouthType.DETERMINED  -> mouthDetermined(pixelSize)
        }

        // Cheeks
        if (emotion.hasCheeks) {
            ellipse(11f, 29f, 4f, 2.5f, Cheek, pixelSize)
            ellipse(34f, 29f, 4f, 2.5f, Cheek, pixelSize)
        }

        // Accessories
        emotion.accessories.forEach { acc ->
            when (acc) {
                AccessoryType.ZZZ             -> accZZZ(frame, pixelSize)
                AccessoryType.TEARS           -> accTears(frame, pixelSize)
                AccessoryType.HEARTS_FLOAT    -> accHeartsFloat(frame, pixelSize)
                AccessoryType.STARS_FLOAT     -> accStarsFloat(frame, pixelSize)
                AccessoryType.MUSIC_NOTES     -> accMusicNotes(frame, pixelSize)
                AccessoryType.COFFEE_STEAM    -> accCoffeeSteam(frame, pixelSize)
                AccessoryType.SPEED_LINES     -> accSpeedLines(pixelSize)
                AccessoryType.THOUGHT_BUBBLE  -> accThoughtBubble(pixelSize)
                AccessoryType.SWEAT_DROPS     -> accSweatDrops(frame, pixelSize)
                AccessoryType.CONFETTI        -> accConfetti(frame, pixelSize)
                AccessoryType.FIREWORKS       -> accFireworks(frame, pixelSize)
                AccessoryType.PROGRESS_BAR    -> accProgressBar(frame, pixelSize)
                AccessoryType.GLITCH_BARS     -> accGlitchBars(frame, pixelSize)
                AccessoryType.RAINBOW_ARC     -> accRainbowArc(pixelSize)
                AccessoryType.UNICORN_HORN    -> accUnicornHorn(pixelSize)
                AccessoryType.ICE_CRYSTALS    -> accIceCrystals(frame, pixelSize)
                AccessoryType.FLASH           -> accFlash(pixelSize)
                AccessoryType.CODE_GLOW       -> accCodeGlow(pixelSize)
                AccessoryType.DIZZY_STARS     -> accDizzyStars(frame, pixelSize)
                AccessoryType.QUESTION_MARK   -> accQuestionMark(pixelSize)
                AccessoryType.PARTY_HAT       -> accPartyHat(pixelSize)
                AccessoryType.HEADPHONES      -> accHeadphones(pixelSize)
                AccessoryType.CROWN           -> accCrown(pixelSize)
                AccessoryType.STEAM           -> accCoffeeSteam(frame, pixelSize)
            }
        }
    }
}

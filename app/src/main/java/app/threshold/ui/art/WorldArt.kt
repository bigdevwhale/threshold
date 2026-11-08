package app.threshold.ui.art

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.runtime.withFrameMillis
import app.threshold.domain.World
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin

object Art {
    /** Screenshots and renders want a still picture. */
    @Volatile var still = false
}

/** Seconds since the screen appeared, ticking every frame (frozen at 4 s when still). */
@Composable
fun rememberTime(): State<Float> {
    if (Art.still || LocalInspectionMode.current) return remember { mutableFloatStateOf(4f) }
    return produceState(0f) {
        val t0 = withFrameMillis { it }
        while (true) withFrameMillis { value = (it - t0) / 1000f }
    }
}

internal fun rnd(i: Int): Float {
    val x = sin(i * 127.1 + 311.7) * 43758.5453
    return (x - floor(x)).toFloat()
}

internal fun col(hex: Long, a: Float = 1f) = Color(hex).copy(alpha = a.coerceIn(0f, 1f))

private fun DrawScope.vgrad(w: Float, h: Float, vararg stops: Pair<Float, Color>, top: Float = 0f, bottom: Float = h) =
    drawRect(Brush.verticalGradient(*stops, startY = top, endY = bottom), Offset(0f, top), Size(w, bottom - top))

private fun DrawScope.stars(w: Float, h: Float, t: Float, n: Int, seed: Int, u: Float) {
    for (i in 0 until n) {
        val a = 0.35f + 0.45f * abs(sin(t * (0.5f + rnd(i + seed)) + i))
        drawRect(Color.White.copy(alpha = a), Offset(rnd(i + seed) * w, rnd(i + seed + 77) * h * 0.5f), Size(1.3f * u, 1.3f * u))
    }
}

private fun poly(vararg p: Float) = Path().apply {
    moveTo(p[0], p[1])
    var i = 2
    while (i < p.size) { lineTo(p[i], p[i + 1]); i += 2 }
    close()
}

/** Draws [world] into a w×h box at the origin; [u] is one prototype pixel. */
fun DrawScope.world(world: World, w: Float, h: Float, t: Float) {
    val u = w / 220f
    when (world) {
        World.NEWS -> news(w, h, t, u)
        World.MUSIC -> music(w, h, t, u)
        World.SPORT -> sport(w, h, t, u)
        World.RADIO -> radio(w, h, t, u)
        World.CINEMA -> cinema(w, h, t, u)
        World.GARDEN -> garden(w, h, t, u)
        World.WILD -> void(w, h, t, World.WILD.accent, 3f)
    }
}

private fun DrawScope.news(w: Float, h: Float, t: Float, u: Float) {
    vgrad(w, h, 0f to col(0xFF24305E), 0.55f to col(0xFFC8735A), 1f to col(0xFFF3B26B))
    drawCircle(col(0xFFFFE4AA, 0.9f), w * 0.09f, Offset(w * 0.3f, h * 0.6f))
    for (i in 0 until 11) {
        val bw = w * (0.07f + rnd(i) * 0.06f)
        val x = i * w / 10 - bw / 2 + rnd(i + 20) * 8 * u
        val bh = h * (0.22f + rnd(i + 40) * 0.42f)
        val y = h - bh
        val c = if (i % 2 == 1) col(0xFF1B1F3B) else col(0xFF252A52)
        drawRect(c, Offset(x, y), Size(bw, bh))
        drawPath(poly(x, y, x + bw / 2, y - bw * 0.9f, x + bw, y), c)
        for (r in 0 until 24) for (k in 0 until 2) {
            val wy = y + 8 * u + r * 11 * u
            if (wy < h - 4 * u && rnd(i * 50 + r * 3 + k) > 0.58f)
                drawRect(col(0xFFFFD27A), Offset(x + bw * (0.22f + k * 0.38f), wy), Size(maxOf(2 * u, bw * 0.14f), 4 * u))
        }
    }
    for (k in 0 until 9) {
        val sp = (12 + rnd(k) * 18) * u
        val bx = ((t * sp + rnd(k + 3) * w * 2) % (w + 60 * u)) - 30 * u
        val by = h * (0.1f + rnd(k + 7) * 0.38f) + sin(t * 1.5f + k) * 5 * u
        val f = (3 + 2.5f * sin(t * 9 + k * 2)) * u
        drawPath(Path().apply { moveTo(bx - 6 * u, by - f); lineTo(bx, by); lineTo(bx + 6 * u, by - f) }, col(0xFFFFF8EC), style = Stroke(1.4f * u))
    }
}

private fun DrawScope.music(w: Float, h: Float, t: Float, u: Float) {
    val y0 = h * 0.55f
    vgrad(w, h, 0f to col(0xFF0F0B26), 1f to col(0xFF3B2470), bottom = y0)
    stars(w, h, t, 40, 5, u)
    drawCircle(col(0xFFF4EAD5), w * 0.075f, Offset(w * 0.68f, h * 0.2f))
    drawRect(col(0xFF0C0A17), Offset(0f, y0), Size(w, h - y0))
    for (j in 0 until 28) {
        val p = j / 28f
        val y = y0 + p.pow(1.6f) * (h - y0)
        val path = Path()
        var x = 0f
        var first = true
        while (x <= w) {
            val yy = y + sin(x / u * 0.03f + t * 1.2f + j * 0.7f) * (1 + j * 0.15f) * u
            if (first) { path.moveTo(x, yy); first = false } else path.lineTo(x, yy)
            x += 5 * u
        }
        drawPath(path, Color(160, 130, 230).copy(alpha = 0.1f + 0.25f * p), style = Stroke(1f * u))
    }
    for (k in 0 until 12) {
        val ww = (8 + k * 2.4f) * u
        drawRect(col(0xFFF4EAD5, 0.55f - k * 0.04f), Offset(w * 0.68f - ww / 2 + sin(t * 2 + k) * 3 * u, y0 + (4 + k * k * 1.3f) * u), Size(ww, 1.6f * u))
    }
}

private fun sportAt(w: Float, h: Float, p: Float) = Offset(w * 0.55f + sin(p * Math.PI.toFloat() * 5) * w * 0.14f * (1 - p), h * 0.99f + (h * 0.31f - h * 0.99f) * p)

private fun DrawScope.sport(w: Float, h: Float, t: Float, u: Float) {
    vgrad(w, h, 0f to col(0xFF5B4B7A), 0.55f to col(0xFFE07A6E), 1f to col(0xFFF6C28B))
    drawCircle(col(0xFFFFE3B0), w * 0.08f, Offset(w * 0.78f, h * 0.36f))
    drawPath(poly(0f, h * 0.65f, w * 0.22f, h * 0.42f, w * 0.42f, h * 0.6f, w * 0.72f, h * 0.38f, w, h * 0.58f, w, h, 0f, h), col(0xFF6A5582))
    drawPath(poly(w * 0.02f, h, w * 0.55f, h * 0.28f, w * 1.05f, h), col(0xFF2F3A5C))
    drawPath(poly(w * 0.55f, h * 0.28f, w * 0.47f, h * 0.39f, w * 0.51f, h * 0.37f, w * 0.55f, h * 0.4f, w * 0.59f, h * 0.36f, w * 0.63f, h * 0.39f), col(0xFFECE8F4))
    val path = Path()
    for (i in 0..60) { val o = sportAt(w, h, i / 60f); if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y) }
    drawPath(path, col(0xFFF3D9B1), style = Stroke(2 * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2 * u, 3 * u))))
    val r = sportAt(w, h, (t * 0.045f) % 1f)
    drawCircle(col(0xFFFFF0D2, 0.35f), 6 * u, r)
    drawCircle(Color.White, 2.6f * u, r)
}

private fun DrawScope.radio(w: Float, h: Float, t: Float, u: Float) {
    val hz = h * 0.66f
    vgrad(w, h, 0f to col(0xFF0A2F3A), 0.75f to col(0xFF2A7F86), 1f to col(0xFF9FD8C9), bottom = hz)
    drawRect(col(0xFF082028), Offset(0f, hz), Size(w, h - hz))
    for (k in 0 until 10) {
        val y = hz + (8 + k * k * 1.6f) * u
        val off = (t * 8 + k * 17) % 40 * u
        var x = -off
        while (x < w) { drawLine(Color(160, 230, 220).copy(alpha = 0.18f), Offset(x, y), Offset(x + 14 * u, y), u); x += 40 * u }
    }
    val isl = listOf(Triple(0.22f, 0.2f, 0.42f), Triple(0.62f, 0.24f, 0.5f), Triple(0.9f, 0.12f, 0.3f))
    isl.forEachIndexed { i, (ix, iw, mh) ->
        val x = w * ix; val y = hz + 2 * u
        drawArc(col(0xFF061A20), 180f, 180f, true, Offset(x - w * iw / 2, y - 10 * u), Size(w * iw, 20 * u))
        val top = y - h * mh
        val mast = col(0xFF04141A)
        drawLine(mast, Offset(x, y), Offset(x, top), 2 * u)
        for (k in 1 until 5) { val yy = y + (top - y) * k / 5f; val s = 9 * (1 - k / 6f) * u; drawLine(mast, Offset(x - s, yy), Offset(x + s, yy), u) }
        drawLine(mast, Offset(x - 10 * u, y), Offset(x, top + 20 * u), u); drawLine(mast, Offset(x, top + 20 * u), Offset(x + 10 * u, y), u)
        val on = sin(t * 3 + i * 2) > 0
        drawCircle(if (on) col(0xFFFF5A4F) else col(0xFF5A2020), 2.5f * u, Offset(x, top))
        for (k in 0 until 3) {
            val r = (t * 18 + k * 25 + i * 10) % 75
            drawCircle(Color(170, 240, 230).copy(alpha = (1 - r / 75) * 0.55f), r * u, Offset(x, top), style = Stroke(u))
        }
    }
}

private fun DrawScope.cinema(w: Float, h: Float, t: Float, u: Float) {
    vgrad(w, h, 0f to col(0xFF04050D), 1f to col(0xFF161B3A))
    stars(w, h, t, 45, 31, u)
    val sx = w * 0.14f; val sy = h * 0.2f; val sw = w * 0.72f; val sh = h * 0.3f
    val post = col(0xFF0A0B16)
    drawLine(post, Offset(sx + 6 * u, sy + sh), Offset(sx + 6 * u, h * 0.8f), 3 * u)
    drawLine(post, Offset(sx + sw - 6 * u, sy + sh), Offset(sx + sw - 6 * u, h * 0.8f), 3 * u)
    drawPath(poly(w * 0.5f, h * 0.93f, sx, sy, sx + sw, sy + sh), Color(200, 215, 255).copy(alpha = 0.08f))
    val b = 0.72f + 0.08f * sin(t * 17) + 0.05f * sin(t * 5.3f)
    drawRect(Brush.verticalGradient(listOf(Color(205, 220, 255).copy(alpha = b), Color(150, 170, 220).copy(alpha = b)), sy, sy + sh), Offset(sx, sy), Size(sw, sh))
    clipRect(sx, sy, sx + sw, sy + sh) {
        val hill = Path().apply {
            moveTo(sx, sy + sh * 0.75f)
            var x = 0f
            while (x <= sw) { lineTo(sx + x, sy + sh * (0.72f + 0.06f * sin(x / u * 0.05f))); x += 6 * u }
            lineTo(sx + sw, sy + sh); lineTo(sx, sy + sh); close()
        }
        val ink = Color(30, 35, 60).copy(alpha = 0.85f)
        drawPath(hill, ink)
        val bx = sx + ((t * 14 * u) % (sw + 30 * u)) - 15 * u
        val by = sy + sh * 0.66f - abs(sin(t * 4)) * 6 * u
        drawOval(ink, Offset(bx - 9 * u, by - 5 * u), Size(18 * u, 10 * u))
        drawRect(ink, Offset(bx + 6 * u, by - 9 * u), Size(3 * u, 7 * u))
    }
    drawRect(col(0xFF090A14), Offset(0f, h * 0.8f), Size(w, h * 0.2f))
    for (r in 0 until 2) for (i in 0 until 9) {
        val x = (i + 0.5f + r * 0.5f) * w / 9; val y = h * (0.86f + r * 0.07f); val rr = (7 + r) * u
        drawArc(col(0xFF02030A), 180f, 180f, true, Offset(x - rr, y - rr), Size(rr * 2, rr * 2))
        drawRect(col(0xFF02030A), Offset(x - rr - u, y), Size(rr * 2 + 2 * u, 10 * u))
    }
}

private fun DrawScope.garden(w: Float, h: Float, t: Float, u: Float) {
    vgrad(w, h, 0f to col(0xFFE3EFE1), 0.5f to col(0xFF9CC2A2), 1f to col(0xFF5C8A6C))
    for (k in 0 until 3) drawPath(poly(w * (0.05f + k * 0.15f), 0f, w * (0.2f + k * 0.15f), 0f, w * (0.7f + k * 0.15f), h, w * (0.45f + k * 0.15f), h), Color(255, 255, 240).copy(alpha = 0.12f))
    for (i in 0 until 8) {
        val x = rnd(i) * w; val y = h * (0.45f + rnd(i + 5) * 0.25f); val r = w * (0.1f + rnd(i + 9) * 0.1f)
        drawRect(col(0xFF3A5A44), Offset(x - 2 * u, y), Size(4 * u, h - y))
        val leaf = Color(47, 90, 66).copy(alpha = 0.45f + rnd(i + 2) * 0.35f)
        drawCircle(leaf, r, Offset(x, y)); drawCircle(leaf, r * 0.7f, Offset(x + r * 0.5f, y + r * 0.2f))
    }
    for (k in 0 until 6) {
        val y = h * (0.25f + k * 0.14f)
        val span = w * 2
        val raw = t * 7 * u * (if (k % 2 == 1) 1 else -1) + k * 90 * u
        val x = ((raw % span) + span) % span - w * 0.5f
        drawOval(Color.White.copy(alpha = 0.2f), Offset(x - w * 0.7f, y - 14 * u), Size(w * 1.4f, 28 * u))
    }
}

/** The bricked-up portal of a day without an alarm. */
fun DrawScope.stones(w: Float, h: Float) {
    val u = w / 220f
    drawRect(col(0xFF2B2833), Offset.Zero, Size(w, h))
    var y = 0f; var row = 0
    while (y < h) {
        var x = if (row % 2 == 1) -18 * u else -4 * u
        var i = 0
        while (x < w) {
            val bw = (30 + rnd(row * 31 + i) * 16) * u
            val s = rnd(row * 7 + i * 3)
            drawRect(Color((70 + s * 20).toInt(), (66 + s * 18).toInt(), (78 + s * 16).toInt()), Offset(x + u, y + u), Size(bw - 2 * u, 16 * u))
            if (rnd(row * 13 + i * 5) > 0.55f) for (m in 0 until 6)
                drawRect(Color(110, 150, 80).copy(alpha = 0.8f), Offset(x + rnd(row + i + m * 9) * bw, y + (12 + rnd(m + i) * 5) * u), Size(2.5f * u, 2.5f * u))
            x += bw; i++
        }
        y += 18 * u; row++
    }
}

/** The swirling dark of a closed portal, tinted by the world waiting behind it. */
fun DrawScope.void(w: Float, h: Float, t: Float, accent: Long, glow: Float = 1f) {
    val u = w / 220f
    drawRect(col(0xFF08070F), Offset.Zero, Size(w, h))
    val cx = w / 2; val cy = h / 2; val m = kotlin.math.hypot(w, h) / 2
    val a = Color(accent)
    for (i in 0 until 28) {
        val r = 6 * u + rnd(i + 4) * m
        val start = t * (0.25f + rnd(i + 1) * 0.5f) * (if (i % 2 == 1) 1 else -1) + i
        val sweep = 1.2f + rnd(i) * 1.5f
        drawArc(a.copy(alpha = ((0.04f + 0.07f * rnd(i)) * glow).coerceAtMost(1f)), Math.toDegrees(start.toDouble()).toFloat(), Math.toDegrees(sweep.toDouble()).toFloat(), false,
            Offset(cx - r, cy - r), Size(r * 2, r * 2), style = Stroke(1.5f * u))
    }
}

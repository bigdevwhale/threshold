package app.threshold.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.StrokeCap
import app.threshold.domain.Frame
import app.threshold.domain.World
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Where the arch stands inside a canvas. */
data class ArchBox(val cx: Float, val top: Float, val aw: Float, val ah: Float) {
    val center get() = Offset(cx, top + ah / 2)
}

fun archPath(cx: Float, top: Float, w: Float, h: Float) = Path().apply {
    val x0 = cx - w / 2; val r = w / 2
    moveTo(x0, top + h)
    lineTo(x0, top + r)
    arcTo(Rect(cx - r, top, cx + r, top + 2 * r), 180f, 180f, false)
    lineTo(x0 + w, top + h)
    close()
}

/**
 * The arch and the portal in it. [world] null is a day off: the arch is bricked up. [open] 0..1 is how far the
 * portal has opened onto the world; [ring] is the circle being traced to hold it open.
 */
fun DrawScope.portal(b: ArchBox, t: Float, world: World?, open: Float, frame: Frame, ring: Float? = null, glow: Float = 1f, floor: Boolean = true) {
    val (cx, top, aw, ah) = b
    val u = aw / 220f
    val accent = Color(world?.accent ?: 0xFF96969F)
    if (floor && open > 0 && world != null) {
        drawRect(Brush.radialGradient(listOf(accent.copy(alpha = 0.35f * open), Color.Transparent), Offset(cx, top + ah), aw), Offset(cx - aw, top + ah - aw * 0.5f), Size(aw * 2, aw))
    }
    // The frame: stones round the arch, joints between them.
    val fw = maxOf(8f * u, aw * 0.07f)
    drawPath(archPath(cx, top - fw, aw + fw * 2, ah + fw), Brush.horizontalGradient(listOf(Color(frame.a), Color(frame.b), Color(frame.a)), cx - aw / 2 - fw, cx + aw / 2 + fw))
    val joint = Color(frame.joint)
    val acy = top + aw / 2
    for (k in 1 until 9) {
        val a = Math.PI + Math.PI * k / 9
        drawLine(joint, Offset(cx + cos(a).toFloat() * aw / 2, acy + sin(a).toFloat() * aw / 2), Offset(cx + cos(a).toFloat() * (aw / 2 + fw), acy + sin(a).toFloat() * (aw / 2 + fw)), 1.3f * u)
    }
    var y = acy + fw * 2
    while (y < top + ah - 4 * u) {
        drawLine(joint, Offset(cx - aw / 2 - fw, y), Offset(cx - aw / 2, y), 1.3f * u)
        drawLine(joint, Offset(cx + aw / 2, y), Offset(cx + aw / 2 + fw, y), 1.3f * u)
        y += fw * 2.2f
    }
    if (frame.leaves) for (k in 0 until 26) {
        val a = Math.PI + Math.PI * k / 25
        val p = Offset(cx + cos(a).toFloat() * (aw / 2 + fw / 2), acy + sin(a).toFloat() * (aw / 2 + fw / 2))
        rotate(Math.toDegrees(a + 0.8).toFloat(), p) { drawOval(Color(0xFF8FC47A), Offset(p.x - 3 * u, p.y - 1.6f * u), Size(6 * u, 3.2f * u)) }
    }
    val inner = archPath(cx, top, aw, ah)
    clipPath(inner) {
        translate(cx - aw / 2, top) {
            if (world == null) stones(aw, ah) else void(aw, ah, t, world.accent, glow)
        }
        if (world != null && open > 0) {
            val r = (hypot(aw / 2, ah / 2) + 6 * u) * open
            val c = Offset(cx, top + ah / 2)
            clipPath(Path().apply { addOval(Rect(c, r)) }) {
                translate(cx - aw / 2, top) { world(world, aw, ah, t) }
            }
            if (open < 0.99f) {
                drawCircle(accent.copy(alpha = 0.12f), r, c, style = Stroke(10 * u))
                drawCircle(accent.copy(alpha = 0.3f), r, c, style = Stroke(5 * u))
                drawCircle(accent.copy(alpha = 0.85f), r, c, style = Stroke(2 * u))
                for (i in 0 until 80) {
                    val a = i / 80f * Math.PI.toFloat() * 2 + t * 0.7f + rnd(i) * 0.3f
                    val rr = r + (rnd(i + 3) - 0.5f) * 14 * u + sin(t * 4 + i) * 3 * u
                    drawRect(accent.copy(alpha = 0.35f + 0.55f * rnd(i + 8)), Offset(cx + cos(a) * rr, top + ah / 2 + sin(a) * rr), Size(2 * u, 2 * u))
                }
            }
        }
        drawRect(Brush.radialGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.45f), center = Offset(cx, top + ah / 2), radius = maxOf(aw, ah) * 0.7f),
            Offset(cx - aw, top), Size(aw * 2, ah))
    }
    if (open >= 0.99f && world != null) {
        drawPath(inner, accent.copy(alpha = 0.2f), style = Stroke(9 * u))
        drawPath(inner, accent.copy(alpha = 0.9f), style = Stroke(2.5f * u))
    } else drawPath(inner, Color.Black.copy(alpha = 0.55f), style = Stroke(2.5f * u))
    if (ring != null) {
        val rr = aw / 2 + fw + 22 * u
        val c = Offset(cx, top + ah / 2)
        val gold = Color(0xFFE8C27A)
        drawCircle(gold.copy(alpha = 0.16f), rr, c, style = Stroke(3 * u))
        if (ring > 0) {
            drawArc(gold.copy(alpha = 0.25f), -90f, 360f * ring, false, Offset(c.x - rr, c.y - rr), Size(rr * 2, rr * 2), style = Stroke(9 * u, cap = StrokeCap.Round))
            drawArc(gold, -90f, 360f * ring, false, Offset(c.x - rr, c.y - rr), Size(rr * 2, rr * 2), style = Stroke(3 * u, cap = StrokeCap.Round))
        }
        val a = -Math.PI / 2 + Math.PI * 2 * ring + if (ring < 1) sin(t * 3) * 0.05 else 0.0
        drawCircle(Color(0xFFF6DCA0), 7 * u, Offset(c.x + cos(a).toFloat() * rr, c.y + sin(a).toFloat() * rr))
    }
}

/** A portal in a box, fitted like the prototype: an arch half as wide as tall. */
@Composable
fun PortalView(world: World?, open: Float, frame: Frame, modifier: Modifier = Modifier, glow: Float = 1f, widthFrac: Float = 0.62f, still: Boolean = false) {
    val time by rememberTime()
    Canvas(modifier) {
        val aw = minOf(size.width * widthFrac, size.height * 0.62f)
        val ah = minOf(size.height * 0.9f, aw * 1.7f)
        portal(ArchBox(size.width / 2, (size.height - ah) / 2 + aw * 0.07f, aw, ah), if (still) 4f else time, world, open, frame, glow = glow, floor = !still)
    }
}

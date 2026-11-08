package app.threshold.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import app.threshold.domain.Shape

private val cache = HashMap<String, Path>()
private fun p(d: String): Path = cache.getOrPut(d) { PathParser().parsePathString(d).toPath() }

private val shapes = mapOf(
    Shape.FEATHER to "M20 3C11 5 5 12 4 21h1.6c.7-4.6 3.4-8.7 7.4-11-2.6 2.8-4.3 5.8-5 9 6.4-.8 11-7.4 12-16z",
    Shape.DISC to "M12 3a9 9 0 0 1 9 9H12z M12 13.5a1.5 1.5 0 1 1 0 .01z",
    Shape.STONE to "M5 16l2.5-8 7.5-3 5 5.5-1.5 8-9 1.5z",
    Shape.BULB to "M12 3a6 6 0 0 0-3.2 11.1V17h6.4v-2.9A6 6 0 0 0 12 3zM9 18.5h6V21H9z",
    Shape.TICKET to "M3 7h18v3a2 2 0 0 0 0 4v3H3v-3a2 2 0 0 0 0-4z",
    Shape.LEAF to "M5 19C5 9 11 4 20 4c0 9-5 15-15 15z",
    Shape.KEY to "M7.5 15.5a4.5 4.5 0 1 1 4.2-6H21v3h-2v2.5h-3V12.5h-4.3a4.5 4.5 0 0 1-4.2 3z",
    Shape.STAR to "M12 3l2.6 5.6 6.1.7-4.5 4.2 1.2 6L12 16.6l-5.4 2.9 1.2-6-4.5-4.2 6.1-.7z",
    Shape.CRYSTAL to "M12 2l5 6-5 14-5-14z",
    Shape.SHELL to "M12 4c5 0 9 4 9 9 0 3-2 5-4 6H7c-2-1-4-3-4-6 0-5 4-9 9-9z",
    Shape.FLAG to "M6 3h2v18H6zM8 4h11l-3 4 3 4H8z",
    Shape.BOTTLE to "M10 2h4v4l2 3v12H8V9l2-3z",
    Shape.DROP to "M12 3c4 5 6 8 6 11a6 6 0 0 1-12 0c0-3 2-6 6-11z",
    Shape.CIRCLE to "M12 4a8 8 0 1 1 0 16 8 8 0 0 1 0-16z",
)

/** A find, drawn filled in a 24-unit box. */
@Composable
fun FindIcon(shape: Shape, color: Color, modifier: Modifier) {
    Canvas(modifier) { scale(size.width / 24f, size.height / 24f, pivot = androidx.compose.ui.geometry.Offset.Zero) { drawPath(p(shapes.getValue(shape)), color) } }
}

enum class Glyph(val d: String) {
    ARCH("M6 21V10a6 6 0 0 1 12 0v11M3 21h18"),
    WEEK("M3 5h18v16H3zM3 10h18M8 3v4M16 3v4"),
    MAP("M12 9a3 3 0 1 1 0 6 3 3 0 1 1 0-6zM4 4a2 2 0 1 1 0 4 2 2 0 1 1 0-4zM20 5a2 2 0 1 1 0 4 2 2 0 1 1 0-4zM18 18a2 2 0 1 1 0 4 2 2 0 1 1 0-4zM10 10 5.5 7M14.5 10.5 18.5 8M14 14l3 4.5"),
    SHELF("M3 8h18M3 16h18M5 8V5h3v3M14 8V4h4v4M7 16v-4h4v4M15 16v-3h3v3M3 21h18"),
    GEAR("M12 9a3 3 0 1 1 0 6 3 3 0 1 1 0-6zM12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M4.9 19.1 7 17M17 7l2.1-2.1"),
    PLUS("M12 5v14M5 12h14"),
    CLOSE("M6 6l12 12M18 6 6 18"),
    BACK("M15 5l-7 7 7 7"),
    CHEVRON("M9 5l7 7-7 7"),
    UP("M6 15l6-6 6 6"),
    DOWN("M6 9l6 6 6-6"),
    TRASH("M4 7h16M9 7V4h6v3M6 7l1 14h10l1-14"),
    LINK("M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1"),
    APP("M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z"),
    RADIO("M4 9h16v11H4zM8 9l9-5M8 14.5a2 2 0 1 0 .01 0M14 13h3M14 16h3"),
    MOON("M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z"),
    CHECK("M5 12.5l4.5 4.5L19 7"),
    WARN("M12 4l9 16H3zM12 10v4M12 17v.5"),
}

@Composable
fun GlyphIcon(g: Glyph, color: Color, modifier: Modifier, stroke: Float = 1.6f) {
    Canvas(modifier) {
        scale(size.width / 24f, size.height / 24f, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            drawPath(p(g.d), color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

package app.threshold.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.threshold.R

/** The night palette: the keeper's room before dawn. */
object C {
    val bg = Color(0xFF0A0912)
    val deep = Color(0xFF06050B)
    val card = Color(0xFF13111F)
    val card2 = Color(0xFF1A1729)
    val line = Color(0xFF25213A)
    val ink = Color(0xFFECE6F5)
    val dim = Color(0xFF9A93AD)
    val faint = Color(0xFF5E5872)
    val gold = Color(0xFFE8C27A)
    val goldDeep = Color(0xFFD4A858)
    val onGold = Color(0xFF251A06)
    val violet = Color(0xFF8A6CFF)
    val red = Color(0xFFE07A6E)
}

val Serif = FontFamily(
    Font(R.font.cormorant_semibold, FontWeight.SemiBold),
    Font(R.font.cormorant_bold, FontWeight.Bold),
    Font(R.font.cormorant_italic, FontWeight.Normal, FontStyle.Italic),
)
val Sans = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

object T {
    val h1 = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 34.sp, color = C.ink)
    val h2 = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 23.sp, lineHeight = 26.sp, color = C.ink)
    val h3 = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 22.sp, color = C.ink)
    val big = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 38.sp, lineHeight = 38.sp, color = C.gold)
    val clock = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 72.sp, lineHeight = 72.sp, color = C.ink)
    val body = TextStyle(fontFamily = Sans, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val sub = TextStyle(fontFamily = Sans, fontSize = 13.sp, lineHeight = 18.sp, color = C.dim)
    val small = TextStyle(fontFamily = Sans, fontSize = 11.5.sp, lineHeight = 15.sp, color = C.dim)
    val button = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = C.ink)
    val italic = TextStyle(fontFamily = Serif, fontStyle = FontStyle.Italic, fontSize = 18.sp, lineHeight = 22.sp, color = C.dim)
}

@Composable
fun ThresholdTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.gold, onPrimary = C.onGold, background = C.bg, surface = C.card, onSurface = C.ink, onBackground = C.ink,
            surfaceContainerHigh = C.card2, surfaceContainerHighest = C.card2, secondary = C.violet,
        ),
        content = content,
    )
}

package app.threshold.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.threshold.ui.art.Glyph
import app.threshold.ui.art.GlyphIcon
import app.threshold.ui.theme.C
import app.threshold.ui.theme.T
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun Card(modifier: Modifier = Modifier, pad: Dp = 16.dp, color: Color = C.card, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(color).border(1.dp, C.line, RoundedCornerShape(16.dp)).padding(horizontal = pad, vertical = 14.dp),
        content = content,
    )
}

@Composable
fun Btn(text: String, modifier: Modifier = Modifier, gold: Boolean = false, enabled: Boolean = true, compact: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier.clip(shape)
            .then(if (gold) Modifier.background(Brush.verticalGradient(listOf(Color(0xFFF0CF8C), C.goldDeep))) else Modifier.background(C.card2).border(1.dp, C.line, shape))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (compact) 6.dp else 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = if (compact) T.button.copy(fontSize = 14.sp) else T.button, maxLines = 1, color = when { !enabled -> C.faint; gold -> C.onGold; else -> C.ink }, textAlign = TextAlign.Center)
    }
}

/** A button that has to be held: the fill creeps across, and only a full hold counts. */
@Composable
fun HoldButton(text: String, modifier: Modifier = Modifier, millis: Int = 1100, onDone: () -> Unit) {
    var p by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier.fillMaxWidth().clip(shape).background(C.card2).border(1.dp, C.line, shape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    job?.cancel()
                    job = scope.launch {
                        val t0 = System.nanoTime()
                        while (p < 1f) {
                            p = ((System.nanoTime() - t0) / 1e6f / millis).coerceAtMost(1f)
                            kotlinx.coroutines.delay(16)
                        }
                        onDone()
                        p = 0f
                    }
                    waitForUpOrCancellation()
                    if (p < 1f) { job?.cancel(); p = 0f }
                }
            },
    ) {
        Box(Modifier.fillMaxWidth(p).height(50.dp).background(C.gold.copy(alpha = 0.22f)))
        Text(text, style = T.button, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center).padding(horizontal = 12.dp))
    }
}

@Composable
fun Toggle(on: Boolean, onChange: (Boolean) -> Unit) {
    Box(
        Modifier.size(44.dp, 26.dp).clip(CircleShape).background(if (on) Color(0xFF6B55C9) else Color(0xFF2A2640)).clickable { onChange(!on) },
    ) {
        Box(Modifier.offset(x = if (on) 21.dp else 3.dp, y = 3.dp).size(20.dp).clip(CircleShape).background(if (on) Color.White else Color(0xFF8B85A0)))
    }
}

@Composable
fun ToggleRow(title: String, sub: String? = null, on: Boolean, divider: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!on) }.padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = T.body)
            if (sub != null) Text(sub, style = T.sub)
        }
        Spacer(Modifier.width(12.dp))
        Toggle(on, onChange)
    }
    if (divider) Line()
}

@Composable
fun NavRow(title: String, value: String? = null, sub: String? = null, divider: Boolean = true, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = T.body)
            if (sub != null) Text(sub, style = T.sub)
        }
        if (value != null) Text(value, style = T.sub.copy(color = C.gold), modifier = Modifier.padding(start = 8.dp))
        GlyphIcon(Glyph.CHEVRON, C.faint, Modifier.padding(start = 6.dp).size(16.dp))
    }
    if (divider) Line()
}

@Composable
fun Line() = Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))

@Composable
fun Chip(text: String, selected: Boolean, modifier: Modifier = Modifier, dot: Color? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier.clip(shape).background(if (selected) C.gold.copy(alpha = 0.16f) else C.card2)
            .border(1.dp, if (selected) C.gold else C.line, shape).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) { Box(Modifier.size(8.dp).clip(CircleShape).background(dot)); Spacer(Modifier.width(7.dp)) }
        Text(text, style = T.sub.copy(color = if (selected) C.gold else C.ink))
    }
}

/** Chips in a choice row: one is picked. */
@Composable
fun <V> Choice(options: List<Pair<V, String>>, value: V, onPick: (V) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (v, label) -> Chip(label, v == value, Modifier.weight(1f)) { onPick(v) } }
    }
}

@Composable
fun Label(text: String, modifier: Modifier = Modifier) = Text(text.uppercase(), style = T.small.copy(color = C.faint), modifier = modifier.padding(top = 18.dp, bottom = 8.dp))

/** A page over the tabs: back arrow, title, scrolling body. */
@Composable
fun Page(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(C.bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) { GlyphIcon(Glyph.BACK, C.ink, Modifier.size(22.dp)) }
            Text(title, style = T.h2, modifier = Modifier.weight(1f).padding(start = 4.dp))
            actions()
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp), content = content)
    }
}

/** A sheet from below over a dimmed screen. */
@Composable
fun Sheet(visible: Boolean, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    if (visible) BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable(remember { MutableInteractionSource() }, null, onClick = onClose))
        }
        AnimatedVisibility(visible, modifier = Modifier.align(Alignment.BottomCenter), enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(Color(0xFF15122A))
                    .border(1.dp, Color(0xFF352F52), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(remember { MutableInteractionSource() }, null) {}
                    .navigationBarsPadding().padding(20.dp).verticalScroll(rememberScrollState()),
                content = content,
            )
        }
    }
}

@Composable
fun BoxScope.Toast(text: String?, onGone: () -> Unit) {
    LaunchedEffect(text) { if (text != null) { kotlinx.coroutines.delay(2600); onGone() } }
    AnimatedVisibility(text != null, Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp), enter = fadeIn(), exit = fadeOut()) {
        Text(text.orEmpty(), style = T.sub.copy(color = C.ink), modifier = Modifier.widthIn(max = 340.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF2A2546)).padding(horizontal = 16.dp, vertical = 12.dp))
    }
}

@Composable
fun Meter(p: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color(0xFF221E33))) {
        Box(Modifier.fillMaxWidth(p.coerceIn(0f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(C.violet, C.gold))))
    }
}

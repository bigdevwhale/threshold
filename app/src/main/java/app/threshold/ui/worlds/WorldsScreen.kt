package app.threshold.ui.worlds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.threshold.R
import app.threshold.data.State
import app.threshold.domain.Finds
import app.threshold.domain.Frames
import app.threshold.domain.Journal
import app.threshold.domain.Portal
import app.threshold.domain.Week
import app.threshold.domain.World
import app.threshold.ru
import app.threshold.ui.Card
import app.threshold.ui.Fmt
import app.threshold.ui.Label
import app.threshold.ui.Line
import app.threshold.ui.Nav
import app.threshold.ui.Sheet
import app.threshold.ui.art.ArchBox
import app.threshold.ui.art.FindIcon
import app.threshold.ui.art.portal
import app.threshold.ui.art.rememberTime
import app.threshold.ui.art.world
import app.threshold.ui.theme.C
import app.threshold.ui.theme.Sans
import app.threshold.ui.theme.Serif
import app.threshold.ui.theme.T
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private data class Node(val w: World, val c: Offset, val r: Float)

@Composable
fun WorldsScreen(s: State, nav: Nav, world0: World?) {
    val ctx = LocalContext.current
    val ru = ctx.ru
    var open by remember { mutableStateOf(world0) }
    val time by rememberTime()
    val measurer = rememberTextMeasurer()
    var nodes by remember { mutableStateOf(emptyList<Node>()) }
    val center = Week.next(s, System.currentTimeMillis())?.let { Portal.morning(s, it.day).world }
    val known = World.entries.filter { it != World.WILD || (s.visits[it] ?: 0) > 0 || s.wild }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 14.dp, bottom = 24.dp)) {
            Text(stringResource(R.string.worlds_title), style = T.h1)
            Text(stringResource(R.string.worlds_sub), style = T.sub)
            Canvas(Modifier.fillMaxWidth().height(470.dp).pointerInput(Unit) {
                detectTapGestures { p -> nodes.firstOrNull { hypot(it.c.x - p.x, it.c.y - p.y) < it.r + 12.dp.toPx() }?.let { open = it.w } }
            }) {
                val cx = size.width / 2; val cy = size.height / 2
                val list = known.mapIndexed { i, w ->
                    val a = -Math.PI / 2 + i * 2 * Math.PI / known.size
                    val lv = Portal.level(s.visits[w] ?: 0)
                    val rad = minOf(size.width, size.height) * (if (w == World.WILD) 0.4f else 0.35f)
                    Node(w, Offset(cx + cos(a).toFloat() * rad, cy + sin(a).toFloat() * rad * 1.08f), (if (w == World.WILD) 24 else 26 + lv * 2).dp.toPx())
                }
                nodes = list
                list.forEach { n ->
                    val lv = Portal.level(s.visits[n.w] ?: 0)
                    drawLine(Color(n.w.accent).copy(alpha = 0.12f + lv * 0.08f), Offset(cx, cy), n.c, (1 + lv * 0.5f).dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()), -time * 12.dp.toPx()))
                }
                val aw = 44.dp.toPx()
                portal(ArchBox(cx, cy - 36.dp.toPx(), aw, 66.dp.toPx()), time, center, 0f, Frames.of(s.frame), glow = 2f)
                val me = measurer.measure(ctx.getString(R.string.worlds_you), T.small.copy(fontFamily = Sans, textAlign = TextAlign.Center))
                drawText(me, topLeft = Offset(cx - me.size.width / 2, cy + 34.dp.toPx()))
                list.forEach { n ->
                    val accent = Color(n.w.accent)
                    clipPath(Path().apply { addOval(Rect(n.c, n.r)) }) {
                        translate(n.c.x - n.r * 1.3f, n.c.y - n.r * 1.6f) { world(n.w, n.r * 2.6f, n.r * 3.2f, time) }
                    }
                    drawCircle(accent.copy(alpha = 0.25f), n.r, n.c, style = Stroke(7.dp.toPx()))
                    drawCircle(accent.copy(alpha = 0.9f), n.r, n.c, style = Stroke(2.dp.toPx()))
                    val visits = s.visits[n.w] ?: 0
                    val name = measurer.measure(n.w.title.of(ru), T.h3.copy(fontFamily = Serif, fontSize = 15.sp, textAlign = TextAlign.Center))
                    drawText(name, topLeft = Offset(n.c.x - name.size.width / 2, n.c.y + n.r + 4.dp.toPx()))
                    val sub = if (n.w == World.WILD) ctx.getString(R.string.worlds_wild_sub) else ctx.getString(R.string.worlds_level, Portal.level(visits), visits)
                    val st = measurer.measure(sub, T.small.copy(fontFamily = Sans, fontSize = 10.5.sp, textAlign = TextAlign.Center))
                    drawText(st, topLeft = Offset(n.c.x - st.size.width / 2, n.c.y + n.r + 4.dp.toPx() + name.size.height))
                }
            }
            JournalCard(s)
        }
        Sheet(open != null, { open = null }) { open?.let { WorldSheet(s, it) } }
    }
}

@Composable
private fun WorldSheet(s: State, w: World) {
    val ctx = LocalContext.current
    val ru = ctx.ru
    val visits = s.visits[w] ?: 0
    val lv = Portal.level(visits)
    Text(w.title.of(ru), style = T.h2.copy(color = Color(w.accent)))
    val days = s.week.withIndex().filter { it.value.on && it.value.world == w }.map { Fmt.dow(ctx, it.index) }
    Text(if (days.isEmpty()) w.hint.of(ru) else days.joinToString(", "), style = T.sub)
    Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..5).forEach { Box(Modifier.width(26.dp).height(5.dp).clip(CircleShape).background(if (it <= lv) C.gold else Color(0xFF2A2640))) }
    }
    Text(w.about.of(ru), style = T.body)
    val left = Portal.toNext(visits)
    Text(if (left != null) stringResource(R.string.worlds_next, visits, left) else stringResource(R.string.worlds_top, visits), style = T.sub, modifier = Modifier.padding(top = 8.dp))
    if (w.residents.isNotEmpty()) {
        Label(stringResource(R.string.worlds_residents))
        w.residents.forEach { r ->
            if (lv >= r.level) {
                Text(r.name.of(ru), style = T.h3)
                Text(r.line.of(ru), style = T.sub, modifier = Modifier.padding(bottom = 8.dp))
            } else Text(stringResource(R.string.worlds_resident_locked, r.level), style = T.sub.copy(color = C.faint), modifier = Modifier.padding(bottom = 8.dp))
        }
    }
    Label(stringResource(R.string.tab_finds))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Finds.of(w).forEach { f ->
            val own = (s.finds[f.key] ?: 0) > 0
            FindIcon(f.shape, when { !own -> Color(0xFF26223A); f.rare -> C.gold; else -> Color(w.accent) }, Modifier.size(30.dp))
        }
    }
}

@Composable
private fun JournalCard(s: State) {
    val ctx = LocalContext.current
    val ru = ctx.ru
    val sum = Journal.sum(s)
    Label(stringResource(R.string.journal))
    Card {
        if (sum.wakes == 0) {
            Text(stringResource(R.string.journal_empty), style = T.sub)
            return@Card
        }
        Row {
            Stat(sum.wakes.toString(), stringResource(R.string.journal_wakes), Modifier.weight(1f))
            Stat("${sum.firstRing}%", stringResource(R.string.journal_first), Modifier.weight(1f))
            Stat(if (sum.avgSecs < 120) stringResource(R.string.sec_n, sum.avgSecs) else stringResource(R.string.min_n, (sum.avgSecs / 60).toString()), stringResource(R.string.journal_step), Modifier.weight(1f))
        }
        if (sum.moodBy.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.journal_mood), style = T.sub, modifier = Modifier.padding(bottom = 8.dp))
            sum.moodBy.entries.sortedByDescending { it.value }.forEach { (w, m) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                    Text(w.title.of(ru), style = T.small.copy(color = C.ink), modifier = Modifier.width(130.dp))
                    Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF221E33))) {
                        Box(Modifier.fillMaxWidth((m / 2).toFloat().coerceIn(0.04f, 1f)).height(8.dp).background(Color(w.accent)))
                    }
                }
            }
            sum.best?.let { Text(stringResource(R.string.journal_best, it.title.of(ru)), style = T.italic.copy(color = C.ink), modifier = Modifier.padding(top = 12.dp)) }
            sum.worst?.let { Text(stringResource(R.string.journal_worst, it.title.of(ru)), style = T.italic, modifier = Modifier.padding(top = 6.dp)) }
        } else Text(stringResource(R.string.journal_rate), style = T.sub, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(12.dp))
        Line()
        s.log.take(7).forEach { w ->
            Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Color(w.world.accent)))
                Text(Fmt.date(ctx, Week.epochDay(w.at)) + " · " + Fmt.hm(w.at), style = T.small.copy(color = C.ink), modifier = Modifier.padding(start = 10.dp).width(120.dp))
                Text(w.world.title.of(ru), style = T.small, modifier = Modifier.weight(1f))
                val mood = when (w.mood) { 2 -> stringResource(R.string.mood_fresh); 1 -> stringResource(R.string.mood_so); 0 -> stringResource(R.string.mood_hard); else -> "" }
                Text(if (w.snoozes > 0) "×${w.snoozes} " + mood else mood, style = T.small)
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = T.big.copy(fontSize = 28.sp))
        Text(label, style = T.small)
    }
}

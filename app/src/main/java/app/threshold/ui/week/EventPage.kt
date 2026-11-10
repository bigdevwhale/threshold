package app.threshold.ui.week

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.threshold.R
import app.threshold.alarm.Launch
import app.threshold.container
import app.threshold.data.Event
import app.threshold.data.Kind
import app.threshold.data.Step
import app.threshold.ui.Btn
import app.threshold.ui.Card
import app.threshold.ui.Fmt
import app.threshold.ui.Label
import app.threshold.ui.Nav
import app.threshold.ui.Page
import app.threshold.ui.ToggleRow
import app.threshold.ui.art.GlyphIcon
import app.threshold.ui.theme.C
import app.threshold.ui.theme.T

/** An event in the list of a day: its time and what it opens. */
@Composable
fun EventCard(e: Event, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Card(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Fmt.hm(e.h, e.m), style = T.h3.copy(color = if (e.on) C.ink else C.dim))
            GlyphIcon(Fmt.glyph(e.step), if (e.on) C.gold else C.dim, Modifier.padding(start = 14.dp).size(20.dp))
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(e.step.label.ifBlank { Fmt.kind(ctx, e.step) }, style = T.body.copy(color = if (e.on) C.ink else C.dim), maxLines = 1)
                Text(if (e.on) days(ctx, e.days) else stringResource(R.string.ev_paused), style = T.small, maxLines = 1)
            }
        }
    }
}

/** «Every day», «Weekdays», or the days themselves: «Mon, Wed, Fri». */
fun days(ctx: android.content.Context, d: Set<Int>): String = when (d) {
    (0..6).toSet() -> ctx.getString(R.string.ev_every_day)
    (0..4).toSet() -> ctx.getString(R.string.ev_weekdays)
    setOf(5, 6) -> ctx.getString(R.string.ev_weekend)
    else -> d.sorted().joinToString(", ") { Fmt.dowShort(ctx, it) }
}

/** One event: when, what it opens, on which days. A new one has id 0 and no step yet. */
@Composable
fun EventPage(e0: Event, onClose: () -> Unit, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val isNew = e0.id == 0L
    var h by remember { mutableStateOf(e0.h) }
    var m by remember { mutableStateOf(e0.m) }
    var step by remember { mutableStateOf(e0.step.takeIf { it.label.isNotBlank() || it.pkg.isNotBlank() || it.url.isNotBlank() }) }
    var days by remember { mutableStateOf(e0.days) }
    var on by remember { mutableStateOf(e0.on) }
    var time by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf<Adding?>(null) }
    val pickFile = rememberFilePicker { step = it }

    fun save() {
        val st = step ?: return
        val e = e0.copy(id = if (isNew) System.currentTimeMillis() else e0.id, days = days, h = h, m = m, step = st, on = on)
        c.write { store.update { s -> s.copy(events = if (isNew) s.events + e else s.events.map { if (it.id == e.id) e else it }) } }
        nav.toast(ctx.getString(R.string.ev_saved, Fmt.hm(h, m)))
        onClose()
    }

    Page(stringResource(if (isNew) R.string.ev_new else R.string.ev_one), onClose) {
        Card(Modifier.clickable { time = true }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.ev_time), style = T.sub)
                    Text(Fmt.hm(h, m), style = T.big)
                }
                Text(stringResource(R.string.day_time_change), style = T.sub.copy(color = C.gold))
            }
        }

        Label(stringResource(R.string.ev_what))
        val chosen = step
        if (chosen == null) Text(stringResource(R.string.ev_what_none), style = T.sub)
        else {
            val st = chosen
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlyphIcon(Fmt.glyph(st), C.gold, Modifier.size(22.dp))
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(st.label.ifBlank { Fmt.kind(ctx, st) }, style = T.body, maxLines = 2)
                        Text(Fmt.kind(ctx, st) + (st.url.takeIf { it.isNotBlank() && st.kind != Kind.FILE }?.let { " · " + it.removePrefix("https://").removePrefix("http://").take(40) } ?: ""), style = T.small, maxLines = 1)
                    }
                }
            }
            Text(stringResource(R.string.ev_change), style = T.sub, modifier = Modifier.padding(top = 10.dp))
        }
        AddRow({ adding = it }, pickFile)
        step?.takeIf { it.kind != Kind.RADIO }?.let { st ->
            Spacer(Modifier.height(8.dp))
            Btn(stringResource(R.string.day_try), Modifier.fillMaxWidth()) { if (!Launch.open(ctx, st)) nav.toast(ctx.getString(R.string.toast_cant_open)) }
        }

        Label(stringResource(R.string.ev_days))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (d in 0..6) {
                val sel = d in days
                val shape = RoundedCornerShape(12.dp)
                Box(
                    Modifier.weight(1f).height(42.dp).clip(shape).background(if (sel) C.gold.copy(alpha = 0.16f) else C.card2)
                        .border(1.dp, if (sel) C.gold else C.line, shape).clickable { days = if (sel) days - d else days + d },
                    contentAlignment = Alignment.Center,
                ) { Text(Fmt.dowShort(ctx, d), style = T.sub.copy(color = if (sel) C.gold else C.ink), maxLines = 1) }
            }
        }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.ev_every_day), style = T.sub.copy(color = C.gold), modifier = Modifier.clickable { days = (0..6).toSet() })
            Text(stringResource(R.string.ev_weekdays), style = T.sub.copy(color = C.gold), modifier = Modifier.clickable { days = (0..4).toSet() })
            Text(stringResource(R.string.ev_weekend), style = T.sub.copy(color = C.gold), modifier = Modifier.clickable { days = setOf(5, 6) })
        }

        if (!isNew) {
            Spacer(Modifier.height(14.dp))
            Card { ToggleRow(stringResource(R.string.ev_on), on = on, divider = false) { on = it } }
        }
        Text(stringResource(R.string.ev_how), style = T.small, modifier = Modifier.padding(top = 14.dp))
        Spacer(Modifier.height(14.dp))
        Btn(stringResource(R.string.pick_save), Modifier.fillMaxWidth(), gold = true, enabled = step != null && days.isNotEmpty()) { save() }
        if (!isNew) {
            Spacer(Modifier.height(10.dp))
            Btn(stringResource(R.string.ev_delete), Modifier.fillMaxWidth()) {
                c.write { store.update { s -> s.copy(events = s.events.filter { it.id != e0.id }) } }
                onClose()
            }
        }
    }
    if (time) TimeDialog(h, m, { time = false }) { hh, mm -> h = hh; m = mm; time = false }
    when (adding) {
        Adding.APP -> AppPicker({ adding = null }) { step = it; adding = null }
        Adding.LINK -> LinkPicker(step?.takeIf { it.kind == Kind.LINK }, { adding = null }) { step = it; adding = null }
        Adding.RADIO -> RadioPicker({ adding = null }) { step = it; adding = null }
        null -> {}
    }
}

/** A step with nothing chosen yet: the placeholder a new event starts from. */
internal val noStep = Step(Kind.APP, "")

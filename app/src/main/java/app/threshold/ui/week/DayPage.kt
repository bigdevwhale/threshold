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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.threshold.R
import app.threshold.alarm.Launch
import app.threshold.container
import app.threshold.data.Day
import app.threshold.data.Event
import app.threshold.data.Kind
import app.threshold.data.State
import app.threshold.data.Step
import app.threshold.domain.Frames
import app.threshold.domain.Portal
import app.threshold.domain.Week
import app.threshold.domain.World
import app.threshold.ru
import app.threshold.ui.Btn
import app.threshold.ui.Card
import app.threshold.ui.Fmt
import app.threshold.ui.Label
import app.threshold.ui.Nav
import app.threshold.ui.Page
import app.threshold.ui.ToggleRow
import app.threshold.ui.art.Glyph
import app.threshold.ui.art.GlyphIcon
import app.threshold.ui.art.PortalView
import app.threshold.ui.theme.C
import app.threshold.ui.theme.T

/** What is being added to the morning. */
enum class Adding { APP, LINK, RADIO }

/** The add buttons: an app, a link, a radio station, a file from the phone. */
@Composable
fun AddRow(onAdd: (Adding) -> Unit, onFile: () -> Unit) {
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(stringResource(R.string.k_app), Modifier.weight(1f), compact = true) { onAdd(Adding.APP) }
            Btn(stringResource(R.string.k_link), Modifier.weight(1f), compact = true) { onAdd(Adding.LINK) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(stringResource(R.string.k_radio), Modifier.weight(1f), compact = true) { onAdd(Adding.RADIO) }
            Btn(stringResource(R.string.k_file), Modifier.weight(1f), compact = true, onClick = onFile)
        }
    }
}

@Composable
fun DayPage(s: State, i: Int, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val ru = ctx.ru
    val d = s.week[i]
    var time by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf<Adding?>(null) }
    var editing by remember { mutableStateOf<Int?>(null) }
    var event by remember { mutableStateOf<Event?>(null) }
    fun set(change: (Day) -> Day) = c.write { setDay(i, change) }
    fun add(step: Step) = set { day -> Portal.place(day, step) }
    // A file either joins the morning or takes the place of the step being edited.
    var fileFor by remember { mutableStateOf<Int?>(null) }
    val pickFile = rememberFilePicker { st ->
        val k = fileFor
        if (k == null) add(st) else set { it.copy(steps = it.steps.mapIndexed { j, x -> if (j == k) st.copy(minutes = x.minutes) else x }) }
        fileFor = null
    }

    Page(Fmt.dow(ctx, i), { nav.open(null) }) {
        PortalView(if (d.on) d.world else null, if (d.on) 1f else 0f, Frames.of(s.frame), Modifier.fillMaxWidth().height(200.dp), widthFrac = 0.4f)
        Card {
            ToggleRow(stringResource(R.string.day_on), on = d.on, divider = false) { v -> set { it.copy(on = v) } }
        }
        if (d.on) {
            Spacer(Modifier.height(10.dp))
            Card(Modifier.clickable { time = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.day_time), style = T.sub)
                        Text(Fmt.hm(d.h, d.m), style = T.big)
                    }
                    Text(stringResource(R.string.day_time_change), style = T.sub.copy(color = C.gold))
                }
            }
            if (i < 5) Text(stringResource(R.string.day_time_all), style = T.sub.copy(color = C.gold), modifier = Modifier.padding(top = 8.dp).clickable {
                c.write { store.update { st -> st.copy(week = st.week.mapIndexed { j, x -> if (j < 5) x.copy(h = d.h, m = d.m) else x }) } }
                nav.toast(ctx.getString(R.string.toast_weekdays, Fmt.hm(d.h, d.m)))
            })

            Spacer(Modifier.height(10.dp))
            Card {
                ToggleRow(stringResource(R.string.day_auto), stringResource(R.string.day_auto_b), d.auto, divider = false) { v -> set { it.copy(auto = v) } }
            }

            Label(stringResource(R.string.day_steps))
            if (d.steps.isEmpty()) Text(stringResource(R.string.day_steps_none), style = T.sub, modifier = Modifier.padding(bottom = 10.dp))
            d.steps.forEachIndexed { k, st ->
                StepCard(st, k, d.steps.size,
                    onMinutes = { m -> set { it.copy(steps = it.steps.mapIndexed { j, x -> if (j == k) x.copy(minutes = m) else x }) } },
                    onUp = { set { it.copy(steps = it.steps.toMutableList().apply { add(k - 1, removeAt(k)) }) } },
                    onDelete = { set { it.copy(steps = it.steps.filterIndexed { j, _ -> j != k }) } },
                    onEdit = { if (st.kind == Kind.FILE) { fileFor = k; pickFile() } else editing = k },
                )
                if (k < d.steps.size - 1) Text(stringResource(R.string.day_then, st.minutes), style = T.small.copy(color = C.gold), modifier = Modifier.padding(start = 14.dp, top = 4.dp, bottom = 6.dp))
            }
            Text(stringResource(R.string.day_add), style = T.sub, modifier = Modifier.padding(top = 10.dp))
            AddRow({ adding = it }) { fileFor = null; pickFile() }
            Text(stringResource(R.string.day_share_hint), style = T.small, modifier = Modifier.padding(top = 8.dp))
            d.steps.firstOrNull()?.takeIf { it.kind != Kind.RADIO }?.let { first ->
                Spacer(Modifier.height(10.dp))
                Btn(stringResource(R.string.day_try), Modifier.fillMaxWidth()) {
                    if (!Launch.open(ctx, first)) nav.toast(ctx.getString(R.string.toast_cant_open))
                }
            }

            Label(stringResource(R.string.day_world))
            Text(stringResource(R.string.day_world_b), style = T.sub, modifier = Modifier.padding(bottom = 10.dp))
            World.chosen.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    row.forEach { w -> WorldChip(w, w == d.world, ru, Modifier.weight(1f)) { set { it.copy(world = w, picked = true) } } }
                }
            }
        }

        // The rest of the day: the evening book, the workout, the film. They come whether the morning rings or not.
        Label(stringResource(R.string.ev_title))
        Text(stringResource(R.string.ev_b), style = T.sub, modifier = Modifier.padding(bottom = 10.dp))
        Week.events(s, i).forEach { e ->
            EventCard(e) { event = e }
            Spacer(Modifier.height(8.dp))
        }
        Btn("+ " + stringResource(R.string.ev_add), Modifier.fillMaxWidth(), gold = s.events.none { i in it.days }) {
            val next = (java.time.LocalTime.now().hour + 1) % 24
            event = Event(0, setOf(i), next, 0, noStep)
        }
    }
    if (time) TimeDialog(d.h, d.m, { time = false }) { h, m -> set { it.copy(h = h, m = m) }; time = false }
    when (adding) {
        Adding.APP -> AppPicker({ adding = null }) { add(it); adding = null }
        Adding.LINK -> LinkPicker(null, { adding = null }) { add(it); adding = null }
        Adding.RADIO -> RadioPicker({ adding = null }) { add(it); adding = null }
        null -> {}
    }
    editing?.let { k ->
        val st = d.steps.getOrNull(k)
        if (st == null) editing = null
        else when (st.kind) {
            Kind.LINK -> LinkPicker(st, { editing = null }) { new -> set { it.copy(steps = it.steps.mapIndexed { j, x -> if (j == k) new.copy(minutes = x.minutes) else x }) }; editing = null }
            Kind.RADIO -> RadioPicker({ editing = null }) { new -> set { it.copy(steps = it.steps.mapIndexed { j, x -> if (j == k) new.copy(minutes = x.minutes) else x }) }; editing = null }
            Kind.APP -> AppPicker({ editing = null }) { new -> set { it.copy(steps = it.steps.mapIndexed { j, x -> if (j == k) new.copy(minutes = x.minutes) else x }) }; editing = null }
            Kind.FILE -> editing = null
        }
    }
    event?.let { e -> EventPage(e, { event = null }, nav) }
}

@Composable
private fun StepCard(st: Step, k: Int, n: Int, onMinutes: (Int) -> Unit, onUp: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit) {
    val ctx = LocalContext.current
    Card(Modifier.clickable(onClick = onEdit)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphIcon(Fmt.glyph(st), C.gold, Modifier.size(22.dp))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(st.label.ifBlank { Fmt.kind(ctx, st) }, style = T.body, maxLines = 2)
                Text(Fmt.kind(ctx, st) + (st.url.takeIf { it.isNotBlank() }?.let { " · " + it.removePrefix("https://").removePrefix("http://").take(40) } ?: ""), style = T.small, maxLines = 1)
            }
            if (k > 0) IconBtn(Glyph.UP, onUp)
            IconBtn(Glyph.TRASH, onDelete)
        }
        if (k < n - 1) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                Text(stringResource(R.string.day_lasts), style = T.sub, modifier = Modifier.weight(1f))
                listOf(10, 15, 20, 30, 45).forEach { m ->
                    val on = st.minutes == m
                    Text("$m", style = T.sub.copy(color = if (on) C.onGold else C.ink), modifier = Modifier.padding(start = 6.dp).clip(RoundedCornerShape(8.dp))
                        .background(if (on) C.gold else C.card2).clickable { onMinutes(m) }.padding(horizontal = 9.dp, vertical = 5.dp))
                }
            }
        }
    }
}

@Composable
internal fun IconBtn(g: Glyph, onClick: () -> Unit) {
    Box(Modifier.size(38.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) { GlyphIcon(g, C.dim, Modifier.size(19.dp)) }
}

@Composable
fun WorldChip(w: World, selected: Boolean, ru: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.clip(shape).background(if (selected) Color(w.accent).copy(alpha = 0.14f) else C.card)
            .border(1.dp, if (selected) Color(w.accent) else C.line, shape).clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PortalView(w, 1f, Frames.of("stone"), Modifier.size(30.dp, 40.dp), widthFrac = 0.8f, still = true)
        Column(Modifier.padding(start = 8.dp)) {
            Text(w.title.of(ru), style = T.sub.copy(color = C.ink))
            Text(w.hint.of(ru), style = T.small, maxLines = 2)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeDialog(h: Int, m: Int, onClose: () -> Unit, onPick: (Int, Int) -> Unit) {
    val st = rememberTimePickerState(h, m, is24Hour = true)
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = C.card,
        confirmButton = { TextButton(onClick = { onPick(st.hour, st.minute) }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.cancel)) } },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(st, colors = TimePickerDefaults.colors(
                    clockDialColor = C.card2, selectorColor = C.gold, timeSelectorSelectedContainerColor = C.gold.copy(alpha = 0.25f),
                    timeSelectorUnselectedContainerColor = C.card2, timeSelectorSelectedContentColor = C.gold, timeSelectorUnselectedContentColor = C.ink,
                    clockDialSelectedContentColor = C.onGold, clockDialUnselectedContentColor = C.ink,
                ))
            }
        },
    )
    Spacer(Modifier.width(0.dp))
}

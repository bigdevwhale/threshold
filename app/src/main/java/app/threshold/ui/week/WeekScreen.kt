package app.threshold.ui.week

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.threshold.R
import app.threshold.container
import app.threshold.data.State
import app.threshold.domain.Frames
import app.threshold.domain.Off
import app.threshold.domain.Week
import app.threshold.ru
import app.threshold.ui.Card
import app.threshold.ui.Fmt
import app.threshold.ui.Label
import app.threshold.ui.Line
import app.threshold.ui.NavRow
import app.threshold.ui.Nav
import app.threshold.ui.Overlay
import app.threshold.ui.ToggleRow
import app.threshold.ui.art.PortalView
import app.threshold.ui.theme.C
import app.threshold.ui.theme.T
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun WeekScreen(s: State, nav: Nav, nowIn: Long?) {
    val ctx = LocalContext.current
    val ru = ctx.ru
    val c = ctx.container
    val now = nowIn ?: System.currentTimeMillis()
    val today = Week.epochDay(now)
    var vacation by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 14.dp, bottom = 24.dp)) {
        Text(stringResource(R.string.week_title), style = T.h1)
        Text(stringResource(R.string.week_sub), style = T.sub, modifier = Modifier.padding(bottom = 6.dp))
        val wild = (0L..6L).map { today + it }.firstOrNull { Week.wildDay(s, it) && Week.off(s, it) == null }
        for (k in 0 until 7) {
            val i = k
            val d = s.week[i]
            val day = (0L..6L).map { today + it }.first { Week.dow(it) == i }
            val off = Week.off(s, day)
            Row(Modifier.fillMaxWidth().clickable { nav.open(Overlay.Day(i)) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                PortalView(if (d.on) d.world else null, if (d.on) 1f else 0f, Frames.of(s.frame), Modifier.size(52.dp, 70.dp), widthFrac = 0.78f, still = true)
                Column(Modifier.width(100.dp).padding(start = 10.dp)) {
                    val mark = when (day - today) { 0L -> " · " + stringResource(R.string.today); 1L -> " · " + stringResource(R.string.tomorrow); else -> "" }
                    Text(Fmt.dowShort(ctx, i) + mark, style = T.small)
                    Text(if (d.on) Fmt.hm(d.h, d.m) else "—", style = T.h2)
                }
                Column(Modifier.weight(1f)) {
                    Text(if (d.on) d.world.title.of(ru) else stringResource(R.string.week_off), style = T.h3)
                    val line = when {
                        !d.on -> stringResource(R.string.week_off_b)
                        d.steps.isEmpty() -> stringResource(R.string.week_plain)
                        else -> d.steps.joinToString(" → ") { it.label }
                    }
                    Text(line, style = T.sub, maxLines = 2)
                    if (d.on && d.auto && d.steps.isNotEmpty()) Text(stringResource(R.string.week_auto), style = T.small.copy(color = C.dim))
                    val note = when (off) {
                        Off.SKIP -> stringResource(R.string.week_skip_once, Fmt.date(ctx, day))
                        Off.HOLIDAY -> stringResource(R.string.week_holiday, Fmt.date(ctx, day))
                        Off.VACATION -> stringResource(R.string.week_vacation)
                        else -> if (day == wild) stringResource(R.string.week_wild) else null
                    }
                    if (note != null && d.on) Text(note, style = T.small.copy(color = C.gold))
                }
            }
            Line()
        }

        Label(stringResource(R.string.week_rules))
        Card {
            ToggleRow(stringResource(R.string.rule_holidays), stringResource(R.string.rule_holidays_b), s.holidays) { v -> c.write { store.update { it.copy(holidays = v) } } }
            val vac = if (s.vacFrom >= 0 && s.vacTo >= today) "${Fmt.date(ctx, s.vacFrom)} – ${Fmt.date(ctx, s.vacTo)}" else stringResource(R.string.rule_vacation_none)
            NavRow(stringResource(R.string.rule_vacation), value = vac) { vacation = true }
            ToggleRow(stringResource(R.string.rule_wild), stringResource(R.string.rule_wild_b), s.wild) { v -> c.write { store.update { it.copy(wild = v) } } }
            NavRow(stringResource(R.string.rule_wishes), value = s.wishes.size.toString(), divider = false) { nav.open(Overlay.Wishes) }
        }
        if (s.vacFrom >= 0 && s.vacTo >= today) {
            Text(stringResource(R.string.rule_vacation_clear), style = T.sub.copy(color = C.gold), modifier = Modifier.padding(top = 10.dp).clickable { c.write { store.update { it.copy(vacFrom = -1, vacTo = -1) } } })
        }
        Spacer(Modifier.height(8.dp))
    }
    if (vacation) VacationDialog(s, { vacation = false }) { a, b -> c.write { store.update { it.copy(vacFrom = a, vacTo = b) } }; vacation = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VacationDialog(s: State, onClose: () -> Unit, onPick: (Long, Long) -> Unit) {
    fun ms(day: Long) = LocalDate.ofEpochDay(day).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    val st = rememberDateRangePickerState(
        initialSelectedStartDateMillis = s.vacFrom.takeIf { it >= 0 }?.let(::ms),
        initialSelectedEndDateMillis = s.vacTo.takeIf { it >= 0 }?.let(::ms),
    )
    DatePickerDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(onClick = {
                val a = st.selectedStartDateMillis; val b = st.selectedEndDateMillis ?: a
                if (a != null && b != null) onPick(a / 86_400_000L, b / 86_400_000L) else onClose()
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.cancel)) } },
    ) {
        Box(Modifier.height(480.dp)) { DateRangePicker(st, title = { Text(stringResource(R.string.rule_vacation), Modifier.padding(start = 24.dp, top = 16.dp)) }) }
    }
}

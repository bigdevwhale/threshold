package app.threshold.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.threshold.R
import app.threshold.alarm.RingService
import app.threshold.container
import app.threshold.data.HOUR
import app.threshold.data.State
import app.threshold.domain.Frames
import app.threshold.domain.Journal
import app.threshold.domain.Portal
import app.threshold.domain.Week
import app.threshold.ru
import app.threshold.ui.Btn
import app.threshold.ui.Card
import app.threshold.ui.Fmt
import app.threshold.ui.HoldButton
import app.threshold.ui.Label
import app.threshold.ui.Meter
import app.threshold.ui.Nav
import app.threshold.ui.Overlay
import app.threshold.ui.art.ArchBox
import app.threshold.ui.art.Glyph
import app.threshold.ui.art.GlyphIcon
import app.threshold.ui.art.portal
import app.threshold.ui.art.rememberTime
import app.threshold.ui.settings.Checks
import app.threshold.ui.theme.C
import app.threshold.ui.theme.T
import kotlin.math.sin

@Composable
fun HomeScreen(s: State, nav: Nav, nowIn: Long?) {
    val ctx = LocalContext.current
    val c = ctx.container
    val ru = ctx.ru
    val now = nowIn ?: System.currentTimeMillis()
    val today = Week.epochDay(now)
    val next = Week.next(s, now)
    val ring = s.ring
    val snoozed = ring != null && ring.snoozeUntil > now
    val morning = next?.let { Portal.morning(s, it.day) }
    val charge = next?.let { Portal.charge(s.closedAt, it.at) } ?: 0
    val time by rememberTime()
    var missing by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { missing = Checks.missing(ctx); onPauseOrDispose { } }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 14.dp, bottom = 24.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(Fmt.today(ctx, today), style = T.sub)
                Text(stringResource(R.string.home_title), style = T.h1)
            }
            Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.open(Overlay.Settings) }, contentAlignment = Alignment.Center) {
                GlyphIcon(Glyph.GEAR, C.dim, Modifier.size(22.dp))
            }
        }
        if (missing > 0 && nowIn == null) {
            Spacer(Modifier.height(12.dp))
            Card(Modifier.clickable { nav.open(Overlay.Reliability) }, color = Color(0xFF2A1A1C)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlyphIcon(Glyph.WARN, C.red, Modifier.size(20.dp))
                    Text(stringResource(R.string.home_unready), style = T.body, modifier = Modifier.padding(start = 10.dp).weight(1f))
                    GlyphIcon(Glyph.CHEVRON, C.dim, Modifier.size(16.dp))
                }
            }
        }
        // The keeper's room: wall, floor, a candle, and the arch with tomorrow behind it.
        Canvas(Modifier.fillMaxWidth().height(400.dp).padding(top = 6.dp)) {
            val w = size.width; val h = size.height; val fy = h * 0.86f
            drawRect(Brush.verticalGradient(listOf(Color(0xFF0B0A13), Color(0xFF16132A)), 0f, fy), Offset.Zero, Size(w, fy))
            drawRect(Color(0xFF0C0A14), Offset(0f, fy), Size(w, h - fy))
            for (i in -6 until 12) drawLine(Color(0xFF191628), Offset(w / 2 + i * 26.dp.toPx(), fy), Offset(w / 2 + i * 60.dp.toPx(), h), 1f)
            val kx = w * 0.88f
            val u = 1.dp.toPx()
            drawRect(Color(0xFFD9CDB0), Offset(kx - 5 * u, fy - 30 * u), Size(10 * u, 30 * u))
            val fl = 1 + 0.12f * sin(time * 11) + 0.08f * sin(time * 7.3f)
            drawCircle(Brush.radialGradient(listOf(Color(0x47FFC878), Color.Transparent), Offset(kx, fy - 40 * u), 60 * u), 60 * u, Offset(kx, fy - 40 * u))
            drawOval(Color(0xFFFFCF7A), Offset(kx - 3 * u * fl, fy - 37 * u - 7 * u * fl), Size(6 * u * fl, 14 * u * fl))
            val aw = w * 0.5f
            val world = if (next == null) null else morning?.world
            val open = if (charge > 0 && world != null) 0.1f + 0.03f * sin(time * 1.5f) else 0f
            portal(ArchBox(w / 2, h * 0.08f, aw, fy - h * 0.08f), time, world, open, Frames.of(s.frame), glow = 0.5f + charge / 50f)
        }
        if (snoozed) {
            Card(color = Color(0xFF1E1830)) {
                Text(stringResource(R.string.home_snoozed, Fmt.hm(ring!!.snoozeUntil)), style = T.h3)
                Text(stringResource(R.string.home_snoozed_b), style = T.sub, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
                Btn(stringResource(R.string.a_up_now), Modifier.fillMaxWidth(), gold = true) { RingService.ring(ctx, ring.day) }
            }
            Spacer(Modifier.height(12.dp))
        }
        Card {
            if (next == null || morning == null) {
                Text(stringResource(R.string.home_none), style = T.h3)
                Text(stringResource(R.string.home_none_b), style = T.sub, modifier = Modifier.padding(top = 4.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.home_next, Fmt.near(ctx, next.day, today)), style = T.sub)
                        Text(morning.world.title.of(ru), style = T.h3)
                        val what = when {
                            morning.wild -> stringResource(R.string.home_wild)
                            morning.steps.isEmpty() -> stringResource(R.string.home_plain)
                            else -> morning.steps.joinToString(" → ") { it.label }
                        }
                        Text(what, style = T.sub)
                    }
                    Text(Fmt.hm(next.at), style = T.big, modifier = Modifier.padding(start = 10.dp))
                }
                Meter(charge / 100f, Modifier.padding(top = 12.dp, bottom = 6.dp))
                val hours = (next.at - now).toFloat() / HOUR
                Text(
                    when {
                        charge >= 87 -> stringResource(R.string.home_charged_full, Fmt.hm(s.closedAt), charge)
                        charge > 0 -> stringResource(R.string.home_charged, Fmt.hm(s.closedAt), charge)
                        hours > 16 -> stringResource(R.string.home_far)
                        else -> stringResource(R.string.home_uncharged)
                    },
                    style = T.sub,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (next != null) {
            val closed = s.closedAt > 0 && now - s.closedAt in 0..(14 * HOUR)
            if (closed) Btn(stringResource(R.string.home_reopen_at, Fmt.hm(s.closedAt)), Modifier.fillMaxWidth()) { c.write { reopenDay() } }
            else HoldButton(stringResource(R.string.home_close)) { c.write { closeDay() } }
            Spacer(Modifier.height(10.dp))
        }
        val skipped = s.offDays.filter { it >= today }.sorted()
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (next != null) Btn(stringResource(R.string.home_skip, Fmt.near(ctx, next.day, today)), Modifier.weight(1f)) {
                c.write { skip(next.day, true) }
                nav.toast(ctx.getString(R.string.toast_skipped, Fmt.near(ctx, next.day, today)))
            }
            skipped.firstOrNull()?.let { d ->
                Btn(stringResource(R.string.home_unskip, Fmt.near(ctx, d, today)), Modifier.weight(1f)) { c.write { skip(d, false) } }
            }
        }
        // The rest of today: the events still to come, a tap opens one now.
        val later = Week.upcoming(s, now, 6).filter { it.day == today }
        if (later.isNotEmpty()) {
            Label(stringResource(R.string.home_later))
            Card {
                later.forEachIndexed { k, d ->
                    Row(Modifier.fillMaxWidth().clickable {
                        if (d.event.step.kind == app.threshold.data.Kind.RADIO) RingService.radio(ctx, d.event.step.url, d.event.step.label)
                        else if (!app.threshold.alarm.Launch.open(ctx, d.event.step)) nav.toast(ctx.getString(R.string.toast_cant_open))
                    }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(Fmt.hm(d.at), style = T.h3, modifier = Modifier.padding(end = 14.dp))
                        GlyphIcon(Fmt.glyph(d.event.step), C.gold, Modifier.size(18.dp))
                        Text(d.event.step.label, style = T.body, maxLines = 1, modifier = Modifier.padding(start = 10.dp).weight(1f))
                    }
                    if (k < later.size - 1) app.threshold.ui.Line()
                }
            }
        }
        val sum = Journal.sum(s)
        if (sum.streak >= 2) {
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.home_streak, sum.streak), style = T.italic, modifier = Modifier.fillMaxWidth())
        }
    }
}

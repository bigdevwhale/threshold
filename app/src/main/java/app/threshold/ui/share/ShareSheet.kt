package app.threshold.ui.share

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.threshold.R
import app.threshold.alarm.Launch
import app.threshold.container
import app.threshold.data.Kind
import app.threshold.data.State
import app.threshold.data.Step
import app.threshold.domain.Portal
import app.threshold.ru
import app.threshold.ui.Btn
import app.threshold.ui.Fmt
import app.threshold.ui.Line
import app.threshold.ui.Nav
import app.threshold.ui.Sheet
import app.threshold.ui.theme.C
import app.threshold.ui.theme.T
import app.threshold.ui.week.firstUrl
import app.threshold.ui.week.guessLabel

/** A link shared from another app (a playlist, a video): which morning it goes to, or the «someday» list. */
@Composable
fun ShareSheet(s: State, text: String?, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val ru = ctx.ru
    val url = text?.let(::firstUrl)
    // A YouTube share is «title url»; what is before the address makes a good name.
    val title = text?.substringBefore(url ?: "\u0000")?.trim()?.trim('-', '–', ':', '«', '»')?.trim().orEmpty()
    val step = url?.let { u ->
        Step(Kind.LINK, if (title.isNotBlank() && title.length < 80) "${guessLabel(u)} · $title" else guessLabel(u), Launch.appFor(ctx, u).orEmpty(), u)
    }
    Sheet(text != null, { c.shared.value = null }) {
        if (step == null) {
            Text(stringResource(R.string.share_none), style = T.h3)
            Spacer(Modifier.height(12.dp))
            Btn(stringResource(R.string.close), Modifier.fillMaxWidth()) { c.shared.value = null }
            return@Sheet
        }
        Text(stringResource(R.string.share_title), style = T.h2)
        Text(step.label, style = T.body, modifier = Modifier.padding(top = 6.dp))
        Text(step.url, style = T.small, maxLines = 1)
        Spacer(Modifier.height(10.dp))
        s.week.forEachIndexed { i, d ->
            Row(Modifier.fillMaxWidth().clickable {
                c.write { setDay(i) { day -> day.copy(on = true, steps = day.steps + step, world = if (day.steps.isEmpty()) Portal.guess(step) else day.world) } }
                c.shared.value = null
                nav.toast(ctx.getString(R.string.toast_added, Fmt.dow(ctx, i)))
            }.padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(Fmt.dow(ctx, i), style = T.body, modifier = Modifier.width(130.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (d.on) "${Fmt.hm(d.h, d.m)} · ${d.world.title.of(ru)}" else stringResource(R.string.week_off), style = T.sub)
                    if (d.steps.isNotEmpty()) Text(stringResource(R.string.share_after, d.steps.last().label), style = T.small, maxLines = 1)
                }
            }
            Line()
        }
        Spacer(Modifier.height(12.dp))
        Btn(stringResource(R.string.share_wish), Modifier.fillMaxWidth(), gold = true) {
            c.write { addWish(step) }
            c.shared.value = null
            nav.toast(ctx.getString(R.string.toast_wish))
        }
        Text(stringResource(R.string.share_wish_b), style = T.small.copy(color = C.dim), modifier = Modifier.padding(top = 6.dp))
    }
}

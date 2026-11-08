package app.threshold.ui.finds

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.threshold.R
import app.threshold.container
import app.threshold.data.State
import app.threshold.domain.Finds
import app.threshold.domain.Frames
import app.threshold.domain.Portal
import app.threshold.domain.World
import app.threshold.ru
import app.threshold.ui.Label
import app.threshold.ui.Nav
import app.threshold.ui.art.FindIcon
import app.threshold.ui.theme.C
import app.threshold.ui.theme.T

@Composable
fun FindsScreen(s: State, nav: Nav) {
    val ctx = LocalContext.current
    val ru = ctx.ru
    val c = ctx.container
    val have = Portal.distinct(s)
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 14.dp, bottom = 24.dp)) {
        Text(stringResource(R.string.finds_title), style = T.h1)
        Text(stringResource(R.string.finds_sub, have, Finds.all.size), style = T.sub)
        World.entries.forEach { w ->
            val list = Finds.of(w)
            if (w == World.WILD && list.none { (s.finds[it.key] ?: 0) > 0 } && !s.wild) return@forEach
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(Color(w.accent)))
                Text(w.title.of(ru), style = T.h3, modifier = Modifier.padding(start = 8.dp))
            }
            list.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    row.forEach { f ->
                        val n = s.finds[f.key] ?: 0
                        val own = n > 0
                        val shape = RoundedCornerShape(12.dp)
                        Column(
                            Modifier.weight(1f).heightIn(min = 96.dp).clip(shape)
                                .background(if (own && f.rare) Color(0xFF1B160C) else Color(0xFF12101D))
                                .border(1.dp, if (own && f.rare) Color(0xFF8A6D33) else C.line, shape).padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            FindIcon(f.shape, when { !own -> Color(0xFF26223A); f.rare -> C.gold; else -> Color(w.accent) }, Modifier.size(32.dp))
                            Spacer(Modifier.height(6.dp))
                            val name = when { own -> f.name.of(ru); f.rare -> stringResource(R.string.finds_rare); else -> "???" }
                            Text(name + if (n > 1) " ×$n" else "", style = T.small.copy(color = if (own) C.ink else C.dim), textAlign = TextAlign.Center)
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Label(stringResource(R.string.finds_frame))
        Text(stringResource(R.string.finds_frame_b), style = T.sub, modifier = Modifier.padding(bottom = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Frames.all.forEach { f ->
                val ok = Portal.frameOpen(s, f)
                val sel = s.frame == f.key
                Column(Modifier.weight(1f).clickable(enabled = ok) { c.write { store.update { it.copy(frame = f.key) } } }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                        .background(Brush.horizontalGradient(listOf(Color(f.a), Color(f.b), Color(f.a))))
                        .border(2.dp, if (sel) C.gold else Color.Transparent, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp)))
                    Text(f.name.of(ru), style = T.small.copy(color = if (sel) C.gold else if (ok) C.ink else C.faint), modifier = Modifier.padding(top = 5.dp))
                    if (!ok) Text(stringResource(R.string.finds_need, f.need), style = T.small.copy(color = C.faint), textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.fillMaxWidth().height(8.dp))
    }
}

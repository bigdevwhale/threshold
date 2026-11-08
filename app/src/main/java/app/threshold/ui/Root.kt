package app.threshold.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.threshold.R
import app.threshold.container
import app.threshold.data.State
import app.threshold.domain.World
import app.threshold.ui.art.Glyph
import app.threshold.ui.art.GlyphIcon
import app.threshold.ui.finds.FindsScreen
import app.threshold.ui.home.HomeScreen
import app.threshold.ui.settings.Reliability
import app.threshold.ui.settings.SettingsPage
import app.threshold.ui.share.ShareSheet
import app.threshold.ui.theme.C
import app.threshold.ui.theme.T
import app.threshold.ui.week.DayPage
import app.threshold.ui.week.WeekScreen
import app.threshold.ui.week.WishesPage
import app.threshold.ui.worlds.WorldsScreen

enum class Tab(val glyph: Glyph, val label: Int) {
    HOME(Glyph.ARCH, R.string.tab_home), WEEK(Glyph.WEEK, R.string.tab_week), WORLDS(Glyph.MAP, R.string.tab_worlds), FINDS(Glyph.SHELF, R.string.tab_finds)
}

/** Pages over the tabs. */
sealed interface Overlay {
    data object Settings : Overlay
    data object Reliability : Overlay
    data object Wishes : Overlay
    data class Day(val i: Int) : Overlay
}

/** Moves between tabs and pages. */
class Nav(val open: (Overlay?) -> Unit, val tab: (Tab) -> Unit, val toast: (String) -> Unit)

@Composable
fun Root(s: State, now: Long? = null, tab0: Tab = Tab.HOME, overlay0: Overlay? = null, world0: World? = null) {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(tab0) }
    var overlay by remember { mutableStateOf(overlay0) }
    var toast by remember { mutableStateOf<String?>(null) }
    val shared by ctx.container.shared.collectAsStateWithLifecycle()
    val nav = remember { Nav({ overlay = it }, { tab = it }, { toast = it }) }
    Box(Modifier.fillMaxSize().background(C.bg)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    Tab.HOME -> HomeScreen(s, nav, now)
                    Tab.WEEK -> WeekScreen(s, nav, now)
                    Tab.WORLDS -> WorldsScreen(s, nav, world0)
                    Tab.FINDS -> FindsScreen(s, nav)
                }
            }
            TabBar(tab) { tab = it }
        }
        when (val o = overlay) {
            Overlay.Settings -> SettingsPage(s, nav)
            Overlay.Reliability -> Reliability(nav)
            Overlay.Wishes -> WishesPage(s, nav)
            is Overlay.Day -> DayPage(s, o.i, nav)
            null -> {}
        }
        ShareSheet(s, shared, nav)
        Toast(toast) { toast = null }
    }
}

@Composable
private fun TabBar(tab: Tab, pick: (Tab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color(0xFF0C0A15)).navigationBarsPadding()) {
        Line()
        Row(Modifier.fillMaxWidth().height(62.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Tab.entries.forEach { t ->
                val color = if (t == tab) C.gold else C.dim
                Column(Modifier.weight(1f).clickable { pick(t) }.padding(top = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    GlyphIcon(t.glyph, color, Modifier.size(23.dp))
                    Text(stringResource(t.label), style = T.small.copy(color = color, fontSize = 11.sp), modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
    }
}

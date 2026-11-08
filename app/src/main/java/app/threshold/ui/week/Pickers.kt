package app.threshold.ui.week

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import app.threshold.R
import app.threshold.alarm.Launch
import app.threshold.container
import app.threshold.data.Kind
import app.threshold.data.State
import app.threshold.data.Step
import app.threshold.ui.Btn
import app.threshold.ui.Card
import app.threshold.ui.Label
import app.threshold.ui.Line
import app.threshold.ui.Nav
import app.threshold.ui.Page
import app.threshold.ui.art.Glyph
import app.threshold.ui.art.GlyphIcon
import app.threshold.ui.theme.C
import app.threshold.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String, url: Boolean = false) {
    OutlinedTextField(
        value, onChange, Modifier.fillMaxWidth().padding(bottom = 10.dp), label = { Text(label) }, singleLine = true,
        keyboardOptions = if (url) KeyboardOptions(keyboardType = KeyboardType.Uri) else KeyboardOptions.Default,
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = C.gold, unfocusedBorderColor = C.line, focusedLabelColor = C.gold, cursorColor = C.gold, focusedTextColor = C.ink, unfocusedTextColor = C.ink),
    )
}

/** The installed apps, to open one as it is. */
@Composable
fun AppPicker(onClose: () -> Unit, onPick: (Step) -> Unit) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<Launch.App>?>(null) }
    var icons by remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }
    var q by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val list = withContext(Dispatchers.IO) { Launch.apps(ctx) }
        apps = list
        // Icons are the slow part: fetch them after the names are on screen, a screenful at a time.
        list.chunked(16).forEach { part ->
            val got = withContext(Dispatchers.IO) {
                part.mapNotNull { a -> runCatching { a.pkg to ctx.packageManager.getApplicationIcon(a.pkg).toBitmap(96, 96).asImageBitmap() }.getOrNull() }
            }
            icons = icons + got
        }
    }
    Page(stringResource(R.string.pick_app), onClose) {
        Field(q, { q = it }, stringResource(R.string.pick_search))
        val list = apps
        if (list == null) Text(stringResource(R.string.pick_loading), style = T.sub)
        else list.filter { q.isBlank() || it.label.contains(q, true) }.forEach { a ->
            val icon = icons[a.pkg]
            Row(Modifier.fillMaxWidth().clickable { onPick(Step(Kind.APP, a.label, a.pkg)) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) Image(icon, null, Modifier.size(36.dp)) else Box(Modifier.size(36.dp))
                Text(a.label, style = T.body, modifier = Modifier.padding(start = 14.dp))
            }
            Line()
        }
    }
}

private fun clip(ctx: Context): String =
    runCatching { ctx.getSystemService(ClipboardManager::class.java).primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString() }.getOrNull().orEmpty()

private val urlRe = Regex("""https?://\S+""")

/** The first web address in a piece of shared text. */
fun firstUrl(text: String): String? = urlRe.find(text)?.value?.trimEnd('.', ',', ')', '»', '"')

/** A name for a link from where it goes, until the user gives a better one. */
fun guessLabel(url: String): String {
    val host = runCatching { Uri.parse(url).host.orEmpty() }.getOrDefault("").removePrefix("www.").removePrefix("m.")
    return when {
        "youtu" in host -> "YouTube"
        "music.yandex" in host -> "Яндекс Музыка"
        "kinopoisk" in host -> "Кинопоиск"
        "spotify" in host -> "Spotify"
        "rutube" in host -> "Rutube"
        "vk.com" in host -> "VK"
        else -> host
    }
}

/** A web address: a playlist, a channel, a film, opened in its own app when it is installed. */
@Composable
fun LinkPicker(was: Step?, onClose: () -> Unit, onPick: (Step) -> Unit) {
    val ctx = LocalContext.current
    var url by remember { mutableStateOf(was?.url.orEmpty()) }
    var label by remember { mutableStateOf(was?.label.orEmpty()) }
    Page(stringResource(R.string.pick_link), onClose) {
        Text(stringResource(R.string.pick_link_b), style = T.sub, modifier = Modifier.padding(bottom = 12.dp))
        Field(url, { url = it; if (label.isBlank() || label == guessLabel(url.dropLast(1))) label = firstUrl(it)?.let(::guessLabel).orEmpty() }, stringResource(R.string.pick_url), url = true)
        Btn(stringResource(R.string.pick_paste), Modifier.fillMaxWidth()) {
            firstUrl(clip(ctx))?.let { u -> url = u; if (label.isBlank()) label = guessLabel(u) }
        }
        Spacer(Modifier.height(12.dp))
        Field(label, { label = it }, stringResource(R.string.pick_label))
        val where = firstUrl(url)?.let { Launch.where(ctx, it) }
        val app = (where as? Launch.Where.App)?.pkg
        val note = when (where) {
            is Launch.Where.App -> stringResource(R.string.pick_opens_in, appName(ctx, where.pkg))
            is Launch.Where.NotTaken -> stringResource(R.string.pick_not_taken, appName(ctx, where.pkg))
            else -> stringResource(R.string.pick_opens_browser)
        }
        Text(note, style = T.small.copy(color = if (where is Launch.Where.NotTaken) C.red else C.dim), modifier = Modifier.padding(bottom = 14.dp))
        val ok = firstUrl(url) != null
        Btn(stringResource(R.string.pick_save), Modifier.fillMaxWidth(), gold = true, enabled = ok) {
            val u = firstUrl(url) ?: return@Btn
            onPick(Step(Kind.LINK, label.ifBlank { guessLabel(u) }, app.orEmpty(), Launch.normalize(u), was?.minutes ?: 15))
        }
    }
}

private fun appName(ctx: Context, pkg: String) = runCatching { ctx.packageManager.getApplicationLabel(ctx.packageManager.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)

/** Stations that have been on the air for years; any other stream can be pasted. */
private val stations = listOf(
    "Radio Swiss Jazz" to "https://stream.srg-ssr.ch/m/rsj/mp3_128",
    "Radio Swiss Classic" to "https://stream.srg-ssr.ch/m/rsc_de/mp3_128",
    "Radio Paradise" to "https://stream.radioparadise.com/mp3-128",
    "SomaFM · Groove Salad" to "https://ice2.somafm.com/groovesalad-128-mp3",
)

/** A radio stream played by the portal itself: it can be the alarm sound. */
@Composable
fun RadioPicker(onClose: () -> Unit, onPick: (Step) -> Unit) {
    var url by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    Page(stringResource(R.string.pick_radio), onClose) {
        Text(stringResource(R.string.pick_radio_b), style = T.sub, modifier = Modifier.padding(bottom = 6.dp))
        Card {
            stations.forEachIndexed { i, (name, u) ->
                Row(Modifier.fillMaxWidth().clickable { onPick(Step(Kind.RADIO, name, "", u, 30)) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlyphIcon(Glyph.RADIO, C.gold, Modifier.size(20.dp))
                    Text(name, style = T.body, modifier = Modifier.padding(start = 12.dp).weight(1f))
                    GlyphIcon(Glyph.PLUS, C.dim, Modifier.size(18.dp))
                }
                if (i < stations.size - 1) Line()
            }
        }
        Label(stringResource(R.string.pick_radio_own))
        Field(url, { url = it }, stringResource(R.string.pick_stream), url = true)
        Field(label, { label = it }, stringResource(R.string.pick_label))
        Btn(stringResource(R.string.pick_save), Modifier.fillMaxWidth(), gold = true, enabled = firstUrl(url) != null) {
            val u = firstUrl(url) ?: return@Btn
            onPick(Step(Kind.RADIO, label.ifBlank { guessLabel(u) }, "", u, 30))
        }
    }
}

/** The «someday» list: what the wild portal brings, one thing a week. */
@Composable
fun WishesPage(s: State, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    var adding by remember { mutableStateOf(false) }
    Page(stringResource(R.string.wishes_title), { nav.open(null) }) {
        Text(stringResource(R.string.wishes_b), style = T.sub, modifier = Modifier.padding(bottom = 12.dp))
        if (s.wishes.isEmpty()) Text(stringResource(R.string.wishes_none), style = T.italic, modifier = Modifier.padding(vertical = 10.dp))
        s.wishes.forEachIndexed { i, w ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                GlyphIcon(Glyph.LINK, C.gold, Modifier.size(20.dp))
                androidx.compose.foundation.layout.Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(w.label, style = T.body)
                    Text(w.url.removePrefix("https://").take(44), style = T.small, maxLines = 1)
                }
                Box(Modifier.size(38.dp).clickable { c.write { store.update { st -> st.copy(wishes = st.wishes.filterIndexed { j, _ -> j != i }) } } }, contentAlignment = Alignment.Center) {
                    GlyphIcon(Glyph.TRASH, C.dim, Modifier.size(19.dp))
                }
            }
            Line()
        }
        Spacer(Modifier.height(12.dp))
        Btn("+ " + stringResource(R.string.k_link), Modifier.fillMaxWidth(), gold = true) { adding = true }
        if (!s.wild) Text(stringResource(R.string.wishes_off), style = T.small, modifier = Modifier.padding(top = 8.dp))
    }
    if (adding) LinkPicker(null, { adding = false }) { st -> c.write { addWish(st) }; adding = false }
}

package app.threshold.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.threshold.BuildConfig
import app.threshold.R
import app.threshold.alarm.RingService
import app.threshold.container
import app.threshold.data.State
import app.threshold.ui.Btn
import app.threshold.ui.Card
import app.threshold.ui.Choice
import app.threshold.ui.Label
import app.threshold.ui.NavRow
import app.threshold.ui.Nav
import app.threshold.ui.Overlay
import app.threshold.ui.Page
import app.threshold.ui.ToggleRow
import app.threshold.ui.art.Glyph
import app.threshold.ui.art.GlyphIcon
import app.threshold.ui.theme.C
import app.threshold.ui.theme.T

/** The StillCraft bot: deep links file a bug or an idea for this app, or follow its releases. */
object Community {
    private const val BOT = "https://t.me/ArcaneWhisperBot?start="
    const val SLUG = "threshold"
    const val BUG = "${BOT}bug_$SLUG"
    const val IDEA = "${BOT}wish_$SLUG"
    const val FOLLOW = "${BOT}f_$SLUG"
    const val CHAT = "https://t.me/+Q4gFEDk8O842OGVi"
    const val SUPPORT = "https://bigdevwhale.github.io/stillcraft/support/"
}

/** What the phone has to allow for the alarm to ring on time, over the lock screen. */
object Checks {
    val xiaomi get() = Build.MANUFACTURER.lowercase().let { "xiaomi" in it || "redmi" in it || "poco" in it }

    fun notifications(ctx: Context) = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    fun fullScreen(ctx: Context) = Build.VERSION.SDK_INT < 34 || ctx.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    fun exact(ctx: Context) = Build.VERSION.SDK_INT < 31 || ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    fun battery(ctx: Context) = ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)

    fun missing(ctx: Context) = listOf(notifications(ctx), fullScreen(ctx), exact(ctx), battery(ctx)).count { !it }
}

@Composable
fun SettingsPage(s: State, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    fun open(url: String) { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    fun set(change: (State) -> State) = c.write { store.update(change) }
    Page(stringResource(R.string.settings), { nav.open(null) }) {
        Label(stringResource(R.string.set_alarm))
        Card {
            Text(stringResource(R.string.set_snooze_len), style = T.sub, modifier = Modifier.padding(bottom = 8.dp))
            Choice(listOf(5 to "5", 10 to "10", 15 to "15").map { it.first to stringResource(R.string.min_n, it.second) }, s.snoozeMin) { v -> set { it.copy(snoozeMin = v) } }
            Text(stringResource(R.string.set_snooze_max), style = T.sub, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
            Choice(listOf(0 to stringResource(R.string.none), 1 to "1", 2 to "2", 3 to "3"), s.maxSnooze) { v -> set { it.copy(maxSnooze = v) } }
            Text(stringResource(R.string.set_snooze_note), style = T.small, modifier = Modifier.padding(top = 8.dp))
            Text(stringResource(R.string.set_ramp), style = T.sub, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
            Choice(listOf(30 to stringResource(R.string.sec_n, 30), 60 to stringResource(R.string.min_n, "1"), 120 to stringResource(R.string.min_n, "2")), s.rampSec) { v -> set { it.copy(rampSec = v) } }
            Spacer(Modifier.height(6.dp))
            ToggleRow(stringResource(R.string.set_vibrate), on = s.vibrate, divider = false) { v -> set { it.copy(vibrate = v) } }
        }
        Spacer(Modifier.height(10.dp))
        Btn(stringResource(R.string.set_test), Modifier.fillMaxWidth()) { RingService.test(ctx) }
        Text(stringResource(R.string.set_test_b), style = T.small, modifier = Modifier.padding(top = 6.dp))

        Label(stringResource(R.string.set_day))
        Card {
            ToggleRow(stringResource(R.string.set_evening), stringResource(R.string.set_evening_b), s.evening) { v -> set { it.copy(evening = v) } }
            Text(stringResource(R.string.set_nudge), style = T.body, modifier = Modifier.padding(top = 12.dp))
            Text(stringResource(R.string.set_nudge_b), style = T.sub, modifier = Modifier.padding(bottom = 8.dp))
            Choice(listOf(0 to stringResource(R.string.off), 15 to "15", 20 to "20", 30 to "30"), s.nudgeMin) { v -> set { it.copy(nudgeMin = v) } }
        }

        Label(stringResource(R.string.set_more))
        Card {
            NavRow(stringResource(R.string.rel_title), sub = stringResource(R.string.rel_sub)) { nav.open(Overlay.Reliability) }
            val cur = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').substringBefore(',')
            Text(stringResource(R.string.set_lang), style = T.body, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
            Choice(listOf("" to stringResource(R.string.lang_system), "ru" to "Русский", "en" to "English"), cur) { v ->
                AppCompatDelegate.setApplicationLocales(if (v.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(v))
            }
            Spacer(Modifier.height(8.dp))
        }

        Label("StillCraft")
        Card {
            Text(stringResource(R.string.community_text), style = T.sub, modifier = Modifier.padding(bottom = 4.dp))
            NavRow(stringResource(R.string.community_bug), sub = stringResource(R.string.community_bug_sub)) { open(Community.BUG) }
            NavRow(stringResource(R.string.community_idea), sub = stringResource(R.string.community_idea_sub)) { open(Community.IDEA) }
            NavRow(stringResource(R.string.community_follow), sub = stringResource(R.string.community_follow_sub)) { open(Community.FOLLOW) }
            NavRow(stringResource(R.string.community_chat)) { open(Community.CHAT) }
            NavRow(stringResource(R.string.community_support), sub = stringResource(R.string.community_support_sub), divider = false) { open(Community.SUPPORT) }
        }
        Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = T.small, modifier = Modifier.padding(top = 18.dp))
    }
}

/** Everything the phone must allow, each with a way to fix it. */
@SuppressLint("BatteryLife")
@Composable
fun Reliability(nav: Nav) {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { tick++; onPauseOrDispose { } }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    // Re-read on every return from the system settings.
    val ok = remember(tick) { listOf(Checks.notifications(ctx), Checks.fullScreen(ctx), Checks.exact(ctx), Checks.battery(ctx)) }
    fun go(i: Intent) { runCatching { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.onFailure { details(ctx) } }
    Page(stringResource(R.string.rel_title), { nav.open(null) }) {
        Text(stringResource(R.string.rel_intro), style = T.sub, modifier = Modifier.padding(vertical = 8.dp))
        Check(stringResource(R.string.rel_notif), stringResource(R.string.rel_notif_b), ok[0]) {
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                ask.launch(Manifest.permission.POST_NOTIFICATIONS)
            else go(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
        }
        Check(stringResource(R.string.rel_full), stringResource(R.string.rel_full_b), ok[1]) {
            if (Build.VERSION.SDK_INT >= 34) go(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${ctx.packageName}")))
        }
        Check(stringResource(R.string.rel_exact), stringResource(R.string.rel_exact_b), ok[2]) {
            if (Build.VERSION.SDK_INT >= 31) go(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")))
        }
        Check(stringResource(R.string.rel_battery), stringResource(R.string.rel_battery_b), ok[3]) {
            go(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}")))
        }
        if (Checks.xiaomi) {
            Label(stringResource(R.string.rel_xiaomi))
            Card {
                Text(stringResource(R.string.rel_xiaomi_b), style = T.sub)
                Spacer(Modifier.height(10.dp))
                Btn(stringResource(R.string.rel_autostart), Modifier.fillMaxWidth()) {
                    go(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")))
                }
                Spacer(Modifier.height(8.dp))
                Btn(stringResource(R.string.rel_other), Modifier.fillMaxWidth()) {
                    go(Intent("miui.intent.action.APP_PERM_EDITOR").putExtra("extra_pkgname", ctx.packageName))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Btn(stringResource(R.string.set_test), Modifier.fillMaxWidth(), gold = true) { RingService.test(ctx) }
    }
}

private fun details(ctx: Context) {
    runCatching { ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

@Composable
private fun Check(title: String, sub: String, ok: Boolean, fix: () -> Unit) {
    Card(Modifier.padding(bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphIcon(if (ok) Glyph.CHECK else Glyph.WARN, if (ok) C.gold else C.red, Modifier.size(22.dp), stroke = 2f)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, style = T.body)
                Text(sub, style = T.sub)
            }
        }
        if (!ok) Btn(stringResource(R.string.rel_fix), Modifier.fillMaxWidth().padding(top = 10.dp), gold = true, onClick = fix)
    }
}

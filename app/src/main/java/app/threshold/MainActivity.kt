package app.threshold

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.threshold.data.State
import app.threshold.ui.Root
import app.threshold.ui.art.Art
import app.threshold.ui.theme.ThresholdTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.SHOTS) Art.still = true
        val t = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(t), navigationBarStyle = SystemBarStyle.dark(t))
        take(intent)
        askNotifications()
        setContent {
            val s: State? by container.store.state.collectAsStateWithLifecycle(initialValue = null)
            val seeding by container.seeding.collectAsStateWithLifecycle()
            if (!seeding) s?.let { ThresholdTheme { Root(it) } }
        }
    }

    private val notificationAsk = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }

    /** An alarm is a notification: ask for them once, on the first start, instead of failing silently in the morning. */
    private fun askNotifications() {
        if (android.os.Build.VERSION.SDK_INT < 33 || BuildConfig.SHOTS) return
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) return
        val prefs = getSharedPreferences("asked", MODE_PRIVATE)
        if (prefs.getBoolean("notifications", false)) return
        prefs.edit().putBoolean("notifications", true).apply()
        notificationAsk.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        take(intent)
    }

    /** A link shared from YouTube, Yandex Music, a browser…: offered to a day of the week. */
    private fun take(i: Intent?) {
        // Debug builds only: `am start … --ei ringIn 30` sets a real alarm for today (or `--el day <epoch day>`) in 30 s, to test the lock screen.
        if (BuildConfig.DEBUG && i != null && i.hasExtra("ringIn")) {
            val at = System.currentTimeMillis() + i.getIntExtra("ringIn", 30) * 1000L
            val ring = android.app.PendingIntent.getBroadcast(this, 9,
                Intent(this, app.threshold.alarm.AlarmReceiver::class.java).setAction(app.threshold.alarm.Scheduler.RING)
                    .putExtra(app.threshold.alarm.Scheduler.EXTRA_DAY, i.getLongExtra("day", app.threshold.domain.Week.epochDay(at))),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
            val show = android.app.PendingIntent.getActivity(this, 2, Intent(this, MainActivity::class.java), android.app.PendingIntent.FLAG_IMMUTABLE)
            getSystemService(android.app.AlarmManager::class.java).setAlarmClock(android.app.AlarmManager.AlarmClockInfo(at, show), ring)
            return
        }
        // Debug builds only: `am start … --es open <url>` opens a link exactly as a morning would.
        if (BuildConfig.DEBUG && i != null && i.hasExtra("open")) {
            app.threshold.alarm.Launch.open(this, app.threshold.data.Step(app.threshold.data.Kind.LINK, "", url = i.getStringExtra("open").orEmpty()))
            return
        }
        // Debug builds only: `--ei chainSec 20` makes chain steps last seconds; `--es openApp <pkg>` opens an app as a
        // morning would; `--ez evening true` posts the evening «close the day» notification now.
        if (BuildConfig.DEBUG && i != null) {
            if (i.hasExtra("chainSec")) { Debug.chainSec = i.getIntExtra("chainSec", 0); return }
            if (i.hasExtra("openApp")) { app.threshold.alarm.Launch.open(this, app.threshold.data.Step(app.threshold.data.Kind.APP, "", i.getStringExtra("openApp").orEmpty())); return }
            if (i.getBooleanExtra("evening", false)) { app.threshold.alarm.Notify.evening(this); return }
            // `--ez event true` adds an event for the coming minute, today, opening a YouTube search: tests the event alarm.
            if (i.getBooleanExtra("event", false)) {
                val t = java.time.LocalTime.now().plusMinutes(1)
                val e = app.threshold.data.Event(System.currentTimeMillis(), setOf(java.time.LocalDate.now().dayOfWeek.value - 1), t.hour, t.minute,
                    app.threshold.data.Step(app.threshold.data.Kind.LINK, "YouTube · Йога", "", "https://www.youtube.com/results?search_query=yoga"))
                container.write { store.update { it.copy(events = it.events + e) } }
                return
            }
            if (i.hasExtra("next")) {
                sendBroadcast(Intent(this, app.threshold.alarm.AlarmReceiver::class.java).setAction(app.threshold.alarm.Scheduler.NEXT)
                    .putExtra(app.threshold.alarm.Scheduler.EXTRA_INDEX, 1).putExtra(app.threshold.alarm.Scheduler.EXTRA_DAY, i.getLongExtra("next", -1)))
                return
            }
        }
        if (i?.action != Intent.ACTION_SEND) return
        val text = listOfNotNull(i.getStringExtra(Intent.EXTRA_SUBJECT), i.getStringExtra(Intent.EXTRA_TEXT)).distinct().joinToString(" ")
        if (text.isNotBlank()) container.shared.value = text
    }
}

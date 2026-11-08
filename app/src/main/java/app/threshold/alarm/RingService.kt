package app.threshold.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import app.threshold.container
import app.threshold.data.Kind
import app.threshold.data.MIN
import app.threshold.data.Ring
import app.threshold.domain.Portal
import app.threshold.domain.Week
import app.threshold.ru
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The ringing portal: a foreground service that plays the hum (or the morning's radio), vibrates, and shows the
 * alarm over the lock screen. It keeps playing the radio after the step through, if the morning is the radio.
 */
class RingService : Service() {
    companion object {
        const val RING = "ring"
        const val TEST = "test"
        const val SNOOZE = "snooze"
        const val STEP = "step"
        const val STOP = "stop"
        const val RADIO = "radio"
        /** «Встать сейчас» from the snoozed notification: ring again at once. */
        const val NOW = "now"
        /** The alarm screen is showing: drop the heads-up that would cover it. */
        const val QUIET = "quiet"
        private const val EXTRA_URL = "url"
        private const val EXTRA_KEEP = "keep"
        private const val EXTRA_NAME = "name"

        /** True while the portal is calling; the alarm screen watches it. */
        val ringing = MutableStateFlow(false)

        @Volatile var lift = 0f
            set(v) { field = v; current?.synth?.lift = v }

        private var current: RingService? = null

        private fun send(ctx: Context, i: Intent) = runCatching { ContextCompat.startForegroundService(ctx, i) }

        fun ring(ctx: Context, day: Long) = send(ctx, Intent(ctx, RingService::class.java).setAction(RING).putExtra(Scheduler.EXTRA_DAY, day))
        fun test(ctx: Context) = send(ctx, Intent(ctx, RingService::class.java).setAction(TEST))
        fun radio(ctx: Context, url: String, name: String) = send(ctx, Intent(ctx, RingService::class.java).setAction(RADIO).putExtra(EXTRA_URL, url).putExtra(EXTRA_NAME, name))
        fun snooze(ctx: Context) = ctx.startService(Intent(ctx, RingService::class.java).setAction(SNOOZE))
        fun step(ctx: Context, keepRadio: Boolean) = ctx.startService(Intent(ctx, RingService::class.java).setAction(STEP).putExtra(EXTRA_KEEP, keepRadio))
        fun stop(ctx: Context) = ctx.startService(Intent(ctx, RingService::class.java).setAction(STOP))
        fun quiet(ctx: Context) = runCatching { ctx.startService(Intent(ctx, RingService::class.java).setAction(QUIET)) }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val main = Handler(Looper.getMainLooper())
    var synth: Synth? = null
    private var radio: Radio? = null
    private var lock: PowerManager.WakeLock? = null
    private var foreground = false
    private var radioName = ""
    private var ringNote: Triple<String, String, Int?>? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        current = this
        Notify.channels(this)
    }

    private fun fg(id: Int, n: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= 29) startForeground(id, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) else startForeground(id, n)
        foreground = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val c = container
        when (intent?.action) {
            RING, TEST, NOW -> {
                // Show something at once: a foreground service has only a few seconds to do so.
                fg(Notify.ID_RING, Notify.ring(this, "", time(System.currentTimeMillis()), null))
                Notify.cancel(this, Notify.ID_SNOOZED)
                val test = intent.action == TEST
                val day = intent.getLongExtra(Scheduler.EXTRA_DAY, -1L)
                scope.launch { begin(day, test) }
            }
            RADIO -> {
                stopSound()
                radioName = intent.getStringExtra(EXTRA_NAME).orEmpty()
                fg(Notify.ID_RING, Notify.radio(this, radioName))
                radio = Radio { finish() }.also { it.play(intent.getStringExtra(EXTRA_URL).orEmpty(), 20, alarm = false) }
            }
            SNOOZE -> scope.launch {
                val now = System.currentTimeMillis()
                var until = 0L
                c.store.update { s ->
                    val r = s.ring ?: return@update s
                    until = now + s.snoozeMin * MIN
                    s.copy(ring = r.copy(snoozes = r.snoozes + 1, snoozeUntil = until))
                }
                if (until > 0) Notify.snoozed(this@RingService, time(until))
                finish()
            }
            STEP -> {
                stopSound(keepRadio = intent.getBooleanExtra(EXTRA_KEEP, false))
                if (radio?.playing == true) fg(Notify.ID_RING, Notify.radio(this, radioName)) else finish()
            }
            STOP -> finish()
            QUIET -> if (ringing.value) ringNote?.let { (w, t, sn) -> fg(Notify.ID_RING, Notify.ring(this, w, t, sn, quiet = true)) }
            else -> if (!foreground) stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun time(ms: Long) = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))

    private suspend fun begin(dayIn: Long, test: Boolean) {
        val c = container
        val now = System.currentTimeMillis()
        c.store.update { s ->
            val r = s.ring
            val day = when {
                r != null && !test -> r.day
                dayIn >= 0 -> dayIn
                else -> Week.epochDay(now)
            }
            // A snoozed morning keeps its first ring time; a new morning (or a test) starts afresh.
            val ring = if (r != null && r.day == day && !test && !r.test) r.copy(snoozeUntil = 0) else Ring(day, now, test = test)
            s.copy(ring = ring)
        }
        val s = c.store.now()
        val r = s.ring ?: return finish()
        val m = Portal.morning(s, r.day)
        val ru = ru
        val snooze = if (r.snoozes < s.maxSnooze) s.snoozeMin else null
        ringNote = Triple(m.world.title.of(ru), time(now), snooze)
        fg(Notify.ID_RING, Notify.ring(this, m.world.title.of(ru), time(now), snooze, quiet = AlarmActivity.visible))
        lock = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "threshold:ring").apply { acquire(11 * MIN) }
        val first = m.steps.firstOrNull()
        if (first?.kind == Kind.RADIO && first.url.isNotBlank()) {
            radioName = first.label
            radio = Radio { startSynth(s.rampSec) }.also { it.play(first.url, s.rampSec, alarm = true) }
        } else startSynth(s.rampSec)
        if (s.vibrate) vibrate()
        ringing.value = true
        // Try to show the alarm screen straight away; when the phone forbids it, the full-screen notification does.
        runCatching { startActivity(Intent(this, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)) }
        // Ten minutes unanswered: snooze if allowed, otherwise fall quiet.
        main.postDelayed({ if (ringing.value) { if (snooze != null) onStartCommand(Intent(this, RingService::class.java).setAction(SNOOZE), 0, 0) else finish() } }, 10 * MIN)
    }

    private fun startSynth(ramp: Int) {
        if (synth != null) return
        synth = Synth(ramp).also { it.lift = lift; it.start() }
    }

    private fun vibrate() {
        val v = if (Build.VERSION.SDK_INT >= 31) getSystemService(VibratorManager::class.java).defaultVibrator else @Suppress("DEPRECATION") getSystemService(Vibrator::class.java)
        // Soft at first, like the bells: a short pulse, then longer ones.
        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 1300, 500, 1100, 700, 900), intArrayOf(0, 90, 0, 160, 0, 230, 0), 3))
    }

    private fun stopVibrate() {
        val v = if (Build.VERSION.SDK_INT >= 31) getSystemService(VibratorManager::class.java).defaultVibrator else @Suppress("DEPRECATION") getSystemService(Vibrator::class.java)
        v.cancel()
    }

    private fun stopSound(keepRadio: Boolean = false) {
        ringing.value = false
        main.removeCallbacksAndMessages(null)
        synth?.stop(); synth = null
        if (!keepRadio) { radio?.stop(); radio = null }
        stopVibrate()
        lift = 0f
    }

    private fun stopForegroundCompat() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        foreground = false
    }

    private fun finish() {
        stopSound()
        lock?.let { if (it.isHeld) it.release() }; lock = null
        stopForegroundCompat()
        stopSelf()
    }

    override fun onDestroy() {
        stopSound()
        lock?.let { if (it.isHeld) it.release() }
        scope.cancel()
        if (current === this) current = null
        super.onDestroy()
    }
}

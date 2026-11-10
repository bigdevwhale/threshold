package app.threshold.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.threshold.MainActivity
import app.threshold.R
import app.threshold.data.Event
import app.threshold.data.Kind
import app.threshold.data.Step

object Notify {
    const val CH_RING = "ring"
    const val CH_DAY = "day"
    const val CH_EVENT = "event"
    const val ID_RING = 1
    const val ID_EVENING = 2
    const val ID_NEXT = 3
    const val ID_NUDGE = 4
    const val ID_SNOOZED = 5

    fun channels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_RING, ctx.getString(R.string.ch_ring), NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null); enableVibration(false); setBypassDnd(true); lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        })
        nm.createNotificationChannel(NotificationChannel(CH_DAY, ctx.getString(R.string.ch_day), NotificationManager.IMPORTANCE_DEFAULT))
        // The day's events: heads-up, with the phone's notification sound.
        nm.createNotificationChannel(NotificationChannel(CH_EVENT, ctx.getString(R.string.ch_event), NotificationManager.IMPORTANCE_HIGH).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        })
    }

    private fun home(ctx: Context): PendingIntent =
        PendingIntent.getActivity(ctx, 20, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)

    fun alarmScreen(ctx: Context): PendingIntent =
        PendingIntent.getActivity(ctx, 21, Intent(ctx, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun service(ctx: Context, action: String, code: Int, fgs: Boolean = false): PendingIntent {
        val i = Intent(ctx, RingService::class.java).setAction(action)
        val f = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (fgs) PendingIntent.getForegroundService(ctx, code, i, f) else PendingIntent.getService(ctx, code, i, f)
    }

    /** [quiet] once the alarm screen is up: the same notification, without a heads-up over the portal. */
    fun ring(ctx: Context, world: String, time: String, snooze: Int?, quiet: Boolean = false): Notification =
        NotificationCompat.Builder(ctx, if (quiet) CH_DAY else CH_RING)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(ctx.getString(R.string.n_ring, time))
            .setContentText(world)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .apply { if (!quiet) setFullScreenIntent(alarmScreen(ctx), true) else setSilent(true) }
            .setContentIntent(alarmScreen(ctx))
            .apply { if (snooze != null) addAction(0, ctx.getString(R.string.a_snooze, snooze), service(ctx, RingService.SNOOZE, 30)) }
            .build()

    fun radio(ctx: Context, name: String): Notification =
        NotificationCompat.Builder(ctx, CH_DAY)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(ctx.getString(R.string.n_radio))
            .setContentText(name)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(home(ctx))
            .addAction(0, ctx.getString(R.string.a_radio_stop), service(ctx, RingService.STOP, 31))
            .build()

    private fun post(ctx: Context, id: Int, n: Notification) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        try { nm.notify(id, n) } catch (_: SecurityException) { }
    }

    fun cancel(ctx: Context, id: Int) = NotificationManagerCompat.from(ctx).cancel(id)

    fun snoozed(ctx: Context, time: String) = post(ctx, ID_SNOOZED,
        NotificationCompat.Builder(ctx, CH_DAY).setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(ctx.getString(R.string.n_snoozed, time)).setContentText(ctx.getString(R.string.n_snoozed_b))
            .setSilent(true).setOngoing(true).setContentIntent(home(ctx))
            .addAction(0, ctx.getString(R.string.a_up_now), service(ctx, RingService.NOW, 32, fgs = true))
            .build())

    fun evening(ctx: Context) = post(ctx, ID_EVENING,
        NotificationCompat.Builder(ctx, CH_DAY).setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(ctx.getString(R.string.n_evening)).setContentText(ctx.getString(R.string.n_evening_b))
            .setAutoCancel(true).setContentIntent(home(ctx))
            .addAction(0, ctx.getString(R.string.a_close_day), Scheduler.closeIntent(ctx))
            .build())

    fun next(ctx: Context, step: Step) {
        val i = Launch.intent(ctx, step) ?: return
        val pi = PendingIntent.getActivity(ctx, 22, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        post(ctx, ID_NEXT, NotificationCompat.Builder(ctx, CH_DAY).setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(ctx.getString(R.string.n_next)).setContentText(step.label)
            .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(pi).build())
    }

    fun nudge(ctx: Context) = post(ctx, ID_NUDGE,
        NotificationCompat.Builder(ctx, CH_DAY).setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(ctx.getString(R.string.n_nudge)).setContentText(ctx.getString(R.string.n_nudge_b))
            .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(home(ctx)).build())

    /** Each event has its own notification, so two at the same time don't replace each other. */
    fun eventId(e: Event): Int = 1000 + Math.floorMod(e.id, 1_000_000L).toInt()

    /** An event of the day is due: a tap opens it; a radio is already playing. */
    fun event(ctx: Context, e: Event, time: String) {
        val st = e.step
        val code = eventId(e)
        val open = Launch.intent(ctx, st)?.let { PendingIntent.getActivity(ctx, code, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) }
        post(ctx, code, NotificationCompat.Builder(ctx, CH_EVENT).setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("$time · ${st.label}")
            .setContentText(ctx.getString(if (st.kind == Kind.RADIO) R.string.n_event_radio else R.string.n_event_b))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setTimeoutAfter(3 * 60 * 60_000L)
            .setContentIntent(open ?: home(ctx))
            .apply { if (open != null) addAction(0, ctx.getString(R.string.a_open), open) }
            .build())
    }
}

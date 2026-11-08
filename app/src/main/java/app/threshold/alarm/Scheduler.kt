package app.threshold.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import app.threshold.MainActivity
import app.threshold.data.State
import app.threshold.domain.Portal
import app.threshold.domain.Week

/** Sets the system alarms: the morning ring (as a real alarm clock), the bedtime nudge, the chain and the «пора». */
object Scheduler {
    const val RING = "app.threshold.RING"
    const val EVENING = "app.threshold.EVENING"
    const val CLOSE = "app.threshold.CLOSE"
    const val NEXT = "app.threshold.NEXT"
    const val NUDGE = "app.threshold.NUDGE"
    const val EXTRA_DAY = "day"
    const val EXTRA_INDEX = "index"

    private fun pi(ctx: Context, action: String, code: Int, extras: Intent.() -> Unit = {}): PendingIntent =
        PendingIntent.getBroadcast(ctx, code, Intent(ctx, AlarmReceiver::class.java).setAction(action).apply(extras),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun canExact(ctx: Context): Boolean {
        val am = ctx.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
    }

    /** The time the portal opens next (a snooze or the next day), or null. */
    fun target(s: State, now: Long): Pair<Long, Long>? {
        val r = s.ring
        if (r != null && r.snoozeUntil > now) return r.snoozeUntil to r.day
        return Week.next(s, now)?.let { it.at to it.day }
    }

    fun arm(ctx: Context, s: State, now: Long = System.currentTimeMillis()) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val t = target(s, now)
        val ring = pi(ctx, RING, 1) { putExtra(EXTRA_DAY, t?.second ?: -1L) }
        if (t == null) am.cancel(ring)
        else {
            val show = PendingIntent.getActivity(ctx, 2, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            if (canExact(ctx)) am.setAlarmClock(AlarmManager.AlarmClockInfo(t.first, show), ring)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.first, ring)
        }
        // The evening reminder to close the day, if it isn't closed yet.
        val eve = pi(ctx, EVENING, 3)
        val next = Week.next(s, now)
        val bed = next?.let { Week.bedtime(it) }
        if (s.evening && next != null && bed != null && bed > now && Portal.charge(s.closedAt, next.at) == 0)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, bed, eve)
        else am.cancel(eve)
    }

    /** The chain: at [at] the portal leads on to step [index] of the morning. */
    fun chain(ctx: Context, index: Int, at: Long, day: Long) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val p = pi(ctx, NEXT, 4) { putExtra(EXTRA_INDEX, index); putExtra(EXTRA_DAY, day) }
        if (canExact(ctx)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p) else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
    }

    fun nudge(ctx: Context, at: Long) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        // «Пора» after twenty minutes means twenty, not up to thirty-five: exact when allowed.
        val p = pi(ctx, NUDGE, 5)
        if (canExact(ctx)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p) else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
    }

    fun cancelMorning(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.cancel(pi(ctx, NEXT, 4)); am.cancel(pi(ctx, NUDGE, 5))
    }

    fun closeIntent(ctx: Context): PendingIntent = pi(ctx, CLOSE, 6)
}

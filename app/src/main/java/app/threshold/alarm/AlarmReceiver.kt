package app.threshold.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.threshold.container
import app.threshold.data.Kind
import app.threshold.domain.Week
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Every system alarm of the app lands here; also re-arms the morning after a reboot or a change of clock. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val c = ctx.container
        when (intent.action) {
            Scheduler.RING -> RingService.ring(ctx, intent.getLongExtra(Scheduler.EXTRA_DAY, -1L))
            else -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        val now = System.currentTimeMillis()
                        when (intent.action) {
                            Scheduler.EVENING -> Notify.evening(ctx)
                            Scheduler.CLOSE -> { c.closeDay(now); Notify.cancel(ctx, Notify.ID_EVENING) }
                            Scheduler.NUDGE -> Notify.nudge(ctx)
                            Scheduler.EVENT -> {
                                val s = c.store.now()
                                val at = intent.getLongExtra(Scheduler.EXTRA_AT, now)
                                s.events.firstOrNull { it.id == intent.getLongExtra(Scheduler.EXTRA_ID, -1L) && it.on }?.let { e ->
                                    // A late alarm (the phone was asleep for long) is still worth a note, a stale one isn't.
                                    if (now - at < 3 * 60 * 60_000L) {
                                        if (e.step.kind == Kind.RADIO) RingService.radio(ctx, e.step.url, e.step.label)
                                        Notify.event(ctx, e, app.threshold.ui.Fmt.hm(e.h, e.m))
                                    }
                                }
                                // Then the one after it; never this same one again.
                                Scheduler.arm(ctx, s, maxOf(now, at) + 1)
                            }
                            Scheduler.NEXT -> {
                                val s = c.store.now()
                                val i = intent.getIntExtra(Scheduler.EXTRA_INDEX, 1)
                                val day = intent.getLongExtra(Scheduler.EXTRA_DAY, s.lastDone)
                                val steps = if (day >= 0) Week.dayOf(s, day).steps else emptyList()
                                steps.getOrNull(i)?.let { st ->
                                    if (st.kind == Kind.RADIO) RingService.radio(ctx, st.url, st.label) else Notify.next(ctx, st)
                                    if (i + 1 < steps.size) Scheduler.chain(ctx, i + 1, now + app.threshold.Debug.minutes(st.minutes), day)
                                }
                            }
                            // Boot, a new time zone, a changed clock, an update: set the alarms again.
                            else -> Scheduler.arm(ctx, c.store.now(), now)
                        }
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}

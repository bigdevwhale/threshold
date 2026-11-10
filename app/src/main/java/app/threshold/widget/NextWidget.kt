package app.threshold.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.graphics.Bitmap
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.RemoteViews
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import app.threshold.MainActivity
import app.threshold.R
import app.threshold.container
import app.threshold.data.State
import app.threshold.domain.Frame
import app.threshold.domain.Frames
import app.threshold.domain.Portal
import app.threshold.domain.Week
import app.threshold.domain.World
import app.threshold.ui.art.ArchBox
import app.threshold.ui.art.portal
import app.threshold.ru
import app.threshold.ui.Fmt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** The home-screen widget: when the portal opens next and onto which world, and what else the day holds. */
class NextWidget : AppWidgetProvider() {
    companion object {
        private val lines = intArrayOf(R.id.w_e1, R.id.w_e2, R.id.w_e3)

        /** Redraws every placed widget; cheap when there are none. */
        fun update(ctx: Context, s: State, now: Long = System.currentTimeMillis()) {
            val wm = AppWidgetManager.getInstance(ctx) ?: return
            val ids = runCatching { wm.getAppWidgetIds(ComponentName(ctx, NextWidget::class.java)) }.getOrNull() ?: return
            if (ids.isEmpty()) return
            val views = views(ctx, s, now)
            runCatching { wm.updateAppWidget(ids, views) }
        }

        fun views(ctx: Context, s: State, now: Long): RemoteViews {
            val v = RemoteViews(ctx.packageName, R.layout.widget_next)
            val ru = ctx.ru
            val today = Week.epochDay(now)
            val r = s.ring
            val next = Week.next(s, now)
            val snoozed = r != null && r.snoozeUntil > now
            val day = if (snoozed) r!!.day else next?.day
            val morning = day?.let { Portal.morning(s, it) }
            when {
                snoozed -> {
                    v.setTextViewText(R.id.w_when, ctx.getString(R.string.w_snoozed))
                    v.setTextViewText(R.id.w_time, Fmt.hm(r!!.snoozeUntil))
                }
                next != null -> {
                    v.setTextViewText(R.id.w_when, ctx.getString(R.string.w_next, Fmt.near(ctx, next.day, today)))
                    v.setTextViewText(R.id.w_time, Fmt.hm(next.at))
                }
                else -> {
                    v.setTextViewText(R.id.w_when, ctx.getString(R.string.w_none))
                    v.setTextViewText(R.id.w_time, "—")
                }
            }
            v.setTextViewText(R.id.w_world, morning?.world?.title?.of(ru).orEmpty())
            v.setTextViewText(R.id.w_what, when {
                morning == null -> ""
                morning.wild -> ctx.getString(R.string.w_wild)
                morning.steps.isEmpty() -> ctx.getString(R.string.week_plain)
                else -> morning.steps.joinToString(" → ") { it.label }
            })
            v.setViewVisibility(R.id.w_what, if (morning == null) View.GONE else View.VISIBLE)
            arch(ctx, morning?.world, Frames.of(s.frame))?.let { v.setImageViewBitmap(R.id.w_arch, it) }

            // The events of the next day and a bit: today's, then tomorrow's; the time in gold.
            val due = Week.upcoming(s, now, lines.size).filter { it.day <= today + 1 }
            v.setViewVisibility(R.id.w_events, if (due.isEmpty()) View.GONE else View.VISIBLE)
            lines.forEachIndexed { i, id ->
                val d = due.getOrNull(i)
                if (d == null) v.setViewVisibility(id, View.GONE)
                else {
                    val time = (if (d.day == today) "" else ctx.getString(R.string.tomorrow) + " ") + Fmt.hm(d.at)
                    val line = SpannableString("$time   ${d.event.step.label}")
                    line.setSpan(ForegroundColorSpan(0xFFE8C27A.toInt()), 0, time.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    v.setTextViewText(id, line)
                    v.setViewVisibility(id, View.VISIBLE)
                }
            }
            val open = PendingIntent.getActivity(ctx, 40, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            v.setOnClickPendingIntent(R.id.w_root, open)
            return v
        }

        /** The arch onto [world], drawn by the same code as in the app, still; a day off is bricked up. */
        private fun arch(ctx: Context, world: World?, frame: Frame): Bitmap? = runCatching {
            val dm = ctx.resources.displayMetrics
            val w = (72 * dm.density).toInt(); val h = (98 * dm.density).toInt()
            val img = ImageBitmap(w, h)
            CanvasDrawScope().draw(Density(dm.density), LayoutDirection.Ltr, Canvas(img), Size(w.toFloat(), h.toFloat())) {
                val aw = size.width * 0.8f
                val ah = minOf(size.height * 0.94f, aw * 1.65f)
                portal(ArchBox(size.width / 2, (size.height - ah) / 2 + aw * 0.07f, aw, ah), 4f, world, if (world != null) 1f else 0f, frame, floor = false)
            }
            img.asAndroidBitmap()
        }.getOrNull()
    }

    override fun onUpdate(ctx: Context, wm: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val views = views(ctx, ctx.container.store.now(), System.currentTimeMillis())
                runCatching { wm.updateAppWidget(ids, views) }
            } finally {
                pending.finish()
            }
        }
    }
}

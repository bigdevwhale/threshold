package app.threshold.ui

import android.content.Context
import app.threshold.R
import app.threshold.data.Kind
import app.threshold.data.Step
import app.threshold.domain.Week
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

object Fmt {
    fun locale(ctx: Context): Locale = ctx.resources.configuration.locales[0]

    fun hm(h: Int, m: Int) = "%02d:%02d".format(h, m)

    fun hm(ms: Long): String {
        val t = java.time.Instant.ofEpochMilli(ms).atZone(Week.zone())
        return hm(t.hour, t.minute)
    }

    private fun cap(s: String) = s.replaceFirstChar { it.titlecase() }

    /** «Понедельник», from 0 = Monday. */
    fun dow(ctx: Context, i: Int): String = cap(java.time.DayOfWeek.of(i + 1).getDisplayName(TextStyle.FULL_STANDALONE, locale(ctx)))

    fun dowShort(ctx: Context, i: Int): String = cap(java.time.DayOfWeek.of(i + 1).getDisplayName(TextStyle.SHORT_STANDALONE, locale(ctx)).trimEnd('.'))

    /** «сегодня», «завтра» or the weekday, for a day near [today]. */
    fun near(ctx: Context, day: Long, today: Long): String = when (day - today) {
        0L -> ctx.getString(R.string.today)
        1L -> ctx.getString(R.string.tomorrow)
        else -> dow(ctx, Week.dow(day)).let { if (locale(ctx).language == "ru") it.lowercase(locale(ctx)) else it }
    }

    fun date(ctx: Context, day: Long): String {
        val d = LocalDate.ofEpochDay(day)
        return "${d.dayOfMonth} ${d.month.getDisplayName(if (locale(ctx).language == "ru") TextStyle.FULL else TextStyle.FULL, locale(ctx))}"
            .let { if (locale(ctx).language == "ru") ruMonth(d) else it }
    }

    private val ruGen = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
    private fun ruMonth(d: LocalDate) = "${d.dayOfMonth} ${ruGen[d.monthValue - 1]}"

    fun today(ctx: Context, day: Long): String = "${dow(ctx, Week.dow(day))}, ${date(ctx, day)}"

    fun kind(ctx: Context, s: Step): String = when (s.kind) {
        Kind.APP -> ctx.getString(R.string.k_app)
        Kind.LINK -> ctx.getString(R.string.k_link)
        Kind.RADIO -> ctx.getString(R.string.k_radio)
    }
}

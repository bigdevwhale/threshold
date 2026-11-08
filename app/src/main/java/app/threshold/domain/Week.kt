package app.threshold.domain

import app.threshold.data.Day
import app.threshold.data.State
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.IsoFields

/** Why a day has no alarm. */
enum class Off { DAY_OFF, HOLIDAY, VACATION, SKIP }

/** The next time the portal opens. */
data class Next(val at: Long, val day: Long, val wild: Boolean)

object Week {
    fun zone(): ZoneId = ZoneId.systemDefault()

    fun epochDay(ms: Long, zone: ZoneId = zone()): Long = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().toEpochDay()

    /** 0 is Monday. */
    fun dow(day: Long): Int = LocalDate.ofEpochDay(day).dayOfWeek.value - 1

    fun at(day: Long, h: Int, m: Int, zone: ZoneId = zone()): Long = LocalDate.ofEpochDay(day).atTime(h, m).atZone(zone).toInstant().toEpochMilli()

    fun dayOf(s: State, day: Long): Day = s.week[dow(day)]

    // The main Russian public holidays. Each year's moved days come out by decree, so only the fixed dates
    // are here, plus the Monday after one that falls on a weekend; the rest the user marks by hand.
    private val fixed = (1..8).map { 1 to it } + listOf(2 to 23, 3 to 8, 5 to 1, 5 to 9, 6 to 12, 11 to 4)

    fun holiday(day: Long): Boolean {
        val d = LocalDate.ofEpochDay(day)
        if ((d.monthValue to d.dayOfMonth) in fixed) return true
        if (d.dayOfWeek == DayOfWeek.MONDAY) {
            val sat = d.minusDays(2); val sun = d.minusDays(1)
            return listOf(sat, sun).any { (it.monthValue to it.dayOfMonth) in fixed && it.monthValue != 1 }
        }
        return false
    }

    fun off(s: State, day: Long): Off? {
        val d = dayOf(s, day)
        return when {
            day in s.offDays -> Off.SKIP
            s.vacFrom >= 0 && day in s.vacFrom..s.vacTo -> Off.VACATION
            !d.on -> Off.DAY_OFF
            s.holidays && holiday(day) -> Off.HOLIDAY
            else -> null
        }
    }

    /**
     * The one day of a week the wild portal opens on, if it is allowed and there is something on the «someday» list:
     * one of that week's ringing days, the same one every time it is asked.
     */
    fun wildDay(s: State, day: Long): Boolean {
        if (!s.wild || s.wishes.isEmpty()) return false
        val d = LocalDate.ofEpochDay(day)
        val monday = d.with(DayOfWeek.MONDAY).toEpochDay()
        val ringing = (0L..6L).map { monday + it }.filter { off(s, it) == null }
        if (ringing.isEmpty()) return false
        val week = d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) + d.get(IsoFields.WEEK_BASED_YEAR) * 53
        return ringing[Math.floorMod(week * 7919 + 13, ringing.size)] == day
    }

    fun next(s: State, now: Long): Next? {
        val today = epochDay(now)
        for (i in 0L..21L) {
            val day = today + i
            if (day <= s.lastDone) continue
            if (off(s, day) != null) continue
            val d = dayOf(s, day)
            val at = at(day, d.h, d.m)
            if (at <= now) continue
            return Next(at, day, wildDay(s, day))
        }
        return null
    }

    /** The bedtime nudge: eight and a half hours before the portal opens. */
    fun bedtime(next: Next): Long = next.at - (8 * 60 + 30) * 60_000L
}

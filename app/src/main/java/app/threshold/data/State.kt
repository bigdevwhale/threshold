package app.threshold.data

import app.threshold.domain.World

const val SEC = 1_000L
const val MIN = 60 * SEC
const val HOUR = 60 * MIN
const val DAY = 24 * HOUR

/**
 * What a step opens: another app, a link (in its app when there is one), a radio stream played here,
 * or an audio or video file from the phone (in the player Android picks).
 */
enum class Kind { APP, LINK, RADIO, FILE }

data class Step(
    val kind: Kind,
    val label: String,
    /** The app to open, or to open the link in; empty lets Android choose. For a file, its type (audio/mpeg…). */
    val pkg: String = "",
    val url: String = "",
    /** How long this step lasts before the portal leads on to the next one (chains only). */
    val minutes: Int = 15,
)

/** One day of the week: when the portal opens, which world it opens onto and what comes through. */
data class Day(
    val on: Boolean = true,
    val h: Int = 7,
    val m: Int = 0,
    val world: World = World.MUSIC,
    val steps: List<Step> = emptyList(),
    /** Open the morning at once, with no circle and no tap: the phone only asks to be unlocked. */
    val auto: Boolean = false,
    /** The world was chosen by hand: adding a step no longer guesses it. */
    val picked: Boolean = false,
)

/**
 * Something else the day holds, apart from the morning: at [h]:[m] on the [days] of the week (0 is Monday) a
 * notification comes, and a tap opens the [step]. A radio step starts playing by itself.
 */
data class Event(
    val id: Long,
    val days: Set<Int>,
    val h: Int,
    val m: Int,
    val step: Step,
    val on: Boolean = true,
)

/** A morning that happened: how it went, for the journal. */
data class Wake(
    val at: Long,
    val world: World,
    val snoozes: Int,
    /** Seconds from the first ring to the step through. */
    val secs: Int,
    val finds: List<String>,
    /** -1 not said, 0 hard, 1 so-so, 2 fresh. */
    val mood: Int = -1,
    val wild: Boolean = false,
)

/** The alarm that is ringing or snoozed right now. */
data class Ring(
    /** The day (epoch day) this morning belongs to. */
    val day: Long,
    val start: Long,
    val snoozes: Int = 0,
    val snoozeUntil: Long = 0,
    /** A test ring from the settings: nothing is written to the journal. */
    val test: Boolean = false,
)

data class State(
    val week: List<Day> = Defaults.week,
    val frame: String = "stone",
    /** When the day was closed (the evening touch on the arch); charges the portal for the next morning. */
    val closedAt: Long = 0,
    val snoozeMin: Int = 5,
    val maxSnooze: Int = 2,
    val rampSec: Int = 60,
    val vibrate: Boolean = true,
    val holidays: Boolean = true,
    /** Single days without the alarm (epoch days): «не будить завтра», days off by hand. */
    val offDays: Set<Long> = emptySet(),
    val vacFrom: Long = -1,
    val vacTo: Long = -1,
    val evening: Boolean = true,
    /** Minutes after the step when a gentle «пора» arrives; 0 is off. */
    val nudgeMin: Int = 0,
    val wild: Boolean = false,
    val wishes: List<Step> = emptyList(),
    /** The day's other events: the evening book, the workout, the film. */
    val events: List<Event> = emptyList(),
    val visits: Map<World, Int> = emptyMap(),
    val finds: Map<String, Int> = emptyMap(),
    val log: List<Wake> = emptyList(),
    val ring: Ring? = null,
    /** The last morning stepped through (epoch day), so that day never rings twice. */
    val lastDone: Long = -1,
    val introDone: Boolean = false,
)

object Defaults {
    val week = listOf(
        Day(true, 7, 0, World.NEWS),
        Day(true, 7, 0, World.MUSIC),
        Day(true, 7, 0, World.SPORT),
        Day(true, 7, 0, World.RADIO),
        Day(true, 7, 0, World.CINEMA),
        Day(false, 9, 0, World.GARDEN),
        Day(false, 9, 30, World.GARDEN),
    )
}

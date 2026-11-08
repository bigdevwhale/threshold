package app.threshold.domain

import app.threshold.data.HOUR
import app.threshold.data.Kind
import app.threshold.data.State
import app.threshold.data.Step
import kotlin.random.Random

/** What a morning opens onto: the day's world and steps, or the wild world with one wish. */
data class Morning(val world: World, val steps: List<Step>, val wild: Boolean, val auto: Boolean = false)

object Portal {
    private val levels = intArrayOf(0, 3, 8, 16, 30)

    fun level(visits: Int): Int = levels.count { visits >= it }

    /** Mornings until the next level, or null at the top. */
    fun toNext(visits: Int): Int? = levels.firstOrNull { it > visits }?.let { it - visits }

    /**
     * How charged the portal is for a morning at [at]: the day closed in the evening before it, and how long before.
     * Eight hours' notice is full charge.
     */
    fun charge(closedAt: Long, at: Long): Int {
        if (closedAt <= 0 || closedAt > at || at - closedAt > 16 * HOUR) return 0
        return ((at - closedAt) * 100 / (8 * HOUR)).toInt().coerceIn(1, 100)
    }

    /** A wide portal (seven hours' notice and no snoozing) lets two finds through; one snooze, one; more, none. */
    fun count(charge: Int, snoozes: Int): Int = when {
        snoozes >= 2 -> 0
        snoozes == 1 -> 1
        charge >= 87 -> 2
        else -> 1
    }

    fun morning(s: State, day: Long): Morning {
        val d = Week.dayOf(s, day)
        if (Week.wildDay(s, day)) s.wishes.firstOrNull()?.let { return Morning(World.WILD, listOf(it), true, d.auto) }
        return Morning(d.world, d.steps, false, d.auto)
    }

    /** Picks [n] finds from [world]: something new first, now and then a rare one; once all are found, any. */
    fun roll(owned: Map<String, Int>, world: World, n: Int, rnd: Random = Random): List<Find> {
        val got = mutableListOf<Find>()
        val pool = Finds.of(world)
        repeat(n) {
            val have = owned.keys + got.map { it.key }
            val common = pool.filter { !it.rare && it.key !in have }
            val rare = pool.filter { it.rare && it.key !in have }
            val f = when {
                rare.isNotEmpty() && (common.isEmpty() || rnd.nextFloat() < 0.2f) -> rare.random(rnd)
                common.isNotEmpty() -> common.random(rnd)
                else -> pool.random(rnd)
            }
            got += f
        }
        return got
    }

    fun distinct(s: State) = s.finds.count { it.value > 0 }

    fun frameOpen(s: State, f: Frame) = distinct(s) >= f.need

    /** The world a step most likely belongs to, from where it leads. */
    fun guess(step: Step): World {
        if (step.kind == Kind.RADIO) return World.RADIO
        val text = (step.url + " " + step.pkg + " " + step.label).lowercase()
        fun has(vararg w: String) = w.any { it in text }
        return when {
            has("зарядк", "йог", "workout", "fitness", "yoga", "stretch", "растяжк", "тренир", "пилатес", "pilates") -> World.SPORT
            has("новост", "news", "meduza", "rbc", "tass", "lenta", "дайджест", "digest") -> World.NEWS
            has("kinopoisk", "кинопоиск", "ivi", "okko", "netflix", "kion", "wink", "start.ru", "кино", "film", "movie", "serial", "сериал") -> World.CINEMA
            has("iptv", "tv", "тв", "radio", "радио", "эфир", "stream") -> World.RADIO
            has("podcast", "подкаст", "audiobook", "аудиокниг", "storytel", "litres", "литрес", "bookmate", "букмейт", "книг") -> World.GARDEN
            has("music", "музык", "spotify", "soundcloud", "zvuk", "deezer", "playlist", "плейлист") -> World.MUSIC
            has("youtube", "youtu.be") -> World.NEWS
            else -> World.MUSIC
        }
    }
}

/** The morning journal, summed up. */
data class Summary(
    val wakes: Int,
    /** Mornings with no snooze, in per cent. */
    val firstRing: Int,
    val avgSecs: Int,
    /** Mornings in a row without a snooze, up to today. */
    val streak: Int,
    val best: World?,
    val worst: World?,
    val moodBy: Map<World, Double>,
    val moods: Int,
)

object Journal {
    fun sum(s: State): Summary {
        val log = s.log.filter { it.at > 0 }
        val n = log.size
        val first = if (n == 0) 0 else log.count { it.snoozes == 0 } * 100 / n
        val avg = if (n == 0) 0 else log.sumOf { it.secs } / n
        val streak = log.sortedByDescending { it.at }.takeWhile { it.snoozes == 0 }.size
        val rated = log.filter { it.mood >= 0 }
        val by = rated.groupBy { it.world }.filterValues { it.size >= 3 }.mapValues { (_, v) -> v.map { it.mood }.average() }
        val best = by.maxByOrNull { it.value }?.key
        val worst = by.minByOrNull { it.value }?.key?.takeIf { it != best && (by[best] ?: 0.0) - (by[it] ?: 0.0) >= 0.4 }
        return Summary(n, first, avg, streak, best, worst, by, rated.size)
    }
}

package app.threshold

import app.threshold.data.DemoSeed
import app.threshold.data.HOUR
import app.threshold.data.Json
import app.threshold.data.Kind
import app.threshold.data.State
import app.threshold.data.Step
import app.threshold.domain.Finds
import app.threshold.domain.Off
import app.threshold.domain.Portal
import app.threshold.domain.Week
import app.threshold.domain.World
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import kotlin.random.Random

class WeekTest {
    private fun day(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).toEpochDay()

    @Test fun nextSkipsDaysOffAndPastTimes() {
        val s = State()
        // Friday 6 Nov 2026, 08:00: Friday's 07:00 is gone, the weekend is off, so Monday 07:00.
        val now = Week.at(day(2026, 11, 6), 8, 0)
        val n = Week.next(s, now)!!
        assertEquals(day(2026, 11, 9), n.day)
        assertEquals(Week.at(day(2026, 11, 9), 7, 0), n.at)
    }

    @Test fun skippedAndDoneDaysDontRing() {
        val now = Week.at(day(2026, 11, 9), 6, 0)
        val s = State(offDays = setOf(day(2026, 11, 9)))
        assertEquals(day(2026, 11, 10), Week.next(s, now)!!.day)
        val done = State(lastDone = day(2026, 11, 9))
        assertEquals(day(2026, 11, 10), Week.next(done, now)!!.day)
    }

    @Test fun holidaysAndVacation() {
        assertTrue(Week.holiday(day(2027, 1, 4)))
        assertTrue(Week.holiday(day(2026, 11, 4)))
        // 23 February 2025 was a Sunday: Monday the 24th is off.
        assertTrue(Week.holiday(day(2025, 2, 24)))
        assertFalse(Week.holiday(day(2026, 11, 5)))
        val s = State(vacFrom = day(2026, 11, 9), vacTo = day(2026, 11, 13))
        assertEquals(Off.VACATION, Week.off(s, day(2026, 11, 11)))
        assertEquals(day(2026, 11, 16), Week.next(s, Week.at(day(2026, 11, 8), 12, 0))!!.day)
        assertNull(Week.next(State(week = State().week.map { it.copy(on = false) }), 0L))
    }

    @Test fun wildOncePerWeek() {
        val wish = Step(Kind.LINK, "x", url = "https://example.com")
        val s = State(wild = true, wishes = listOf(wish))
        val monday = day(2026, 11, 9)
        val wild = (0L..6L).count { Week.wildDay(s, monday + it) }
        assertEquals(1, wild)
        val d = (0L..6L).first { Week.wildDay(s, monday + it) }
        assertEquals(World.WILD, Portal.morning(s, monday + d).world)
        assertFalse(Week.wildDay(s.copy(wishes = emptyList()), monday + d))
    }

    @Test fun chargeAndFinds() {
        val at = 1_000_000_000_000L
        assertEquals(0, Portal.charge(0, at))
        assertEquals(100, Portal.charge(at - 9 * HOUR, at))
        assertEquals(50, Portal.charge(at - 4 * HOUR, at))
        assertEquals(0, Portal.charge(at - 20 * HOUR, at))
        assertEquals(2, Portal.count(100, 0))
        assertEquals(1, Portal.count(40, 0))
        assertEquals(1, Portal.count(100, 1))
        assertEquals(0, Portal.count(100, 2))
        // New finds come first: six rolls in one world find all six.
        var owned = emptyMap<String, Int>()
        repeat(6) { Portal.roll(owned, World.MUSIC, 1, Random(it)).forEach { f -> owned = owned + (f.key to 1) } }
        assertEquals(Finds.of(World.MUSIC).map { it.key }.toSet(), owned.keys)
    }

    @Test fun linksThatPlay() {
        val n = app.threshold.alarm.Launch::normalize
        assertEquals("spotify:playlist:37i9dQZF1DXcBWIGoYBM5M:play", n("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?si=abc"))
        assertEquals("spotify:album:4aawyAB9vmqN3uQ7FjRGTy:play", n("https://open.spotify.com/intl-ru/album/4aawyAB9vmqN3uQ7FjRGTy"))
        assertEquals("https://open.spotify.com/track/4cOdK2wGLETKBW3PvgPWqT", n("https://open.spotify.com/track/4cOdK2wGLETKBW3PvgPWqT"))
        assertEquals("https://music.yandex.ru/album/9294859?play=true", n("https://music.yandex.ru/album/9294859"))
        assertEquals("https://music.yandex.ru/users/a/playlists/3?utm=x&play=true", n("https://music.yandex.ru/users/a/playlists/3?utm=x"))
        assertEquals("https://www.kinopoisk.ru/film/435/", n("https://hd.kinopoisk.ru/film/435"))
        assertEquals("https://meduza.io/", n("https://meduza.io/"))
    }

    @Test fun guessesWorlds() {
        assertEquals(World.SPORT, Portal.guess(Step(Kind.LINK, "YouTube · Зарядка", url = "https://youtube.com/x")))
        assertEquals(World.CINEMA, Portal.guess(Step(Kind.LINK, "", url = "https://hd.kinopoisk.ru/film/1")))
        assertEquals(World.MUSIC, Portal.guess(Step(Kind.LINK, "", url = "https://music.yandex.ru/album/1")))
        assertEquals(World.RADIO, Portal.guess(Step(Kind.RADIO, "Jazz", url = "https://x/stream")))
        assertEquals(World.GARDEN, Portal.guess(Step(Kind.LINK, "YouTube · Аудиокнига «Мастер и Маргарита»", url = "https://youtu.be/abc")))
        assertEquals(World.SPORT, Portal.guess(Step(Kind.LINK, "YouTube · Йога утром", url = "https://youtu.be/abc")))
        // A bare video says nothing about its world: no guess, rather than «news».
        assertNull(Portal.guess(Step(Kind.LINK, "YouTube", url = "https://youtu.be/abc")))
    }

    // Issue #3: a world chosen by hand stayed only until the first step was added.
    @Test fun aPickedWorldStays() {
        val yoga = Step(Kind.LINK, "YouTube", url = "https://youtu.be/abc")
        val book = Step(Kind.LINK, "YouTube · Аудиокнига", url = "https://youtu.be/abc")
        val picked = app.threshold.data.Day(world = World.GARDEN, picked = true)
        assertEquals(World.GARDEN, Portal.place(picked, Step(Kind.LINK, "YouTube · Новости", url = "https://youtu.be/x")).world)
        // Not picked: a telling first step chooses, a bare one leaves the world as it was.
        val fresh = app.threshold.data.Day(world = World.CINEMA)
        assertEquals(World.GARDEN, Portal.place(fresh, book).world)
        assertEquals(World.CINEMA, Portal.place(fresh, yoga).world)
        assertEquals(1, Portal.place(fresh, yoga).steps.size)
    }

    @Test fun eventsComeInOrderAndIgnoreTheMorningsDaysOff() {
        val read = app.threshold.data.Event(1, setOf(0, 1, 2, 3, 4, 5, 6), 20, 0, Step(Kind.APP, "Books", "ru.yandex.books"))
        val fast = app.threshold.data.Event(2, setOf(5), 6, 0, Step(Kind.APP, "Ember", "x.ember"))
        val off = app.threshold.data.Event(3, setOf(5), 7, 0, Step(Kind.APP, "Off"), on = false)
        // Friday 6 Nov 2026, 21:00, with a vacation over the weekend.
        val s = State(events = listOf(read, fast, off), vacFrom = day(2026, 11, 7), vacTo = day(2026, 11, 8))
        val up = Week.upcoming(s, Week.at(day(2026, 11, 6), 21, 0), 3)
        assertEquals(listOf(2L, 1L, 1L), up.map { it.event.id })
        assertEquals(Week.at(day(2026, 11, 7), 6, 0), up[0].at)
        assertEquals(Week.at(day(2026, 11, 7), 20, 0), up[1].at)
        assertEquals(listOf(fast, off), Week.events(s, 5).filter { it.id != 1L })
        assertTrue(Week.upcoming(State(), 0).isEmpty())
    }
}

/** org.json is Android's, so the store round trip runs under Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StoreTest {
    private fun day(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).toEpochDay()

    @Test fun jsonRoundTrip() {
        val s = DemoSeed.seeded(Week.at(day(2026, 11, 6), 22, 0), true)
        val back = Json.read(Json.write(s))
        assertEquals(s.week, back.week)
        assertEquals(s.finds, back.finds)
        assertEquals(s.visits, back.visits)
        assertEquals(s.log, back.log)
        assertEquals(s.wishes, back.wishes)
        val e = State(
            events = listOf(app.threshold.data.Event(7, setOf(0, 4), 20, 30, Step(Kind.FILE, "Йога", "video/mp4", "content://x/1"), on = false)),
            week = s.week.mapIndexed { i, d -> if (i == 2) d.copy(picked = true) else d },
        )
        val back2 = Json.read(Json.write(e))
        assertEquals(e.events, back2.events)
        assertEquals(e.week, back2.week)
        assertNotNull(back.log.firstOrNull())
    }
}

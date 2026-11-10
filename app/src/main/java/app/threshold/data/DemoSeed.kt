package app.threshold.data

import app.threshold.AppContainer
import app.threshold.domain.Portal
import app.threshold.domain.Week
import app.threshold.domain.World
import kotlin.random.Random

/** A lived-in week for the demo build and the screen renders: the morning plan from the idea, two months of mornings. */
object DemoSeed {
    suspend fun seedIfEmpty(c: AppContainer) {
        if (c.store.now().log.isNotEmpty()) return
        val s = seeded(System.currentTimeMillis(), c.app.resources.configuration.locales[0].language == "ru")
        c.store.update { s }
    }

    fun week(ru: Boolean): List<Day> {
        fun t(en: String, r: String) = if (ru) r else en
        return listOf(
            Day(true, 6, 30, World.NEWS, listOf(Step(Kind.LINK, t("YouTube · Morning news", "YouTube · Новости дня"), "com.google.android.youtube", "https://www.youtube.com/@rbc/videos", 20))),
            Day(true, 6, 30, World.MUSIC, listOf(Step(Kind.LINK, t("Yandex Music · Brisk morning", "Яндекс Музыка · Бодрое утро"), "ru.yandex.music", "https://music.yandex.ru/users/music-blog/playlists/2207", 30))),
            Day(true, 6, 30, World.SPORT, listOf(
                Step(Kind.LINK, t("YouTube · Exercise, 15 min", "YouTube · Зарядка, 15 минут"), "com.google.android.youtube", "https://www.youtube.com/results?search_query=зарядка+15+минут", 15),
                Step(Kind.APP, t("IPTV · morning show", "IPTV · утренний эфир"), "ru.iptvremote.android.iptv", "", 20),
            )),
            Day(true, 6, 30, World.RADIO, listOf(Step(Kind.RADIO, "Radio Swiss Jazz", "", "https://stream.srg-ssr.ch/m/rsj/mp3_128", 30))),
            Day(true, 6, 30, World.CINEMA, listOf(Step(Kind.LINK, t("Kinopoisk · The Green Mile", "Кинопоиск · Зелёная миля"), "ru.kinopoisk", "https://www.kinopoisk.ru/film/435/", 30))),
            Day(true, 9, 0, World.GARDEN, listOf(Step(Kind.LINK, t("Podcast · Slow morning", "Подкаст · Неспешное утро"), "ru.yandex.music", "https://music.yandex.ru/album/9294859", 40))),
            Day(false, 10, 0, World.GARDEN),
        )
    }

    fun seeded(now: Long, ru: Boolean): State {
        val rnd = Random(7)
        val today = Week.epochDay(now)
        var s = State(week = week(ru), frame = "brass", wild = true, nudgeMin = 20,
            wishes = listOf(
                Step(Kind.LINK, if (ru) "Альбом · Ronny Jordan" else "Album · Ronny Jordan", "", "https://music.yandex.ru/album/31155"),
                Step(Kind.LINK, if (ru) "Фильм · Амели" else "Film · Amélie", "", "https://www.kinopoisk.ru/film/450/"),
            ),
            events = listOf(
                Event(1, (0..4).toSet(), 19, 0, Step(Kind.LINK, if (ru) "YouTube · Йога для начинающих" else "YouTube · Yoga for beginners", "com.google.android.youtube", "https://www.youtube.com/results?search_query=yoga")),
                Event(2, (0..6).toSet(), 23, 0, Step(Kind.APP, if (ru) "Яндекс Книги" else "Yandex Books", "ru.yandex.books")),
            ))
        val log = mutableListOf<Wake>()
        for (back in 60 downTo 1) {
            val day = today - back
            val d = Week.dayOf(s, day)
            if (!d.on) continue
            val at = Week.at(day, d.h, d.m)
            val sn = when (rnd.nextInt(10)) { in 0..6 -> 0; 7, 8 -> 1; else -> 2 }
            val finds = Portal.roll(s.finds, d.world, Portal.count(if (rnd.nextInt(3) > 0) 95 else 40, sn), rnd)
            var owned = s.finds
            finds.forEach { f -> owned = owned + (f.key to (owned[f.key] ?: 0) + 1) }
            val mood = when (d.world) {
                World.SPORT -> listOf(2, 2, 1).random(rnd)
                World.NEWS -> listOf(1, 0, 1, 0).random(rnd)
                World.MUSIC -> listOf(2, 1, 2).random(rnd)
                else -> listOf(2, 1, 1, -1).random(rnd)
            }
            val secs = 20 + sn * s.snoozeMin * 60 + rnd.nextInt(90)
            log.add(0, Wake(at + secs * 1000L, d.world, sn, secs, finds.map { it.key }, mood))
            s = s.copy(finds = owned, visits = s.visits + (d.world to (s.visits[d.world] ?: 0) + 1), lastDone = day)
        }
        // Three trips into the wild along the way.
        s = s.copy(visits = s.visits + (World.WILD to 3), finds = s.finds + ("wild_key" to 1) + ("wild_map" to 1))
        // The latest mornings went without a snooze: a streak to show.
        val fixed = log.mapIndexed { i, w -> if (i < 6) w.copy(snoozes = 0) else w }
        return s.copy(log = fixed, closedAt = now - 30 * MIN, introDone = true)
    }
}

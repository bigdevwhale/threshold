package app.threshold

import app.threshold.ui.week.firstUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** What counts as a link in the link form (#4). */
class LinkTest {
    @Test fun withScheme() {
        assertEquals("https://youtu.be/abc", firstUrl("Смотри https://youtu.be/abc."))
        assertEquals("http://example.com/a", firstUrl("http://example.com/a"))
    }

    @Test fun bareAddressGetsHttps() {
        assertEquals("https://music.yandex.ru/album/123", firstUrl("music.yandex.ru/album/123"))
        assertEquals("https://youtu.be/xyz?t=5", firstUrl("  youtu.be/xyz?t=5 "))
        assertEquals("https://kinopoisk.ru", firstUrl("kinopoisk.ru"))
    }

    @Test fun notAnAddress() {
        assertNull(firstUrl(""))
        assertNull(firstUrl("Просто текст"))
        assertNull(firstUrl("song.mp3"))
        assertNull(firstUrl("два слова.ru"))
        assertNull(firstUrl("me@mail.ru"))
    }
}

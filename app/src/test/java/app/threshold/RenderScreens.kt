package app.threshold

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import app.threshold.alarm.AlarmScreen
import app.threshold.alarm.Phase
import app.threshold.data.DemoSeed
import app.threshold.data.MIN
import app.threshold.data.Ring
import app.threshold.data.State
import app.threshold.domain.Finds
import app.threshold.domain.Portal
import app.threshold.domain.Week
import app.threshold.domain.World
import app.threshold.ui.Overlay
import app.threshold.ui.Root
import app.threshold.ui.Tab
import app.threshold.ui.art.Art
import app.threshold.ui.theme.ThresholdTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/** The main screens over the demo week, drawn to PNGs (run with `-Prender --tests '*RenderScreens*'`), in both languages. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }

    @Before fun still() { Art.still = true }

    /** A Thursday evening, so «завтра» is Friday's cinema. */
    private val day = LocalDate.of(2026, 11, 5).toEpochDay()
    private val evening = Week.at(day, 22, 40)

    private fun shot(name: String, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { ThresholdTheme { content() } }
        rule.mainClock.advanceTimeBy(1_500)
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(500)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun demo(): State = DemoSeed.seeded(evening, lang == "ru").copy(closedAt = evening - 20 * MIN)

    private fun screen(name: String, tab: Tab, overlay: Overlay? = null, world: World? = null, edit: (State) -> State = { it }) {
        val s = edit(demo())
        shot(name) { Root(s, evening, tab, overlay, world) }
    }

    @Test fun home() = screen("1-home", Tab.HOME)
    @Test fun week() = screen("2-week", Tab.WEEK)
    @Test fun worlds() = screen("3-worlds", Tab.WORLDS)
    @Test fun finds() = screen("4-finds", Tab.FINDS)
    @Test fun day() = screen("5-day", Tab.WEEK, Overlay.Day(2))
    @Test fun world() = screen("6-world", Tab.WORLDS, world = World.SPORT)
    @Test fun settings() = screen("7-settings", Tab.HOME, Overlay.Settings)
    @Test fun event() {
        val s = demo()
        shot("11-event") { app.threshold.ui.week.EventPage(s.events.first(), {}, app.threshold.ui.Nav({}, {}, {})) }
    }
    @Test fun dayEvents() = screen("12-day-events", Tab.WEEK, Overlay.Day(6))

    private fun ringing(): State {
        val s = demo()
        val friday = day + 1
        return s.copy(ring = Ring(friday, Week.at(friday, 6, 30)), lastDone = day)
    }

    /** The launcher icon at 512 px, for the README and the site. */
    @Test fun icon() {
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val d = androidx.core.content.ContextCompat.getDrawable(ctx, R.mipmap.ic_launcher)!!
        val image = android.graphics.Bitmap.createBitmap(512, 512, android.graphics.Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(image)
        val clip = android.graphics.Path().apply { addRoundRect(0f, 0f, 512f, 512f, 112f, 112f, android.graphics.Path.Direction.CW) }
        c.clipPath(clip)
        d.setBounds(0, 0, 512, 512)
        d.draw(c)
        File(out.parentFile, "icon.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun alarm() { val s = ringing(); shot("8-alarm") { AlarmScreen(s, null, Phase.HOLDING, 3.6f) } }
    @Test fun alarmReady() { val s = ringing(); shot("9-alarm-ready") { AlarmScreen(s, null, Phase.READY, 6.3f) } }
    @Test fun result() {
        val s = ringing()
        val m = Portal.morning(s, day + 1)
        val st = Stepped(m.world, m.steps, listOf(Finds.byKey.getValue("cinema_popcorn"), Finds.byKey.getValue("cinema_reel")), evening, false, false, 0)
        shot("10-result") { AlarmScreen(s, null, Phase.RESULT, stepped0 = st) }
    }
}

@Config(sdk = [34], qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderScreensRu : ScreensBase() { override val lang = "ru" }

@Config(sdk = [34], qualifiers = "en-w411dp-h891dp-xxhdpi")
class RenderScreensEn : ScreensBase() { override val lang = "en" }

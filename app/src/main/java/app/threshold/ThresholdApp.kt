package app.threshold

import android.app.Application
import android.content.Context
import app.threshold.alarm.Notify
import app.threshold.alarm.Scheduler
import app.threshold.data.DemoSeed
import app.threshold.data.State
import app.threshold.data.Step
import app.threshold.data.Store
import app.threshold.data.Wake
import app.threshold.domain.Find
import app.threshold.domain.Portal
import app.threshold.domain.Week
import app.threshold.domain.World
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** What came through the portal this morning, for the alarm screen. */
data class Stepped(val world: World, val steps: List<Step>, val finds: List<Find>, val at: Long, val test: Boolean, val wild: Boolean, val snoozes: Int, val day: Long = -1)

/** Debug builds only: a chain step can last seconds instead of minutes, to test it without waiting. */
object Debug {
    @Volatile var chainSec = 0
    fun minutes(m: Int): Long = if (BuildConfig.DEBUG && chainSec > 0) chainSec * 1000L else m.coerceAtLeast(1) * 60_000L
}

class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val store = Store(app)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** True while the demo build fills the week, so the first screen doesn't jump. */
    val seeding = MutableStateFlow(BuildConfig.DEMO)

    /** A link shared into the app from another one, waiting to be put somewhere. */
    val shared = MutableStateFlow<String?>(null)

    fun write(block: suspend AppContainer.() -> Unit) { scope.launch { block() } }

    suspend fun closeDay(now: Long = System.currentTimeMillis()) = store.update { it.copy(closedAt = now) }

    suspend fun reopenDay() = store.update { it.copy(closedAt = 0) }

    /** «Не будить» for one day, or take it back. */
    suspend fun skip(day: Long, skip: Boolean) = store.update { s ->
        val today = Week.epochDay(System.currentTimeMillis())
        val kept = s.offDays.filter { it >= today - 1 }.toSet()
        s.copy(offDays = if (skip) kept + day else kept - day)
    }

    suspend fun setDay(i: Int, change: (app.threshold.data.Day) -> app.threshold.data.Day) =
        store.update { s -> s.copy(week = s.week.mapIndexed { j, d -> if (j == i) change(d) else d }) }

    /**
     * The step through the portal: the morning is written down with what fell out of the world,
     * and the chain and the «пора» are set.
     */
    suspend fun step(now: Long = System.currentTimeMillis()): Stepped? {
        var out: Stepped? = null
        store.update { s ->
            val r = s.ring ?: return@update s
            val m = Portal.morning(s, r.day)
            if (r.test) {
                out = Stepped(m.world, m.steps, emptyList(), now, true, m.wild, r.snoozes)
                return@update s.copy(ring = null)
            }
            val charge = Portal.charge(s.closedAt, r.start)
            val finds = Portal.roll(s.finds, m.world, Portal.count(charge, r.snoozes))
            out = Stepped(m.world, m.steps, finds, now, false, m.wild, r.snoozes, r.day)
            val wake = Wake(now, m.world, r.snoozes, ((now - r.start) / 1000).toInt().coerceAtLeast(0), finds.map { it.key }, wild = m.wild)
            var owned = s.finds
            finds.forEach { f -> owned = owned + (f.key to (owned[f.key] ?: 0) + 1) }
            s.copy(
                ring = null,
                lastDone = maxOf(s.lastDone, r.day),
                visits = s.visits + (m.world to (s.visits[m.world] ?: 0) + 1),
                finds = owned,
                log = listOf(wake) + s.log,
                closedAt = 0,
                wishes = if (m.wild) s.wishes.drop(1) else s.wishes,
            )
        }
        val o = out ?: return null
        Notify.cancel(app, Notify.ID_SNOOZED)
        if (!o.test) {
            Scheduler.cancelMorning(app)
            val s = store.now()
            if (o.steps.size > 1 && !o.wild) Scheduler.chain(app, 1, now + Debug.minutes(o.steps[0].minutes), o.day)
            if (s.nudgeMin > 0) Scheduler.nudge(app, now + Debug.minutes(s.nudgeMin))
        }
        return o
    }

    /** How the morning felt, said on the alarm screen. */
    suspend fun mood(at: Long, mood: Int) = store.update { s -> s.copy(log = s.log.map { if (it.at == at) it.copy(mood = mood) else it }) }

    suspend fun addWish(step: Step) = store.update { it.copy(wishes = it.wishes + step) }
}

class ThresholdApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.LANG.isNotEmpty()) androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.forLanguageTags(BuildConfig.LANG))
        container = AppContainer(this)
        Notify.channels(this)
        // Every change to the week sets the alarms again.
        container.scope.launch {
            container.store.state.distinctUntilChanged().collect { s: State -> Scheduler.arm(this@ThresholdApp, s) }
        }
        if (BuildConfig.DEMO) container.scope.launch { try { DemoSeed.seedIfEmpty(container) } finally { container.seeding.value = false } }
    }
}

val Context.container: AppContainer get() = (applicationContext as ThresholdApp).container

val Context.ru: Boolean get() = resources.configuration.locales[0].language == "ru"

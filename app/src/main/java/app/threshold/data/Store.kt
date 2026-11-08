package app.threshold.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.threshold.domain.World
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore("threshold")
private val KEY = stringPreferencesKey("state")

/** The whole week, the finds and the journal as one JSON document, edited atomically by the app, the alarm and the receivers. */
class Store(context: Context) {
    private val store = context.applicationContext.dataStore

    val state: Flow<State> = store.data.map { p -> p[KEY]?.let { runCatching { Json.read(it) }.getOrNull() } ?: State() }

    suspend fun now(): State = state.first()

    suspend fun update(change: (State) -> State) {
        store.edit { p ->
            val s = p[KEY]?.let { runCatching { Json.read(it) }.getOrNull() } ?: State()
            p[KEY] = Json.write(change(s))
        }
    }
}

object Json {
    private fun step(s: Step) = JSONObject().put("k", s.kind.name).put("l", s.label).put("p", s.pkg).put("u", s.url).put("m", s.minutes)
    private fun step(o: JSONObject) = Step(
        runCatching { Kind.valueOf(o.getString("k")) }.getOrDefault(Kind.LINK),
        o.optString("l"), o.optString("p"), o.optString("u"), o.optInt("m", 15),
    )

    private fun world(s: String?) = World.entries.firstOrNull { it.name == s } ?: World.MUSIC

    fun write(s: State): String = JSONObject().apply {
        put("week", JSONArray().also { a ->
            s.week.forEach { d ->
                a.put(JSONObject().put("on", d.on).put("auto", d.auto).put("h", d.h).put("m", d.m).put("w", d.world.name)
                    .put("s", JSONArray().also { st -> d.steps.forEach { st.put(step(it)) } }))
            }
        })
        put("frame", s.frame); put("closed", s.closedAt)
        put("snMin", s.snoozeMin); put("snMax", s.maxSnooze); put("ramp", s.rampSec); put("vib", s.vibrate)
        put("hol", s.holidays); put("off", JSONArray(s.offDays.sorted().takeLast(200))); put("vacFrom", s.vacFrom); put("vacTo", s.vacTo)
        put("eve", s.evening); put("nudge", s.nudgeMin); put("wild", s.wild)
        put("wishes", JSONArray().also { a -> s.wishes.forEach { a.put(step(it)) } })
        put("visits", JSONObject().also { o -> s.visits.forEach { (k, v) -> o.put(k.name, v) } })
        put("finds", JSONObject().also { o -> s.finds.forEach { (k, v) -> o.put(k, v) } })
        put("log", JSONArray().also { a ->
            s.log.take(1000).forEach { w ->
                a.put(JSONObject().put("at", w.at).put("w", w.world.name).put("sn", w.snoozes).put("s", w.secs)
                    .put("f", JSONArray(w.finds)).put("mood", w.mood).put("wild", w.wild))
            }
        })
        s.ring?.let { r -> put("ring", JSONObject().put("d", r.day).put("st", r.start).put("sn", r.snoozes).put("until", r.snoozeUntil).put("test", r.test)) }
        put("done", s.lastDone); put("intro", s.introDone)
    }.toString()

    fun read(text: String): State {
        val o = JSONObject(text)
        val week = o.optJSONArray("week")?.let { a ->
            (0 until a.length()).map { i ->
                val d = a.getJSONObject(i)
                Day(d.optBoolean("on", true), d.optInt("h", 7), d.optInt("m"), world(d.optString("w")),
                    d.optJSONArray("s")?.let { st -> (0 until st.length()).map { step(st.getJSONObject(it)) } } ?: emptyList(),
                    d.optBoolean("auto"))
            }
        }?.takeIf { it.size == 7 } ?: Defaults.week
        val log = o.optJSONArray("log")?.let { a ->
            (0 until a.length()).map { i ->
                val w = a.getJSONObject(i)
                Wake(w.getLong("at"), world(w.optString("w")), w.optInt("sn"), w.optInt("s"),
                    w.optJSONArray("f")?.let { f -> (0 until f.length()).map { f.getString(it) } } ?: emptyList(),
                    w.optInt("mood", -1), w.optBoolean("wild"))
            }
        } ?: emptyList()
        val ring = o.optJSONObject("ring")?.let { Ring(it.getLong("d"), it.getLong("st"), it.optInt("sn"), it.optLong("until"), it.optBoolean("test")) }
        return State(
            week = week,
            frame = o.optString("frame", "stone"),
            closedAt = o.optLong("closed"),
            snoozeMin = o.optInt("snMin", 5), maxSnooze = o.optInt("snMax", 2), rampSec = o.optInt("ramp", 60), vibrate = o.optBoolean("vib", true),
            holidays = o.optBoolean("hol", true),
            offDays = o.optJSONArray("off")?.let { a -> (0 until a.length()).map { a.getLong(it) }.toSet() } ?: emptySet(),
            vacFrom = o.optLong("vacFrom", -1), vacTo = o.optLong("vacTo", -1),
            evening = o.optBoolean("eve", true), nudgeMin = o.optInt("nudge"), wild = o.optBoolean("wild"),
            wishes = o.optJSONArray("wishes")?.let { a -> (0 until a.length()).map { step(a.getJSONObject(it)) } } ?: emptyList(),
            visits = o.optJSONObject("visits")?.let { v -> v.keys().asSequence().mapNotNull { k -> World.entries.firstOrNull { it.name == k }?.let { it to v.getInt(k) } }.toMap() } ?: emptyMap(),
            finds = o.optJSONObject("finds")?.let { v -> v.keys().asSequence().associateWith { v.getInt(it) } } ?: emptyMap(),
            log = log,
            ring = ring,
            lastDone = o.optLong("done", -1),
            introDone = o.optBoolean("intro"),
        )
    }
}

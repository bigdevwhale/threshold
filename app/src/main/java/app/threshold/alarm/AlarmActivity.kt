package app.threshold.alarm

import android.app.KeyguardManager
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.threshold.R
import app.threshold.Stepped
import app.threshold.container
import app.threshold.data.Kind
import app.threshold.data.State
import app.threshold.domain.Frames
import app.threshold.domain.Portal
import app.threshold.domain.Week
import app.threshold.ru
import app.threshold.ui.Btn
import app.threshold.ui.Fmt
import app.threshold.ui.art.ArchBox
import app.threshold.ui.art.FindIcon
import app.threshold.ui.art.portal
import app.threshold.ui.art.rememberTime
import app.threshold.ui.theme.C
import app.threshold.ui.theme.T
import app.threshold.ui.theme.ThresholdTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

/** The portal calling: shown over the lock screen; trace a circle to hold it open, then step through. */
class AlarmActivity : AppCompatActivity() {
    companion object {
        @Volatile var visible = false
    }

    override fun onResume() {
        super.onResume()
        visible = true
        RingService.quiet(this)
    }

    override fun onPause() {
        visible = false
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // The volume keys here turn the alarm up or down, not the music.
        volumeControlStream = android.media.AudioManager.STREAM_ALARM
        val t = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(t), navigationBarStyle = SystemBarStyle.dark(t))
        setContent {
            val s: State? by container.store.state.collectAsStateWithLifecycle(initialValue = null)
            ThresholdTheme { s?.let { AlarmScreen(it, this) } }
        }
    }

    /** Opens the morning's app once the phone is unlocked; the lock screen asks for the PIN if there is one. */
    fun openAfterUnlock(onCancel: () -> Unit = { finish() }, open: () -> Unit) {
        val km = getSystemService(KeyguardManager::class.java)
        if (km.isKeyguardLocked) km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() { open(); finish() }
            override fun onDismissCancelled() { onCancel() }
            override fun onDismissError() { open(); finish() }
        }) else { open(); finish() }
    }
}

internal enum class Phase { OPENING, HOLDING, READY, STEPPING, RESULT }

/** [phase0], [acc0] and [stepped0] let the screen renders show the middle of the morning. */
@Composable
internal fun AlarmScreen(s: State, activity: AlarmActivity?, phase0: Phase = Phase.OPENING, acc0: Float = 0f, stepped0: Stepped? = null) {
    val ctx = LocalContext.current
    val c = ctx.container
    val ru = ctx.ru
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var phase by remember { mutableStateOf(phase0) }
    var acc by remember { mutableFloatStateOf(acc0) }
    var stepped by remember { mutableStateOf(stepped0) }
    var center by remember { mutableStateOf(Offset.Zero) }
    var last by remember { mutableFloatStateOf(0f) }
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val zoom = remember { Animatable(1f) }
    val time by rememberTime()
    LaunchedEffect(Unit) { while (true) { clock = System.currentTimeMillis(); delay(1000) } }

    val ring = s.ring
    // Snoozed from the notification, or nothing is ringing: nothing to show.
    LaunchedEffect(ring, phase) {
        if (phase < Phase.STEPPING && (ring == null || ring.snoozeUntil > System.currentTimeMillis())) { delay(400); activity?.finish() }
    }
    BackHandler(enabled = phase < Phase.RESULT) {}

    val day = ring?.day ?: Week.epochDay(clock)
    val morning = Portal.morning(s, day)
    val progress = (acc / (2 * PI.toFloat())).coerceIn(0f, 1f)
    val target = when (phase) { Phase.OPENING, Phase.HOLDING -> 0.28f + 0.72f * progress; else -> 1f }
    val open by animateFloatAsState(target, tween(if (phase == Phase.OPENING && acc == 0f && activity != null) 2500 else 300), label = "open")
    val canSnooze = ring != null && ring.snoozes < s.maxSnooze && !ring.test

    fun step() {
        if (phase != Phase.READY) return
        phase = Phase.STEPPING
        scope.launch {
            val st = c.step()
            stepped = st
            RingService.step(ctx, keepRadio = st?.steps?.firstOrNull()?.kind == Kind.RADIO)
            zoom.animateTo(3.4f, tween(900, easing = FastOutLinearInEasing))
            phase = Phase.RESULT
        }
    }

    // The morning that opens by itself: the portal opens wide, the phone asks to be unlocked (a glance with face
    // unlock, nothing at all without a lock), and the app comes up. The sound stops only once it has, so an
    // ignored lock screen keeps ringing; a dismissed one falls back to the circle.
    var autoFailed by remember { mutableStateOf(false) }
    val autoOn = morning.auto && !autoFailed && ring?.test != true
    LaunchedEffect(morning.auto, autoFailed) {
        if (!autoOn || activity == null || ring == null) return@LaunchedEffect
        delay(2500)
        if (phase > Phase.HOLDING) return@LaunchedEffect
        phase = Phase.READY
        val first = morning.steps.firstOrNull()
        fun done(open: Boolean) {
            phase = Phase.STEPPING
            val app = ctx.applicationContext
            c.write {
                val st = this.step()
                RingService.step(app, keepRadio = first?.kind == Kind.RADIO)
                val found = st?.finds.orEmpty()
                if (found.isNotEmpty()) kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(app, app.getString(R.string.al_auto_found, found.joinToString(", ") { it.name.of(ru) }), android.widget.Toast.LENGTH_LONG).show()
                }
            }
            if (open && first != null && !Launch.open(ctx, first)) android.widget.Toast.makeText(ctx, R.string.toast_cant_open, android.widget.Toast.LENGTH_LONG).show()
        }
        if (first == null || first.kind == Kind.RADIO) { done(false); activity.finish() }
        else activity.openAfterUnlock(onCancel = { phase = Phase.HOLDING; acc = 0f; autoFailed = true }) { done(true) }
    }

    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF16122A), C.deep), radius = 1600f))) {
        if (phase != Phase.RESULT) Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Column(Modifier.fillMaxWidth().padding(top = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Fmt.today(ctx, Week.epochDay(clock)).uppercase(), style = T.small.copy(letterSpacing = 1.sp))
                Text(Fmt.hm(clock), style = T.clock)
                Text(morning.world.title.of(ru), style = T.h3.copy(color = C.gold))
            }
            Canvas(
                Modifier.weight(1f).fillMaxWidth()
                    .graphicsLayer { scaleX = zoom.value; scaleY = zoom.value; alpha = (1f - (zoom.value - 1f) / 2.4f).coerceIn(0f, 1f) }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { p -> last = atan2(p.y - center.y, p.x - center.x) },
                        ) { change, _ ->
                            if (phase != Phase.OPENING && phase != Phase.HOLDING) return@detectDragGestures
                            val a = atan2(change.position.y - center.y, change.position.x - center.x)
                            var d = a - last
                            if (d > PI) d -= 2 * PI.toFloat()
                            if (d < -PI) d += 2 * PI.toFloat()
                            last = a
                            acc += abs(d)
                            phase = Phase.HOLDING
                            RingService.lift = (acc / (2 * PI.toFloat())).coerceAtMost(1f)
                            if (acc >= 2 * PI) {
                                phase = Phase.READY
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            }
                        }
                    },
            ) {
                val aw = minOf(size.width * 0.58f, size.height * 0.5f)
                val ah = minOf(size.height * 0.78f, aw * 1.65f)
                val b = ArchBox(size.width / 2, (size.height - ah) / 2 + 6.dp.toPx(), aw, ah)
                center = b.center
                portal(b, time, morning.world, open, Frames.of(s.frame), ring = if (phase <= Phase.READY) progress else null, glow = 1.5f)
            }
            Text(
                when (phase) {
                    Phase.OPENING -> if (autoOn) stringResource(R.string.al_hint_auto) else stringResource(R.string.al_hint_open)
                    Phase.HOLDING -> if (progress < 0.5f) stringResource(R.string.al_hint_more) else stringResource(R.string.al_hint_half)
                    Phase.READY -> if (autoOn) stringResource(R.string.al_hint_auto_unlock) else stringResource(R.string.al_hint_ready)
                    else -> ""
                },
                style = T.body.copy(color = Color(0xFFC9C0E0), textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(46.dp),
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 22.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (phase == Phase.READY && !autoOn) Btn(stringResource(R.string.al_step), Modifier.weight(1f), gold = true) { step() }
                else if (phase < Phase.READY) {
                    if (canSnooze) Btn(stringResource(R.string.a_snooze, s.snoozeMin), Modifier.weight(1f)) { RingService.snooze(ctx); activity?.finish() }
                    else Text(stringResource(if (ring?.test == true) R.string.al_test_ring else R.string.al_no_snooze), style = T.sub, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(vertical = 14.dp))
                }
            }
        } else stepped.let { st -> Result(st, activity) }
    }
}

@Composable
private fun Result(st: Stepped?, activity: AlarmActivity?) {
    val ctx = LocalContext.current
    val c = ctx.container
    val ru = ctx.ru
    val first = st?.steps?.firstOrNull()
    fun go(mood: Int?) {
        if (st != null && mood != null && !st.test) c.write { mood(st.at, mood) }
        if (first != null && first.kind != Kind.RADIO) activity?.openAfterUnlock { if (!Launch.open(ctx, first)) android.widget.Toast.makeText(ctx, R.string.toast_cant_open, android.widget.Toast.LENGTH_LONG).show() }
        else activity?.finish()
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        if (st == null) {
            Text(stringResource(R.string.al_gone), style = T.h2)
            Spacer(Modifier.height(20.dp))
            Btn(stringResource(R.string.close), Modifier.fillMaxWidth()) { activity?.finish() }
            return@Column
        }
        Text(stringResource(R.string.al_through), style = T.sub)
        Text(st.world.title.of(ru), style = T.h1.copy(color = Color(st.world.accent), textAlign = TextAlign.Center))
        Spacer(Modifier.height(10.dp))
        Text(
            when {
                first == null -> stringResource(R.string.al_plain)
                first.kind == Kind.RADIO -> stringResource(R.string.al_radio, first.label)
                else -> stringResource(R.string.al_opens, first.label)
            },
            style = T.body, textAlign = TextAlign.Center,
        )
        if (st.steps.size > 1 && !st.wild) Text(stringResource(R.string.al_chain, st.steps[0].minutes, st.steps[1].label), style = T.sub, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(22.dp))
        when {
            st.test -> Text(stringResource(R.string.al_test), style = T.sub, textAlign = TextAlign.Center)
            st.finds.isEmpty() -> Text(stringResource(R.string.al_no_finds), style = T.sub, textAlign = TextAlign.Center)
            else -> {
                Text(if (st.finds.size > 1) stringResource(R.string.al_finds_two) else stringResource(R.string.al_finds_one), style = T.sub)
                Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    st.finds.forEach { f ->
                        Column(Modifier.width(110.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            FindIcon(f.shape, if (f.rare) C.gold else Color(f.world.accent), Modifier.size(46.dp))
                            Text(f.name.of(ru), style = T.sub.copy(color = C.ink), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                            if (f.rare) Text(stringResource(R.string.finds_rare), style = T.small.copy(color = C.gold))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(30.dp))
        if (!st.test) {
            Text(stringResource(R.string.al_how), style = T.sub)
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(stringResource(R.string.mood_fresh), Modifier.weight(1f)) { go(2) }
                Btn(stringResource(R.string.mood_so), Modifier.weight(1f)) { go(1) }
                Btn(stringResource(R.string.mood_hard), Modifier.weight(1f)) { go(0) }
            }
            Spacer(Modifier.height(10.dp))
        }
        Btn(
            when {
                first == null || first.kind == Kind.RADIO -> stringResource(R.string.al_done)
                else -> stringResource(R.string.al_open)
            },
            Modifier.fillMaxWidth(), gold = true,
        ) { go(null) }
    }
}


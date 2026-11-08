package app.threshold.alarm

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

private val alarmAttrs: AudioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_ALARM)
    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    .build()

/**
 * The sound of the portal, made on the fly: a low hum that rises as the portal is held open, and bells
 * that start quietly and grow, so the alarm wakes but never jumps at you.
 */
class Synth(private val rampSec: Int) {
    @Volatile private var running = false
    private var thread: Thread? = null

    /** 0..1: how far the portal has been held open; lifts the hum. */
    @Volatile var lift = 0f

    fun start() {
        if (running) return
        running = true
        thread = Thread({ loop() }, "portal-hum").apply { start() }
    }

    fun stop() {
        running = false
        thread?.join(500)
        thread = null
    }

    private fun loop() {
        val rate = 22050
        val min = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(alarmAttrs)
                .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(maxOf(min, 4096) * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrNull() ?: return
        track.play()
        val buf = ShortArray(1024)
        var n = 0L
        val bells = doubleArrayOf(523.25, 659.25, 783.99, 659.25, 587.33, 698.46)
        var p1 = 0.0; var p2 = 0.0; var p3 = 0.0
        val ramp = rampSec.coerceAtLeast(5).toDouble()
        try {
            while (running) {
                for (i in buf.indices) {
                    val t = n.toDouble() / rate
                    val up = 1.0 + lift * 0.5
                    p1 += 2 * PI * 55.0 * up / rate
                    p2 += 2 * PI * 82.6 * up / rate
                    p3 += 2 * PI * 220.0 * (1 + lift * 0.75) / rate
                    val lfo = 0.8 + 0.2 * sin(2 * PI * 0.7 * t)
                    val tri = 2 / PI * kotlin.math.asin(sin(p3))
                    var v = (sin(p1) * 0.35 + sin(p2) * 0.25 + tri * 0.08) * lfo
                    // A bell every 1.6 seconds, joining in after the first few and growing with the ramp.
                    val period = 1.6
                    val k = (t / period).toInt()
                    val dt = t - k * period
                    val f = bells[k % bells.size]
                    val bellGain = min(1.0, (t - 6).coerceAtLeast(0.0) / (ramp * 1.2))
                    v += bellGain * exp(-2.6 * dt) * (sin(2 * PI * f * dt) * 0.55 + sin(2 * PI * f * 2.76 * dt) * 0.18 + sin(2 * PI * f * 5.4 * dt) * 0.06)
                    val gain = 0.18 + 0.82 * min(1.0, t / ramp)
                    val s = (v * gain * 0.8).coerceIn(-1.0, 1.0)
                    buf[i] = (s * Short.MAX_VALUE).toInt().toShort()
                    n++
                }
                track.write(buf, 0, buf.size)
                if (abs(p1) > 1e6) { p1 %= 2 * PI; p2 %= 2 * PI; p3 %= 2 * PI }
            }
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }
}

/** An internet radio stream, as the alarm itself or after it. Falls back to [onFail] if it won't start. */
class Radio(private val onFail: () -> Unit) {
    private var player: MediaPlayer? = null
    private val main = Handler(Looper.getMainLooper())
    private var started = false

    val playing get() = started

    fun play(url: String, rampSec: Int, alarm: Boolean) {
        stop()
        val mp = MediaPlayer()
        player = mp
        runCatching {
            mp.setAudioAttributes(if (alarm) alarmAttrs else AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            mp.setDataSource(url)
            mp.setVolume(0.05f, 0.05f)
            mp.setOnPreparedListener {
                started = true
                it.start()
                val t0 = System.currentTimeMillis()
                val ramp = rampSec.coerceAtLeast(5) * 1000L
                main.post(object : Runnable {
                    override fun run() {
                        val p = player ?: return
                        val v = (0.05f + 0.95f * ((System.currentTimeMillis() - t0).toFloat() / ramp)).coerceAtMost(1f)
                        runCatching { p.setVolume(v, v) }
                        if (v < 1f) main.postDelayed(this, 250)
                    }
                })
            }
            mp.setOnErrorListener { _, _, _ -> fail(); true }
            mp.prepareAsync()
        }.onFailure { fail() }
        // A stream that is silent after twelve seconds won't wake anyone.
        main.postDelayed({ if (!started && player === mp) fail() }, 12_000)
    }

    private fun fail() {
        stop()
        onFail()
    }

    fun stop() {
        main.removeCallbacksAndMessages(null)
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
        started = false
    }
}

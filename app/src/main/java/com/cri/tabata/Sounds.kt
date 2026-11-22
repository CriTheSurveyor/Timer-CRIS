package com.cri.tabata

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Sounds synthesized at startup, so the APK ships no audio files. */
object Synth {
    const val RATE = 44100

    fun pcm(seconds: Double, f: (Double) -> Double) =
        ShortArray((seconds * RATE).toInt()) { (f(it.toDouble() / RATE).coerceIn(-1.0, 1.0) * 30000).toInt().toShort() }

    /** One strike of a boxing-ring bell: inharmonic partials, higher ones die faster. */
    fun strike(t: Double, at: Double, f0: Double): Double {
        if (t < at) return 0.0
        val x = t - at
        return listOf(1.0 to 1.0, 2.76 to .6, 5.4 to .35, 8.93 to .2)
            .sumOf { (m, a) -> a * sin(2 * PI * f0 * m * x) * exp(-x * (2.0 + m)) } / 2.15
    }

    /** Referee pea whistle: a trilled high tone with a little breath noise, one value per blast (start, length). */
    fun whistle(t: Double, fc: Double, blasts: List<Pair<Double, Double>>, noise: Random): Double {
        val (s, d) = blasts.firstOrNull { (s, d) -> t >= s && t <= s + d } ?: return 0.0
        val env = min(1.0, (t - s) / 0.02) * min(1.0, (s + d - t) / 0.05)
        val trill = sin(2 * PI * 26 * t)
        val tone = sin(2 * PI * fc * t + 6 * trill)
        return env * ((0.75 + 0.25 * trill) * tone * 0.8 + (noise.nextDouble() * 2 - 1) * 0.06)
    }

    fun countdown() = pcm(0.14) { t -> sin(2 * PI * 1000 * t) * min(1.0, (0.14 - t) / 0.03) * 0.7 }
    fun workBell() = pcm(1.8) { t -> strike(t, 0.0, 720.0) + strike(t, 0.32, 720.0) }
    fun restChime() = pcm(1.2) { t -> 0.6 * (strike(t, 0.0, 660.0) + strike(t, 0.22, 495.0)) }
    fun startWhistle() = Random(1).let { r -> pcm(0.8) { t -> whistle(t, 3100.0, listOf(0.0 to 0.75), r) } }
    fun endWhistle() = Random(2).let { r ->
        pcm(1.7) { t -> whistle(t, 2700.0, listOf(0.0 to 0.18, 0.28 to 0.18, 0.56 to 1.1), r) }
    }
}

class Sounds {
    private fun track(pcm: ShortArray): AudioTrack? = runCatching {
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
            )
            .setAudioFormat(
                AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(Synth.RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build().apply { write(pcm, 0, pcm.size) }
    }.getOrNull()

    val countdown = track(Synth.countdown())
    val work = track(Synth.workBell())
    val rest = track(Synth.restChime())
    val start = track(Synth.startWhistle())
    val end = track(Synth.endWhistle())

    fun play(t: AudioTrack?) = runCatching {
        t ?: return@runCatching
        t.stop()
        t.reloadStaticData()
        t.play()
    }

    fun release() = listOf(countdown, work, rest, start, end).forEach { it?.release() }
}

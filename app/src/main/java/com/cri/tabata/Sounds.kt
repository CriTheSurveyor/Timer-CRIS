package com.cri.tabata

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

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

    /** A soft marimba note: a pure tone plus its quiet fourth-octave overtone, fading quickly. */
    fun mallet(t: Double, at: Double, f: Double): Double {
        if (t < at) return 0.0
        val x = t - at
        return min(1.0, x / 0.004) * (sin(2 * PI * f * x) * exp(-x * 3.5) + 0.15 * sin(2 * PI * f * 3.9 * x) * exp(-x * 12))
    }

    fun countdown() = pcm(0.14) { t -> sin(2 * PI * 1000 * t) * min(1.0, (0.14 - t) / 0.03) * 0.7 }
    fun workBell() = pcm(1.8) { t -> strike(t, 0.0, 720.0) + strike(t, 0.32, 720.0) }
    fun restChime() = pcm(1.2) { t -> 0.6 * (strike(t, 0.0, 660.0) + strike(t, 0.22, 495.0)) }
    // C6-E6-G6 rising for the start, then the full chord with the top C held for the end
    fun startChime() = pcm(1.4) { t -> 0.55 * (mallet(t, 0.0, 1046.5) + mallet(t, 0.14, 1318.5) + mallet(t, 0.28, 1568.0)) }
    fun endChime() = pcm(2.4) { t ->
        0.45 * (mallet(t, 0.0, 1046.5) + mallet(t, 0.16, 1318.5) + mallet(t, 0.32, 1568.0)) +
            0.18 * (mallet(t, 0.56, 1046.5) + mallet(t, 0.56, 1318.5) + mallet(t, 0.56, 2093.0))
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
    val start = track(Synth.startChime())
    val end = track(Synth.endChime())

    fun play(t: AudioTrack?) = runCatching {
        t ?: return@runCatching
        t.stop()
        t.reloadStaticData()
        t.play()
    }

    fun release() = listOf(countdown, work, rest, start, end).forEach { it?.release() }
}

package com.cri.tabata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SynthTest {
    private fun peak(a: ShortArray, from: Double, to: Double) =
        a.copyOfRange((from * Synth.RATE).toInt(), (to * Synth.RATE).toInt()).maxOf { abs(it.toInt()) }

    @Test
    fun soundsAreAudibleAndDistinct() {
        listOf(Synth.countdown(), Synth.workBell(), Synth.restChime(), Synth.startChime(), Synth.endChime())
            .forEach { assertTrue(peak(it, 0.0, 0.1) > 5000) }
        assertEquals((2.4 * Synth.RATE).toInt(), Synth.endChime().size)
    }

    @Test
    fun endChimeNeverClips() {
        assertTrue(peak(Synth.endChime(), 0.0, 2.4) < 30000)
    }

    @Test
    fun bellDecays() {
        val b = Synth.workBell()
        assertTrue(peak(b, 1.6, 1.8) < peak(b, 0.32, 0.5) / 4)
    }
}

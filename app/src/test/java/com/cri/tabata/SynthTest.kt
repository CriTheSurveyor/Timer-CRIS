package com.cri.tabata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class SynthTest {
    private fun peak(a: ShortArray, from: Double, to: Double) =
        a.copyOfRange((from * Synth.RATE).toInt(), (to * Synth.RATE).toInt()).maxOf { abs(it.toInt()) }

    @Test
    fun soundsAreAudibleAndDistinct() {
        listOf(Synth.countdown(), Synth.workBell(), Synth.restChime(), Synth.startWhistle(), Synth.endWhistle())
            .forEach { assertTrue(peak(it, 0.0, 0.1) > 5000) }
        assertEquals((1.7 * Synth.RATE).toInt(), Synth.endWhistle().size)
    }

    @Test
    fun endWhistleHasGapsBetweenBlasts() {
        assertEquals(0.0, Synth.whistle(0.22, 2700.0, listOf(0.0 to 0.18, 0.28 to 0.18), Random(0)), 0.0)
    }

    @Test
    fun bellDecays() {
        val b = Synth.workBell()
        assertTrue(peak(b, 1.6, 1.8) < peak(b, 0.32, 0.5) / 4)
    }
}

package com.cri.tabata

import org.junit.Assert.assertEquals
import org.junit.Test

class PhasesTest {
    @Test
    fun classicTabata() {
        val p = Workout(prepare = 10, work = 20, rest = 10, cycles = 8, sets = 1, cooldown = 0).phases()
        assertEquals(1 + 8 + 7, p.size) // no rest after the last cycle
        assertEquals(10 + 8 * 20 + 7 * 10, p.sumOf { it.seconds })
        assertEquals(Kind.WORK, p.last().kind)
    }

    @Test
    fun setsAndCooldown() {
        val p = Workout(prepare = 0, work = 30, rest = 15, cycles = 3, sets = 2, setRest = 60, cooldown = 120).phases()
        assertEquals(listOf(
            Kind.WORK, Kind.REST, Kind.WORK, Kind.REST, Kind.WORK, Kind.SET_REST,
            Kind.WORK, Kind.REST, Kind.WORK, Kind.REST, Kind.WORK, Kind.COOLDOWN,
        ), p.map { it.kind })
        assertEquals(2, p[6].set)
    }

    @Test
    fun format() {
        assertEquals("0:05", fmt(5))
        assertEquals("12:00", fmt(720))
    }
}

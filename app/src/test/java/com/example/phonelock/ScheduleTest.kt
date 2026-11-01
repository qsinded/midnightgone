package com.example.phonelock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleTest {
    private fun m(h: Int, min: Int = 0) = h * 60 + min

    @Test fun overnightWindow() {
        assertTrue(Schedule.isLocked(m(23), m(22), m(6)))
        assertTrue(Schedule.isLocked(m(0), m(22), m(6)))
        assertTrue(Schedule.isLocked(m(5, 59), m(22), m(6)))
        assertFalse(Schedule.isLocked(m(6), m(22), m(6)))   // end is exclusive
        assertFalse(Schedule.isLocked(m(12), m(22), m(6)))
        assertTrue(Schedule.isLocked(m(22), m(22), m(6)))   // start is inclusive
    }

    @Test fun sameDayWindow() {
        assertTrue(Schedule.isLocked(m(9), m(8), m(10)))
        assertFalse(Schedule.isLocked(m(10), m(8), m(10)))
        assertFalse(Schedule.isLocked(m(7), m(8), m(10)))
    }

    @Test fun emptyWindowNeverLocks() {
        assertFalse(Schedule.isLocked(m(12), m(8), m(8)))
    }

    @Test fun lockedMinutes() {
        assertEquals(480, Schedule.lockedMinutes(m(22), m(6)))
        assertEquals(120, Schedule.lockedMinutes(m(8), m(10)))
        assertEquals(0, Schedule.lockedMinutes(m(8), m(8)))
    }

    @Test fun fmtDur() {
        assertEquals("8h", Schedule.fmtDur(480))
        assertEquals("1h 30m", Schedule.fmtDur(90))
        assertEquals("Off", Schedule.fmtDur(0))
    }

    @Test fun progress() {
        assertEquals(0f, Schedule.progress(m(12), m(22), m(6)), 0.001f)    // not locked
        assertEquals(0.25f, Schedule.progress(m(0), m(22), m(6)), 0.001f)  // 2h of 8h
    }
}

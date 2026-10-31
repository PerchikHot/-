package com.perchikhot.novagame

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DailyMemoryLayoutTest {
    @Test fun matchesWindowsDailyBoard() {
        assertEquals(listOf(2, 0, 1, 0, 1, 2, 3, 3),
            DailyMemoryLayout.positions("2026-10-31", 4))
        assertEquals(listOf(1, 0, 0, 1, 3, 4, 5, 5, 2, 2, 3, 4),
            DailyMemoryLayout.positions("2026-10-31", 6))
    }

    @Test fun stableAndTwoOfEachPair() {
        val first = DailyMemoryLayout.positions("2026-11-01", 6)
        assertEquals(first, DailyMemoryLayout.positions("2026-11-01", 6))
        for (index in 0 until 6) assertEquals(2, first.count { it == index })
        assertNotEquals(first, DailyMemoryLayout.positions("2026-11-02", 6))
    }
}

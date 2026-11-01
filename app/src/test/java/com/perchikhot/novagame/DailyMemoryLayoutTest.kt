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

    @Test fun portableChallengeCodeMatchesWindows() {
        assertEquals("NOVA-20261101-P6",
            DailyMemoryLayout.shareCode("2026-11-01", 6))
        try {
            DailyMemoryLayout.shareCode("2026-02-30", 6)
            throw AssertionError("Invalid day accepted")
        } catch (_: java.time.DateTimeException) { }
        try {
            DailyMemoryLayout.shareCode("2026-11-01", 9)
            throw AssertionError("Invalid difficulty accepted")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun importCodeAndRejectMalformed() {
        assertEquals("2026-11-01" to 6,
            DailyMemoryLayout.parseShareCode("NOVA-20261101-P6"))
        assertEquals(DailyMemoryLayout.positions("2026-11-01", 6),
            DailyMemoryLayout.positions(DailyMemoryLayout.parseShareCode("NOVA-20261101-P6").first, 6))
        for (invalid in listOf("NOVA-20260229-P6", "NOVA-20261101-P9",
                "NOVA-20261101-P06", "nova-20261101-P6", " NOVA-20261101-P6")) {
            try {
                DailyMemoryLayout.parseShareCode(invalid)
                throw AssertionError("Accepted malformed challenge: $invalid")
            } catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun stableAndTwoOfEachPair() {
        val first = DailyMemoryLayout.positions("2026-11-01", 6)
        assertEquals(first, DailyMemoryLayout.positions("2026-11-01", 6))
        for (index in 0 until 6) assertEquals(2, first.count { it == index })
        assertNotEquals(first, DailyMemoryLayout.positions("2026-11-02", 6))
    }
}

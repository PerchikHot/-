package com.perchikhot.novagame

import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryRatingTest {
    @Test fun ratesPerfectAndEfficientWins() {
        assertEquals(3, MemoryRating.stars(6, 6, true))
        assertEquals(2, MemoryRating.stars(6, 7, true))
        assertEquals(2, MemoryRating.stars(6, 12, true))
        assertEquals(1, MemoryRating.stars(6, 13, true))
    }

    @Test fun failedAndInvalidRoundsNeverReceiveStars() {
        assertEquals(0, MemoryRating.stars(6, 6, false))
        assertEquals(0, MemoryRating.stars(6, 5, true))
        assertEquals(0, MemoryRating.stars(0, 0, true))
    }
}

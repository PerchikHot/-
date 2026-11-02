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

    @Test fun hintAssistedWinsReceiveOnlyOnePracticeStar() {
        assertEquals(3, MemoryRating.stars(4, 4, true, 0))
        assertEquals(1, MemoryRating.stars(4, 4, true, 1))
        assertEquals(1, MemoryRating.stars(4, 7, true, 2))
        assertEquals(0, MemoryRating.stars(4, 4, false, 1))
    }

    @Test fun failedAndInvalidRoundsNeverReceiveStars() {
        assertEquals(0, MemoryRating.stars(6, 6, false))
        assertEquals(0, MemoryRating.stars(6, 5, true))
        assertEquals(0, MemoryRating.stars(0, 0, true))
    }
    @Test fun accuracyCountsOnlyCompletedPairs() {
        assertEquals(0, MemoryRating.accuracy(8, 12, false))
        assertEquals(0, MemoryRating.accuracy(8, 7, true))
        assertEquals(100, MemoryRating.accuracy(8, 8, true))
        assertEquals(67, MemoryRating.accuracy(8, 12, true))
        assertEquals(50, MemoryRating.accuracy(8, 16, true))
    }
}

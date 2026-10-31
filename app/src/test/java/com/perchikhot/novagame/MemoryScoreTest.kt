package com.perchikhot.novagame

import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryScoreTest {
    @Test fun comboRewardsConsecutiveMatches() {
        assertEquals(0, MemoryScore.matchPoints(0))
        assertEquals(100, MemoryScore.matchPoints(1))
        assertEquals(125, MemoryScore.matchPoints(2))
        assertEquals(175, MemoryScore.matchPoints(4))
    }

    @Test fun practiceAndTimedModesCannotOverwriteNormalRecord() {
        assertEquals(350, MemoryScore.bestNormalScore(350, 500, false))
        assertEquals(500, MemoryScore.bestNormalScore(350, 500, true))
        assertEquals(350, MemoryScore.bestNormalScore(350, 300, true))
    }
}

package com.perchikhot.novagame

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class PuzzleBoardTest {
    @Test fun cornerAndEdgeNeighborsAreLegal() {
        assertEquals(listOf(3, 1), PuzzleBoard.neighbors(0))
        assertEquals(listOf(5, 7), PuzzleBoard.neighbors(8))
        assertEquals(listOf(1, 7, 3, 5), PuzzleBoard.neighbors(4))
        assertTrue(PuzzleBoard.canMove(1, 0))
        assertFalse(PuzzleBoard.canMove(8, 0))
        assertFalse(PuzzleBoard.canMove(-1, 0))
    }

    @Test fun everySeedProducesNontrivialSolvablePermutation() {
        val solved = (0..8).toList()
        for (seed in 0 until 200) {
            val board = PuzzleBoard.scramble(90, Random(seed))
            assertEquals("seed $seed", solved, board.sorted())
            assertNotEquals("seed $seed", solved, board)
            assertEquals(9, board.distinct().size)
        }
    }

    @Test fun invalidInputsDoNotCreateBoards() {
        for (blank in listOf(-1, 9)) {
            try {
                PuzzleBoard.neighbors(blank)
                fail("Expected invalid position")
            } catch (_: IllegalArgumentException) {}
        }
        try {
            PuzzleBoard.scramble(0)
            fail("Expected invalid step count")
        } catch (_: IllegalArgumentException) {}
    }
    @Test fun boardFitsPortraitAndLandscapeWithoutClipping() {
        assertEquals(332, PuzzleBoard.boardSizeDp(360, 800))
        assertEquals(187, PuzzleBoard.boardSizeDp(640, 360))
        assertEquals(420, PuzzleBoard.boardSizeDp(1200, 1000))
        assertEquals(172, PuzzleBoard.boardSizeDp(200, 400))
        try {
            PuzzleBoard.boardSizeDp(0, 600)
            fail("Invalid screen width accepted")
        } catch (_: IllegalArgumentException) {}
    }

}

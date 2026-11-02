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
            assertTrue("seed $seed was too easy",
                PuzzleBoard.manhattanDistance(board) >= 8)
        }
    }

    @Test fun puzzleDifficultyUsesActualTileDistances() {
        assertEquals(0, PuzzleBoard.manhattanDistance((0..8).toList()))
        assertEquals(12, PuzzleBoard.manhattanDistance(
            listOf(4, 7, 1, 0, 6, 2, 8, 3, 5)))
        try {
            PuzzleBoard.manhattanDistance(listOf(0, 0, 1, 2, 3, 4, 5, 6, 8))
            fail("Malformed board accepted")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun progressLabelReportsOnlyLowerBoundNotExactSolution() {
        assertEquals("Пазл собран", PuzzleBoard.remainingMovesLabel((0..8).toList()))
        assertEquals("До решения: не менее 1 ходов",
            PuzzleBoard.remainingMovesLabel(listOf(0,1,2,3,4,5,6,8,7)))
        assertEquals("До решения: не менее 12 ходов",
            PuzzleBoard.remainingMovesLabel(listOf(4,7,1,0,6,2,8,3,5)))
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

    @Test fun hintEscapesGreedyLocalMinimumAndRejectsUnsolvableState() {
        // Both neighbors lower Manhattan distance, but only tile 3 is
        // on an optimal route. The former greedy hint picked tile 1.
        val board = listOf(0, 4, 1, 7, 8, 2, 3, 6, 5)
        assertEquals(3, PuzzleBoard.suggestMove(board))
        assertEquals(listOf(0, 4, 1, 7, 8, 2, 3, 6, 5), board)
        assertNull(PuzzleBoard.suggestMove(listOf(1, 0, 2, 3, 4, 5, 6, 7, 8)))
    }

    @Test fun hintIsAlwaysLegalAndNeverMutatesThePuzzle() {
        assertNull(PuzzleBoard.suggestMove((0..8).toList()))
        for (seed in 0 until 200) {
            val board = PuzzleBoard.scramble(90, Random(seed))
            val before = board.toList()
            val hint = PuzzleBoard.suggestMove(board)
            assertNotNull("seed $seed", hint)
            assertTrue("seed $seed", PuzzleBoard.canMove(hint!!, board.indexOf(8)))
            assertEquals(before, board)
        }
        assertEquals(8, PuzzleBoard.suggestMove(listOf(0,1,2,3,4,5,6,8,7)))
    }


}

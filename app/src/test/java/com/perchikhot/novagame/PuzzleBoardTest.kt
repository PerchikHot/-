package com.perchikhot.novagame

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class PuzzleBoardTest {
    @org.junit.Test fun undoRevertsOnlyOneLegalMoveWithoutChangingSource() {
        val before = listOf(0,1,2,3,4,5,6,8,7)
        val after = listOf(0,1,2,3,4,5,6,7,8)
        assertEquals(before, PuzzleBoard.undoLastMove(after, before))
        assertEquals(after, listOf(0,1,2,3,4,5,6,7,8))
        assertNull(PuzzleBoard.undoLastMove(before, before))
        assertNull(PuzzleBoard.undoLastMove(listOf(1,0,2,3,4,5,6,8,7), before))
        assertNull(PuzzleBoard.undoLastMove(after, listOf(0,1,2,3,4,5,8,6,7)))
    }

    @Test fun cornerAndEdgeNeighborsAreLegal() {
        assertEquals(listOf(3, 1), PuzzleBoard.neighbors(0))
        assertEquals(listOf(5, 7), PuzzleBoard.neighbors(8))
        assertEquals(listOf(1, 7, 3, 5), PuzzleBoard.neighbors(4))
        assertTrue(PuzzleBoard.canMove(1, 0))
        assertFalse(PuzzleBoard.canMove(8, 0))
        assertFalse(PuzzleBoard.canMove(-1, 0))
    }

    @Test fun completedPuzzleCannotBeMovedOrResavedByLateTaps() {
        // A win grants points and deletes the unfinished checkpoint.
        // Touch events queued immediately afterward must not recreate it.
        assertTrue(PuzzleBoard.canAcceptMove(false, 7, 8))
        assertFalse(PuzzleBoard.canAcceptMove(true, 7, 8))
        assertFalse(PuzzleBoard.canAcceptMove(false, 0, 8))
        assertFalse(PuzzleBoard.canAcceptMove(true, 0, 8))
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

    @Test fun difficultyModesCreateSolvableNontrivialBoards() {
        assertEquals(listOf(24, 90, 180), (0..2).map(PuzzleBoard::difficultySteps))
        assertEquals(listOf("Лёгкая", "Обычная", "Сложная"),
            (0..2).map(PuzzleBoard::difficultyName))
        for (level in 0..2) {
            val threshold = listOf(6, 8, 12)[level]
            for (seed in 0 until 40) {
                val cells = PuzzleBoard.scramble(
                    PuzzleBoard.difficultySteps(level), Random(seed))
                assertEquals((0..8).toList(), cells.sorted())
                assertTrue("level $level seed $seed",
                    PuzzleBoard.manhattanDistance(cells) >= threshold)
            }
        }
        try {
            PuzzleBoard.difficultySteps(3)
            fail("Invalid difficulty accepted")
        } catch (_: IllegalArgumentException) {}
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
        assertEquals("До решения: не менее 1 ход",
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

    @Test fun hintTextMatchesActualSlideDirection() {
        val board = listOf(0, 1, 2, 3, 8, 4, 6, 7, 5)
        assertTrue(PuzzleBoard.hintInstruction(board, 3).contains("вправо"))
        assertTrue(PuzzleBoard.hintInstruction(board, 5).contains("влево"))
        assertTrue(PuzzleBoard.hintInstruction(board, 1).contains("вниз"))
        assertTrue(PuzzleBoard.hintInstruction(board, 7).contains("вверх"))
        assertEquals(board, listOf(0, 1, 2, 3, 8, 4, 6, 7, 5))
        try {
            PuzzleBoard.hintInstruction(board, 0)
            fail("Nonadjacent hint accepted")
        } catch (_: IllegalArgumentException) { }
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

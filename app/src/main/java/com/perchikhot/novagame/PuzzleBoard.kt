package com.perchikhot.novagame

import kotlin.random.Random

/** Solvable 3x3 sliding-puzzle layouts, independent of Android views. */
internal object PuzzleBoard {
    private val solved = (0..8).toList()

    fun neighbors(blank: Int): List<Int> {
        require(blank in 0..8) { "Invalid blank position" }
        val row = blank / 3
        val col = blank % 3
        return buildList {
            if (row > 0) add(blank - 3)
            if (row < 2) add(blank + 3)
            if (col > 0) add(blank - 1)
            if (col < 2) add(blank + 1)
        }
    }

    /** Fit the playable square on compact Android screens in both
     * orientations; never allow a fixed 360dp board to overflow a phone.
     * The view's GridLayout gets the same width and height.
     */
    fun boardSizeDp(widthDp: Int, heightDp: Int): Int {
        require(widthDp > 0 && heightDp > 0)
        val usableWidth = (widthDp - 28).coerceAtLeast(1)
        val usableHeight = (heightDp * 0.52f).toInt().coerceAtLeast(1)
        return minOf(usableWidth, usableHeight, 420)
    }

    fun canMove(tile: Int, blank: Int): Boolean =
        tile in 0..8 && blank in 0..8 && tile in neighbors(blank)

    /** Legal random walk guarantees solvability; avoid immediate undo.
     * Never offer an already solved board, even for an unlucky seed.
     */
    fun scramble(steps: Int = 90, random: Random = Random.Default): MutableList<Int> {
        require(steps in 1..10000) { "Invalid shuffle length" }
        val cells = solved.toMutableList()
        var previousBlank = -1
        repeat(steps) {
            val blank = cells.indexOf(8)
            val choices = neighbors(blank).filter { it != previousBlank }
            val next = (if (choices.isEmpty()) neighbors(blank) else choices).random(random)
            cells[blank] = cells[next]
            cells[next] = 8
            previousBlank = blank
        }
        if (cells == solved) {
            val blank = cells.indexOf(8)
            val next = neighbors(blank).first()
            cells[blank] = cells[next]
            cells[next] = 8
        }
        return cells
    }
}

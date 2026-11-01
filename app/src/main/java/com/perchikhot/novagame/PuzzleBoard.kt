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

    /** Manhattan distance from the solved board, ignoring the blank tile.
     * Used to avoid offering trivial one-move puzzles after a long shuffle.
     */
    fun manhattanDistance(cells: List<Int>): Int {
        require(cells.size == 9 && cells.sorted() == solved) { "Invalid board" }
        return cells.indices.sumOf { index ->
            val tile = cells[index]
            if (tile == 8) 0 else
                kotlin.math.abs(index / 3 - tile / 3) +
                    kotlin.math.abs(index % 3 - tile % 3)
        }
    }

    /** Return a legal first move on a solved path, not just the lowest
     * Manhattan neighbor. A bounded IDA* search fixes local minima where
     * a greedy hint repeatedly sends the player backwards. Never mutates
     * the caller's puzzle and falls back to a legal hint under CPU pressure.
     */
    fun suggestMove(cells: List<Int>): Int? {
        require(cells.size == 9 && cells.sorted() == solved) { "Invalid board" }
        if (cells == solved) return null
        // A 3x3 puzzle with odd inversion parity cannot be completed.
        var inversions = 0
        for (i in cells.indices) for (j in i + 1 until cells.size) {
            if (cells[i] != 8 && cells[j] != 8 && cells[i] > cells[j]) inversions++
        }
        if (inversions % 2 != 0) return null

        val board = cells.toMutableList()
        val initialBlank = board.indexOf(8)
        var threshold = manhattanDistance(board)
        var visited = 0
        var nextThreshold = Int.MAX_VALUE
        var firstSolutionMove: Int? = null
        fun heuristic(): Int = board.indices.sumOf { index ->
            val tile = board[index]
            if (tile == 8) 0 else
                kotlin.math.abs(index / 3 - tile / 3) +
                    kotlin.math.abs(index % 3 - tile % 3)
        }
        fun search(depth: Int, blank: Int, previousBlank: Int, first: Int): Boolean {
            val h = heuristic()
            val score = depth + h
            if (score > threshold) {
                nextThreshold = minOf(nextThreshold, score)
                return false
            }
            if (h == 0) {
                firstSolutionMove = first
                return true
            }
            if (++visited > 12000) return false
            for (tile in neighbors(blank)) {
                if (tile == previousBlank) continue
                board[blank] = board[tile]
                board[tile] = 8
                val initial = if (depth == 0) tile else first
                if (search(depth + 1, tile, blank, initial)) return true
                board[tile] = board[blank]
                board[blank] = 8
            }
            return false
        }
        while (threshold <= 31 && visited < 12000) {
            nextThreshold = Int.MAX_VALUE
            if (search(0, initialBlank, -1, -1)) return firstSolutionMove
            if (nextThreshold == Int.MAX_VALUE) break
            threshold = nextThreshold
        }
        // Under a strict computation budget, preserve the previous fast hint.
        return neighbors(initialBlank).minByOrNull { tile ->
            val candidate = cells.toMutableList()
            candidate[initialBlank] = candidate[tile]
            candidate[tile] = 8
            manhattanDistance(candidate)
        }
    }

    /** Legal random walk guarantees solvability; avoid immediate undo.
     * Never offer an already solved board, even for an unlucky seed.
     */
    fun scramble(steps: Int = 90, random: Random = Random.Default): MutableList<Int> {
        require(steps in 1..10000) { "Invalid shuffle length" }
        // A random walk can return near its origin; retry boundedly so a
        // standard 90-step game is not accidentally solved in two moves.
        repeat(if (steps >= 20) 24 else 1) {
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
            if (cells != solved && (steps < 20 || manhattanDistance(cells) >= 8)) {
                return cells
            }
        }
        // A fixed reachable 12-move state is the deterministic fallback if
        // an adversarial random source repeatedly produces easy boards.
        return mutableListOf(4, 7, 1, 0, 6, 2, 8, 3, 5)
    }
}

package com.perchikhot.novagame

/** Pure scoring shared by normal, daily and practice memory rounds.
 *  The rating is display-only; currency and personal bests stay separate. */
internal object MemoryRating {
    /** Percentage of successful attempts on a completed memory board. */
    fun accuracy(pairs: Int, moves: Int, won: Boolean): Int {
        if (!won || pairs <= 0 || moves < pairs) return 0
        return ((pairs.toLong() * 100 + moves / 2) / moves).coerceIn(0, 100).toInt()
    }

    fun stars(pairs: Int, moves: Int, won: Boolean, hintsUsed: Int = 0): Int {
        if (!won || pairs <= 0 || moves < pairs) return 0
        // Hint-assisted completions remain practice results, never 3-star wins.
        if (hintsUsed > 0) return 1
        return when {
            moves == pairs -> 3
            moves <= pairs * 2 -> 2
            else -> 1
        }
    }
}

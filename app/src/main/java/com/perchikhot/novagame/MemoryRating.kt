package com.perchikhot.novagame

/** Pure scoring shared by normal, daily and practice memory rounds.
 *  The rating is display-only; currency and personal bests stay separate. */
internal object MemoryRating {
    fun stars(pairs: Int, moves: Int, won: Boolean): Int {
        if (!won || pairs <= 0 || moves < pairs) return 0
        return when {
            moves == pairs -> 3
            moves <= pairs * 2 -> 2
            else -> 1
        }
    }
}

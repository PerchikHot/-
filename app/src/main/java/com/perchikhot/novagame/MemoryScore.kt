package com.perchikhot.novagame

/** Round points and normal-mode record rules. Practice and challenge
 *  modes never change persistent records or shop currency. */
internal object MemoryScore {
    fun matchPoints(streak: Int): Int =
        if (streak <= 0) 0 else 100 + 25 * (streak - 1)

    fun bestNormalScore(previous: Int, round: Int, eligible: Boolean): Int =
        if (eligible && round > previous) round else previous
}

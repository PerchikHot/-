package com.perchikhot.novagame

import java.time.LocalDate

/** Deterministic daily card positions shared with Nova Windows.
 * Images remain local to each device; only pair positions are identical.
 * No global RNG or saved progress is modified. */
internal object DailyMemoryLayout {
    fun positions(day: String, pairs: Int): List<Int> {
        require(pairs in 2..8) { "Pair count must be between 2 and 8" }
        val parsed = LocalDate.parse(day)
        var state = (parsed.year * 10000 + parsed.monthValue * 100 +
            parsed.dayOfMonth) xor (pairs * 0x9E3779B9.toInt())
        val result = (0 until pairs).toList().let { (it + it).toMutableList() }
        for (i in result.lastIndex downTo 1) {
            state = state xor (state shl 13)
            state = state xor (state ushr 17)
            state = state xor (state shl 5)
            val j = ((state.toLong() and 0xffffffffL) % (i + 1)).toInt()
            val temp = result[i]
            result[i] = result[j]
            result[j] = temp
        }
        return result
    }
}

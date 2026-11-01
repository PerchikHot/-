package com.perchikhot.novagame

import java.time.LocalDate

/** Deterministic daily card positions shared with Nova Windows.
 * Images remain local to each device; only pair positions are identical.
 * No global RNG or saved progress is modified. */
internal object DailyMemoryLayout {
    /** Copyable board code, independent of the user's private image files. */
    fun shareCode(day: String, pairs: Int): String {
        require(pairs in 2..8) { "Pair count must be between 2 and 8" }
        val parsed = LocalDate.parse(day)
        require(parsed.toString() == day) { "Use ISO date" }
        return "NOVA-${day.replace("-", "")}-P$pairs"
    }

    /** Imported codes select the same board on Android and Windows. */
    fun parseShareCode(code: String): Pair<String, Int> {
        val match = Regex("^NOVA-(\\d{4})(\\d{2})(\\d{2})-P([2-8])$").matchEntire(code)
            ?: throw IllegalArgumentException("Invalid challenge code")
        val day = "${match.groupValues[1]}-${match.groupValues[2]}-${match.groupValues[3]}"
        val pairs = match.groupValues[4].toInt()
        try {
            require(shareCode(day, pairs) == code) { "Invalid challenge date" }
        } catch (error: java.time.DateTimeException) {
            throw IllegalArgumentException("Invalid challenge date", error)
        }
        return day to pairs
    }

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

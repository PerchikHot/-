package com.perchikhot.novagame

import kotlin.random.Random

/** Pure memory-game hint rules, independent of Android widgets or images. */
internal object MemoryHintSelector {
    fun candidates(values: List<Int>, opened: Set<Int>): List<Pair<Int, Int>> {
        val pairs = mutableListOf<Pair<Int, Int>>()
        for (index in values.indices) {
            if (index in opened) continue
            val partner = ((index + 1) until values.size).firstOrNull {
                it !in opened && values[it] == values[index]
            }
            if (partner != null) pairs.add(index to partner)
        }
        return pairs
    }

    /** Do not always reveal the first pair; preserve matched cards and quota. */
    fun choose(values: List<Int>, opened: Set<Int>, random: Random = Random.Default,
               previouslyShown: Set<Pair<Int, Int>> = emptySet()): Pair<Int, Int>? {
        // A hint consumes one of only two chances. Do not reveal the same
        // unmatched pair twice when the player has not solved it yet.
        val fresh = candidates(values, opened).filter { it !in previouslyShown }
        return if (fresh.isEmpty()) null else fresh.random(random)
    }
}

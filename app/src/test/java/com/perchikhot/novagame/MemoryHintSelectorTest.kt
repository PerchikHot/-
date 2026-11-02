package com.perchikhot.novagame

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class MemoryHintSelectorTest {
    @Test fun hintChoosesOnlyCompleteUnmatchedPairs() {
        val cards = listOf(0, 1, 2, 0, 1, 2)
        val opened = setOf(0, 3)
        val candidates = MemoryHintSelector.candidates(cards, opened)
        assertEquals(listOf(1 to 4, 2 to 5), candidates)
        repeat(30) {
            val selected = MemoryHintSelector.choose(cards, opened, Random(it))
            assertTrue(selected in candidates)
            assertTrue(selected!!.first !in opened)
        }
    }

    @Test fun seededHintIsReproducibleAndDoesNotChangeCards() {
        val cards = listOf(0, 1, 2, 0, 1, 2)
        val before = cards.toList()
        val a = MemoryHintSelector.choose(cards, emptySet(), Random(42))
        val b = MemoryHintSelector.choose(cards, emptySet(), Random(42))
        assertEquals(a, b)
        assertEquals(before, cards)
    }

    @Test fun completedBoardHasNoHint() {
        val cards = listOf(0, 1, 0, 1)
        assertNull(MemoryHintSelector.choose(cards, setOf(0, 1, 2, 3)))
        assertNull(MemoryHintSelector.choose(emptyList(), emptySet()))
    }
}

package com.perchikhot.novagame

import org.junit.Assert.*
import org.junit.Test

class PuzzleSessionTest {
    private val uri = "content://com.android.providers.media.documents/document/image%3A123"
    private val board = listOf(0,1,2,3,4,5,6,8,7)

    @Test fun restoresUnfinishedBoardWithoutChangingScore() {
        val raw = PuzzleSession.encode(uri,board,42,3)
        assertNotNull(raw)
        assertTrue(raw!!.startsWith("v2|"))
        val restored = PuzzleSession.decode(raw)
        assertEquals(board, restored?.cells)
        assertEquals(42, restored?.moves)
        assertEquals(3, restored?.hints)
        assertEquals(uri, restored?.uri)
        assertNull(PuzzleSession.encode(uri,(0..8).toList(),42,3))
    }

    @Test fun rejectsCorruptOrImpossibleProgress() {
        val valid = PuzzleSession.encode(uri,board,42,3)!!
        // Preserve old users' unfinished puzzles, but checksum every new save.
        assertEquals(board, PuzzleSession.decode("v1|42|3|0,1,2,3,4,5,6,8,7|$uri")?.cells)
        assertNull(PuzzleSession.decode(valid.replace("|42|", "|43|")))
        assertNull(PuzzleSession.decode(valid.replace("image%3A123", "image%3A124")))
        assertNull(PuzzleSession.decode(valid.replace("0,1,2,3,4,5,6,8,7",
            "0,2,1,4,3,5,6,8,7")))

        for (raw in listOf("", "v0|42|3|0,1,2,3,4,5,6,8,7|$uri",
            valid.replace("|42|", "|-1|"),
            valid.replace("|3|", "|10001|"),
            valid.replace("6,8,7", "7,8,6"),
            valid.replace("content://", "https://"),
            valid.replace("6,8,7", "6,7,7"),
            "v1|0|0|0,1,2,3,4,5,6,7,8|$uri",
            "x".repeat(2401))) {
            assertNull(PuzzleSession.decode(raw))
        }
        assertNull(PuzzleSession.decode(null))
    }
}

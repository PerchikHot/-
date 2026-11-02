package com.perchikhot.novagame

import org.junit.Assert.*
import org.junit.Test

class PuzzleSessionTest {
    private val uri = "content://com.android.providers.media.documents/document/image%3A123"
    private val board = listOf(0,1,2,3,4,5,6,8,7)

    @Test fun restoresUnfinishedBoardWithoutChangingScore() {
        val raw = PuzzleSession.encode(uri,board,42,3)
        assertNotNull(raw)
        assertTrue(raw!!.startsWith("v3|"))
        val restored = PuzzleSession.decode(raw)
        assertEquals(board, restored?.cells)
        assertEquals(42, restored?.moves)
        assertEquals(3, restored?.hints)
        assertEquals(uri, restored?.uri)
        assertNull(PuzzleSession.encode(uri,(0..8).toList(),42,3))
    }

    @Test fun savedUndoSurvivesProcessDeathAndCannotBeReplayed() {
        val before = board
        val after = listOf(0,1,2,3,4,5,8,6,7)
        val raw = PuzzleSession.encode(uri, after, 43, 3, before)
        assertNotNull(raw)
        val resumed = PuzzleSession.decode(raw)
        assertEquals(after, resumed?.cells)
        assertEquals(before, resumed?.previous)
        assertTrue(PuzzleSession.canResume(resumed, true))
        assertEquals(before, PuzzleBoard.undoLastMove(resumed!!.cells, resumed.previous!!))
        // Undo consumes the saved previous board; moves are NOT refunded.
        val consumed = PuzzleSession.decode(PuzzleSession.encode(uri, before, 43, 3))
        assertEquals(43, consumed?.moves)
        assertNull(consumed?.previous)
        // A forged or unrelated undo state is rejected before disk write.
        assertNull(PuzzleSession.encode(uri, after, 43, 3, (0..8).toList()))
        assertFalse(PuzzleSession.canResume(
            PuzzleSession.Round(uri, after, 43, 3, (0..8).toList()), true))
    }

    @Test fun oldV2CheckpointsRemainReadableAfterV3Upgrade() {
        val payload = "42|3|0,1,2,3,4,5,6,8,7|$uri"
        val crc = java.util.zip.CRC32()
        crc.update(payload.toByteArray(Charsets.UTF_8))
        val old = "v2|" + crc.value.toString(16).padStart(8, '0') + "|" + payload
        val recovered = PuzzleSession.decode(old)
        assertEquals(board, recovered?.cells)
        assertNull(recovered?.previous)
        assertEquals(42, recovered?.moves)
    }

    @Test fun refusesToResumeWhenSavedPhotoIsNoLongerReadable() {
        val saved = PuzzleSession.decode(PuzzleSession.encode(uri, board, 42, 3))
        assertTrue(PuzzleSession.canResume(saved, true))
        assertFalse(PuzzleSession.canResume(saved, false))
        assertFalse(PuzzleSession.canResume(null, true))
        // A caller may construct a Round without going through decode().
        // Never reopen a solved, impossible or corrupt board as an active game.
        assertFalse(PuzzleSession.canResume(
            PuzzleSession.Round(uri, (0..8).toList(), 4, 0), true))
        assertFalse(PuzzleSession.canResume(
            PuzzleSession.Round(uri, listOf(1,0,2,3,4,5,6,7,8), 4, 0), true))
        assertFalse(PuzzleSession.canResume(
            PuzzleSession.Round("https://remote.invalid/picture", board, 4, 0), true))
        assertFalse(PuzzleSession.canResume(
            PuzzleSession.Round(uri, board, -1, 0), true))
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

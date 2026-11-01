package com.perchikhot.novagame

/** Durable 3x3 puzzle checkpoint. Stores only a local photo URI and board
 * indices; never embeds image pixels, files, or external network data.
 * Corrupted or unsolvable checkpoints are ignored without changing rewards.
 */
internal object PuzzleSession {
    data class Round(val uri: String, val cells: List<Int>, val moves: Int, val hints: Int)

    fun encode(uri: String, cells: List<Int>, moves: Int, hints: Int): String? {
        if (!valid(uri, cells, moves, hints) || cells == (0..8).toList()) return null
        return "v1|$moves|$hints|${cells.joinToString(",")}|$uri"
    }

    fun decode(raw: String?): Round? {
        if (raw == null || raw.length > 2400) return null
        val fields = raw.split('|', limit = 5)
        if (fields.size != 5 || fields[0] != "v1") return null
        val moves = fields[1].toIntOrNull() ?: return null
        val hints = fields[2].toIntOrNull() ?: return null
        val cells = fields[3].split(',').map { it.toIntOrNull() ?: return null }
        if (!valid(fields[4], cells, moves, hints) || cells == (0..8).toList()) return null
        return Round(fields[4], cells, moves, hints)
    }

    private fun valid(uri: String, cells: List<Int>, moves: Int, hints: Int): Boolean {
        if (uri.length !in 10..2048 ||
            !(uri.startsWith("content://") || uri.startsWith("file://")) ||
            uri.any { it == '\\n' || it == '\\r' || it == '\\u0000' } ||
            cells.size != 9 || cells.sorted() != (0..8).toList() ||
            moves !in 0..100000 || hints !in 0..10000) return false
        var inversions = 0
        for (i in cells.indices) for (j in i + 1 until cells.size) {
            if (cells[i] != 8 && cells[j] != 8 && cells[i] > cells[j]) inversions++
        }
        return inversions % 2 == 0
    }
}

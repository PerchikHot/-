package com.perchikhot.novagame

import java.util.zip.CRC32

/** Durable 3x3 puzzle checkpoint. Stores only a local photo URI and board
 * indices; never embeds image pixels, files, or external network data.
 * Corrupted or unsolvable checkpoints are ignored without changing rewards.
 */
internal object PuzzleSession {
    data class Round(val uri: String, val cells: List<Int>, val moves: Int, val hints: Int)

    fun encode(uri: String, cells: List<Int>, moves: Int, hints: Int): String? {
        if (!valid(uri, cells, moves, hints) || cells == (0..8).toList()) return null
        val payload = "$moves|$hints|${cells.joinToString(",")}|$uri"
        return "v2|${checksum(payload)}|$payload"
    }

    fun decode(raw: String?): Round? {
        if (raw == null || raw.length > 2400) return null
        val version = raw.substringBefore('|')
        val payload = when (version) {
            "v1" -> raw.removePrefix("v1|") // Legacy checkpoints remain readable.
            "v2" -> {
                val fields = raw.split('|', limit = 3)
                if (fields.size != 3 || fields[1].length != 8 ||
                    fields[1].any { it !in "0123456789abcdef" } ||
                    checksum(fields[2]) != fields[1]) return null
                fields[2]
            }
            else -> return null
        }
        val fields = payload.split('|', limit = 4)
        if (fields.size != 4) return null
        val moves = fields[0].toIntOrNull() ?: return null
        val hints = fields[1].toIntOrNull() ?: return null
        val cells = fields[2].split(',').map { it.toIntOrNull() ?: return null }
        if (!valid(fields[3], cells, moves, hints) || cells == (0..8).toList()) return null
        return Round(fields[3], cells, moves, hints)
    }

    private fun checksum(payload: String): String {
        val crc = CRC32()
        crc.update(payload.toByteArray(Charsets.UTF_8))
        return crc.value.toString(16).padStart(8, '0')
    }

    private fun valid(uri: String, cells: List<Int>, moves: Int, hints: Int): Boolean {
        if (uri.length !in 10..2048 ||
            !(uri.startsWith("content://") || uri.startsWith("file://")) ||
            uri.any { it == '\n' || it == '\r' || it == '\u0000' } ||
            cells.size != 9 || cells.sorted() != (0..8).toList() ||
            moves !in 0..100000 || hints !in 0..10000) return false
        var inversions = 0
        for (i in cells.indices) for (j in i + 1 until cells.size) {
            if (cells[i] != 8 && cells[j] != 8 && cells[i] > cells[j]) inversions++
        }
        return inversions % 2 == 0
    }
}

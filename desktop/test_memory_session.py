"""Nova Windows checkpoint regression tests; no Tk window required."""
import json
import tempfile
import unittest
from pathlib import Path

from memory_session import (load_session, save_session, clear_session,
                            restore_session, snapshot_session)
from nova_desktop import MemoryGame, SYMBOLS


class MemorySessionTests(unittest.TestCase):
    def test_resume_preserves_board_and_score_without_face_up_cards(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "current_memory_session.json"
            game = MemoryGame(seed=123, pair_count=4)
            game.choose(0)
            game.choose(1)
            game.score = 125
            save_session(path, game, SYMBOLS)
            restored = load_session(path, MemoryGame, SYMBOLS)
            self.assertIsNotNone(restored)
            self.assertEqual(restored.cards, game.cards)
            self.assertEqual(restored.moves, game.moves)
            self.assertEqual(restored.score, 125)
            self.assertEqual(restored.flipped, [])
            self.assertEqual(restored.hint_indices, set())
            self.assertNotIn("gallery", path.read_text(encoding="utf-8"))

    def test_valid_backup_recovers_corrupted_primary(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "round.json"
            game = MemoryGame(seed=4, pair_count=2)
            game.moves = 1
            save_session(path, game, SYMBOLS)
            game.moves = 2
            save_session(path, game, SYMBOLS)
            path.write_text("{broken", encoding="utf-8")
            restored = load_session(path, MemoryGame, SYMBOLS)
            self.assertEqual(restored.moves, 1)
            clear_session(path)
            self.assertIsNone(load_session(path, MemoryGame, SYMBOLS))

    def test_rejects_tampered_board_and_half_matched_pair(self):
        game = MemoryGame(seed=12, pair_count=2)
        game.moves = 1
        clean = snapshot_session(game, SYMBOLS)
        forged = dict(clean)
        forged["cards"] = ["X"] * 4
        with self.assertRaises(ValueError):
            restore_session(forged, MemoryGame, SYMBOLS)
        forged = dict(clean)
        forged["matched"] = [0, 1]
        if clean["cards"][0] == clean["cards"][1]:
            forged["matched"] = [0, 2]
        with self.assertRaises(ValueError):
            restore_session(forged, MemoryGame, SYMBOLS)

    def test_special_modes_and_completed_rounds_never_save(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "round.json"
            with self.assertRaises(ValueError):
                save_session(path, MemoryGame(seed=1, pair_count=2,
                                             perfect_mode=True), SYMBOLS)
            game = MemoryGame(seed=1, pair_count=2)
            game.matched = set(range(4))
            with self.assertRaises(ValueError):
                save_session(path, game, SYMBOLS)
            self.assertFalse(path.exists())


if __name__ == "__main__":
    unittest.main()

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

    def test_backup_recovery_repairs_primary_for_subsequent_launches(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "round.json"
            game = MemoryGame(seed=4, pair_count=2)
            game.moves = 1
            save_session(path, game, SYMBOLS)
            game.moves = 2
            save_session(path, game, SYMBOLS)
            backup_before = path.with_suffix(".bak").read_bytes()
            path.write_text("{broken", encoding="utf-8")
            recovered = load_session(path, MemoryGame, SYMBOLS)
            self.assertEqual(recovered.moves, 1)
            self.assertEqual(path.with_suffix(".bak").read_bytes(), backup_before)
            # The repaired primary now works without the backup.
            path.with_suffix(".bak").unlink()
            self.assertEqual(load_session(path, MemoryGame, SYMBOLS).moves, 1)
            self.assertEqual(list(Path(folder).glob(".nova-session-*.tmp")), [])

    def test_failed_backup_replacement_keeps_primary_save_and_old_backup(self):
        from unittest.mock import patch
        import os
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "round.json"
            game = MemoryGame(seed=4, pair_count=2)
            game.moves = 1
            save_session(path, game, SYMBOLS)
            game.moves = 2
            save_session(path, game, SYMBOLS)
            old_backup = path.with_suffix(".bak").read_bytes()
            real_replace = os.replace
            def fail_backup(source, destination):
                if Path(destination).suffix == ".bak":
                    raise OSError("simulated interrupted backup write")
                return real_replace(source, destination)
            game.moves = 3
            with patch("memory_session.os.replace", side_effect=fail_backup):
                save_session(path, game, SYMBOLS)
            self.assertEqual(load_session(path, MemoryGame, SYMBOLS).moves, 3)
            self.assertEqual(path.with_suffix(".bak").read_bytes(), old_backup)
            self.assertEqual(list(Path(folder).glob(".nova-session-*.tmp")), [])

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

    def test_rejects_impossible_streak_even_with_valid_legacy_format(self):
        game = MemoryGame(seed=13, pair_count=4)
        game.moves = 2
        checkpoint = snapshot_session(game, SYMBOLS)
        # Legacy v1 has no checksum; semantic validation still matters.
        checkpoint["version"] = 1
        checkpoint.pop("checksum")
        checkpoint["match_streak"] = 1
        checkpoint["best_streak"] = 1
        with self.assertRaises(ValueError):
            restore_session(checkpoint, MemoryGame, SYMBOLS)
        checkpoint["match_streak"] = 0
        with self.assertRaises(ValueError):
            restore_session(checkpoint, MemoryGame, SYMBOLS)

    def test_v2_checksum_rejects_plausible_but_modified_checkpoint(self):
        game = MemoryGame(seed=12, pair_count=4)
        clean = snapshot_session(game, SYMBOLS)
        self.assertEqual(clean["version"], 2)
        self.assertEqual(len(clean["checksum"]), 8)
        changed = dict(clean)
        changed["moves"] = 10  # Still valid counters, but not the saved turn.
        with self.assertRaises(ValueError):
            restore_session(changed, MemoryGame, SYMBOLS)
        changed = dict(clean)
        changed["cards"] = list(reversed(clean["cards"]))
        with self.assertRaises(ValueError):
            restore_session(changed, MemoryGame, SYMBOLS)

    def test_legacy_v1_checkpoint_is_restored_then_upgraded(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "round.json"
            game = MemoryGame(seed=7, pair_count=4)
            legacy = snapshot_session(game, SYMBOLS)
            legacy["version"] = 1
            legacy.pop("checksum")
            path.write_text(json.dumps(legacy, ensure_ascii=False), encoding="utf-8")
            restored = load_session(path, MemoryGame, SYMBOLS)
            self.assertIsNotNone(restored)
            self.assertEqual(restored.cards, game.cards)
            save_session(path, restored, SYMBOLS)
            updated = json.loads(path.read_text(encoding="utf-8"))
            self.assertEqual(updated["version"], 2)
            self.assertEqual(len(updated["checksum"]), 8)

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

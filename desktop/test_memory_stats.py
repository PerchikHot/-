"""Persistent memory-game records never store gallery images or paths."""
import json
import tempfile
import unittest
from pathlib import Path

from memory_stats import (load_memory_stats, record_memory_win, save_memory_stats,
                          load_best_score, updated_best_score, save_best_score)


class MemoryRecordTests(unittest.TestCase):
    def test_difficulty_records_are_independent_and_validate_completed_boards(self):
        with tempfile.TemporaryDirectory() as folder:
            small = Path(folder) / "memory_stats_2_pairs.json"
            large = Path(folder) / "memory_stats.json"
            stats = record_memory_win(load_memory_stats(small, 2), 2, 2)
            save_memory_stats(small, stats)
            self.assertEqual(load_memory_stats(small, 2), {"wins": 1, "best_moves": 2})
            self.assertEqual(load_memory_stats(large), {"wins": 0, "best_moves": None})
            with self.assertRaises(ValueError):
                record_memory_win(stats, 1, 2)


    def test_best_moves_and_wins_survive_restart(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder) / "Veyrion" / "memory_stats.json"
            stats = load_memory_stats(target)
            self.assertEqual(stats, {"wins": 0, "best_moves": None})
            for moves in (18, 25, 12):
                stats = record_memory_win(stats, moves)
                save_memory_stats(target, stats)
            self.assertEqual(load_memory_stats(target), {"wins": 3, "best_moves": 12})
            self.assertEqual(set(json.loads(target.read_text())), {"wins", "best_moves"})

    def test_corrupt_or_untrusted_records_are_ignored(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder) / "memory_stats.json"
            for payload in ('{', '{"wins":true,"best_moves":8}',
                            '{"wins":-1,"best_moves":8}',
                            '{"wins":1,"best_moves":2}',
                            '{"wins":0,"best_moves":8}',
                            ' ' * 5000):
                target.write_text(payload, encoding="utf-8")
                self.assertEqual(load_memory_stats(target), {"wins": 0, "best_moves": None})

    def test_normal_score_is_persistent_and_challenges_do_not_modify_it(self):
        with tempfile.TemporaryDirectory() as folder:
            normal = Path(folder) / "normal_best_score.json"
            score = load_best_score(normal)
            self.assertEqual(score, 0)
            score = updated_best_score(score, 550, eligible=True)
            score = updated_best_score(score, 900, eligible=False)
            score = updated_best_score(score, 450, eligible=True)
            save_best_score(normal, score)
            self.assertEqual(load_best_score(normal), 550)
            self.assertEqual(set(json.loads(normal.read_text())), {"best_score"})

    def test_bad_score_records_are_ignored(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder) / "normal_best_score.json"
            for payload in ('{', '{"best_score":true}', '{"best_score":-2}',
                            '{"best_score":99999999}', ' ' * 500):
                target.write_text(payload, encoding="utf-8")
                self.assertEqual(load_best_score(target), 0)
            with self.assertRaises(ValueError):
                updated_best_score(0, -1, eligible=True)

    def test_invalid_moves_do_not_change_records(self):
        stats = {"wins": 1, "best_moves": 11}
        for moves in (0, 7, -1, 100001, True):
            with self.assertRaises(ValueError):
                record_memory_win(stats, moves)
        self.assertEqual(stats, {"wins": 1, "best_moves": 11})


if __name__ == "__main__":
    unittest.main()

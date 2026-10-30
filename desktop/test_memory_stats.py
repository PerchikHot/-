"""Persistent memory-game records never store gallery images or paths."""
import json
import tempfile
import unittest
from pathlib import Path

from memory_stats import load_memory_stats, record_memory_win, save_memory_stats


class MemoryRecordTests(unittest.TestCase):
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

    def test_invalid_moves_do_not_change_records(self):
        stats = {"wins": 1, "best_moves": 11}
        for moves in (0, 7, -1, 100001, True):
            with self.assertRaises(ValueError):
                record_memory_win(stats, moves)
        self.assertEqual(stats, {"wins": 1, "best_moves": 11})


if __name__ == "__main__":
    unittest.main()

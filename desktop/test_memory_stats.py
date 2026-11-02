"""Persistent memory-game records never store gallery images or paths."""
import json
import tempfile
import unittest
from pathlib import Path

from memory_stats import (load_memory_stats, record_memory_win, save_memory_stats,
                          load_best_score, updated_best_score, save_best_score,
                          load_daily_challenge, record_daily_challenge_win)


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

    def test_recovers_memory_record_from_last_valid_backup(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder) / "memory_stats.json"
            save_memory_stats(target, {"wins": 1, "best_moves": 14})
            save_memory_stats(target, {"wins": 2, "best_moves": 12})
            target.write_text("{damaged", encoding="utf-8")
            self.assertEqual(load_memory_stats(target),
                             {"wins": 1, "best_moves": 14})
            # A damaged primary must not overwrite the last good backup.
            save_memory_stats(target, {"wins": 3, "best_moves": 10})
            self.assertEqual(load_memory_stats(target.with_suffix(".json.bak")),
                             {"wins": 1, "best_moves": 14})
            self.assertEqual(load_memory_stats(target),
                             {"wins": 3, "best_moves": 10})

    def test_recovers_best_score_and_current_daily_challenge(self):
        with tempfile.TemporaryDirectory() as folder:
            score = Path(folder) / "score.json"
            save_best_score(score, 300)
            save_best_score(score, 500)
            score.write_text("invalid", encoding="utf-8")
            self.assertEqual(load_best_score(score), 300)

            daily = Path(folder) / "daily.json"
            day = "2026-11-01"
            first = record_daily_challenge_win(
                load_daily_challenge(daily, day), 18, day)
            save_memory_stats(daily, first)
            second = record_daily_challenge_win(first, 14, day)
            save_memory_stats(daily, second)
            daily.write_text("{", encoding="utf-8")
            self.assertEqual(load_daily_challenge(daily, day), first)
            self.assertEqual(load_daily_challenge(daily, "2026-11-02"),
                             {"date": "2026-11-02", "wins": 0,
                              "best_moves": None})

    def test_older_records_with_extra_metadata_keep_player_progress(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder) / "old.json"
            target.write_text(json.dumps({
                "wins": 4, "best_moves": 11, "legacy_version": 1
            }), encoding="utf-8")
            self.assertEqual(load_memory_stats(target),
                             {"wins": 4, "best_moves": 11})
            score = Path(folder) / "old-score.json"
            score.write_text(json.dumps({
                "best_score": 950, "legacy_version": 1
            }), encoding="utf-8")
            self.assertEqual(load_best_score(score), 950)

    def test_rejects_invalid_stats_before_touching_disk(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder) / "record.json"
            with self.assertRaises(ValueError):
                save_memory_stats(target, {"wins": True, "best_moves": 8})
            self.assertFalse(target.exists())

    def test_invalid_existing_records_cannot_be_promoted_to_wins(self):
        for bad in (
            {"wins": True, "best_moves": 8},
            {"wins": -1, "best_moves": 8},
            {"wins": 0, "best_moves": 8},
            {"wins": 1, "best_moves": 1},
            {"best_score": 500},
            {},
        ):
            with self.subTest(record=bad), self.assertRaises(ValueError):
                record_memory_win(bad, 10)
        self.assertEqual(record_memory_win({"wins": 0, "best_moves": None}, 10),
                         {"wins": 1, "best_moves": 10})

    def test_invalid_moves_do_not_change_records(self):
        stats = {"wins": 1, "best_moves": 11}
        for moves in (0, 7, -1, 100001, True):
            with self.assertRaises(ValueError):
                record_memory_win(stats, moves)
        self.assertEqual(stats, {"wins": 1, "best_moves": 11})


if __name__ == "__main__":
    unittest.main()

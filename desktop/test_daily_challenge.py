"""Daily challenge records stay independent of ordinary memory statistics."""
import tempfile
import unittest
from pathlib import Path
from memory_stats import (
    daily_challenge_seed, load_daily_challenge, record_daily_challenge_win,
    save_memory_stats, load_memory_stats,
)
from nova_desktop import MemoryGame


class DailyChallengeTests(unittest.TestCase):
    def test_fixed_board_for_same_day_and_new_board_for_another(self):
        a = MemoryGame(seed=daily_challenge_seed("2026-10-30"))
        b = MemoryGame(seed=daily_challenge_seed("2026-10-30"))
        self.assertEqual(a.cards, b.cards)
        self.assertNotEqual(daily_challenge_seed("2026-10-30"),
                            daily_challenge_seed("2026-10-31"))

    def test_daily_best_persists_and_resets_on_new_day(self):
        with tempfile.TemporaryDirectory() as folder:
            daily = Path(folder) / "daily.json"
            ordinary = Path(folder) / "normal.json"
            save_memory_stats(ordinary, {"wins": 2, "best_moves": 10})
            stats = load_daily_challenge(daily, "2026-10-30")
            stats = record_daily_challenge_win(stats, 15, "2026-10-30")
            stats = record_daily_challenge_win(stats, 12, "2026-10-30")
            stats = record_daily_challenge_win(stats, 18, "2026-10-30")
            save_memory_stats(daily, stats)
            self.assertEqual(load_daily_challenge(daily, "2026-10-30"),
                             {"date": "2026-10-30", "wins": 3, "best_moves": 12})
            self.assertEqual(load_daily_challenge(daily, "2026-10-31")["wins"], 0)
            self.assertEqual(load_memory_stats(ordinary), {"wins": 2, "best_moves": 10})

    def test_invalid_record_is_not_loaded(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "daily.json"
            path.write_text('{"date":"2026-10-30","wins":true,"best_moves":8}',
                            encoding="utf-8")
            self.assertEqual(load_daily_challenge(path, "2026-10-30")["wins"], 0)
            with self.assertRaises(ValueError):
                daily_challenge_seed("not-a-day")
            with self.assertRaises(ValueError):
                record_daily_challenge_win({"date": "2026-10-30", "wins": 0,
                                            "best_moves": None}, 7, "2026-10-30")


if __name__ == "__main__":
    unittest.main()

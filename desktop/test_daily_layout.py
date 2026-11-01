import unittest
from daily_layout import (daily_positions, daily_challenge_code,
                          parse_daily_challenge_code, shared_challenge_summary)


class DailyLayoutTests(unittest.TestCase):
    def test_shared_android_windows_vectors(self):
        self.assertEqual(daily_positions("2026-10-31", 4),
                         [2, 0, 1, 0, 1, 2, 3, 3])
        self.assertEqual(daily_positions("2026-10-31", 6),
                         [1, 0, 0, 1, 3, 4, 5, 5, 2, 2, 3, 4])

    def test_validity_and_determinism(self):
        self.assertEqual(daily_positions("2026-11-01", 6),
                         daily_positions("2026-11-01", 6))
        self.assertNotEqual(daily_positions("2026-11-01", 6),
                            daily_positions("2026-11-02", 6))
        for pairs in (2, 4, 6, 8):
            values = daily_positions("2026-11-03", pairs)
            self.assertEqual(len(values), pairs * 2)
            self.assertTrue(all(values.count(i) == 2 for i in range(pairs)))

    def test_portable_daily_challenge_code(self):
        self.assertEqual(daily_challenge_code("2026-11-01", 6),
                         "NOVA-20261101-P6")
        for day, pairs in [("2026-02-30", 6), ("2026-11-01", True),
                           ("2026-11-01", 9), ("01/11/2026", 4)]:
            with self.subTest(day=day, pairs=pairs):
                with self.assertRaises(ValueError):
                    daily_challenge_code(day, pairs)

    def test_import_shared_challenge_code(self):
        self.assertEqual(parse_daily_challenge_code("NOVA-20261101-P6"), ("2026-11-01", 6))
        self.assertEqual(parse_daily_challenge_code("NOVA-20240229-P2"), ("2024-02-29", 2))
        self.assertEqual(parse_daily_challenge_code("  NOVA-20261101-P6\n"), ("2026-11-01", 6))
        self.assertEqual(daily_positions(*parse_daily_challenge_code("NOVA-20261101-P6")),
                         daily_positions("2026-11-01", 6))
        for invalid in ("NOVA-20260229-P6", "NOVA-20261101-P9", "NOVA-20261101-P1",
                        "NOVA-20261101-P06", "nova-20261101-P6", "NOVA-20261101-P6/../",
                        "NOVA-20261101-P6" + " " * 100, "NOVA-20261101-P6\x00", None):
            with self.subTest(invalid=invalid), self.assertRaises(ValueError):
                parse_daily_challenge_code(invalid)

    def test_shared_challenge_preview_is_explicit_and_private(self):
        summary = shared_challenge_summary("2026-11-01", 6)
        self.assertIn("NOVA-20261101-P6", summary)
        self.assertIn("12 (6 пар)", summary)
        self.assertIn("рекорды и награды не изменятся", summary)
        self.assertNotIn("C:\\\\Users", summary)
        with self.assertRaises(ValueError):
            shared_challenge_summary("2026-02-30", 6)

    def test_invalid_inputs(self):
        for pairs in (0, 1, 9, True):
            with self.assertRaises(ValueError):
                daily_positions("2026-10-31", pairs)
        with self.assertRaises(ValueError):
            daily_positions("2026-02-30", 4)


if __name__ == "__main__":
    unittest.main()

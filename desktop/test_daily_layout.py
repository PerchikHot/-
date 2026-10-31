import unittest
from daily_layout import daily_positions


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

    def test_invalid_inputs(self):
        for pairs in (0, 1, 9, True):
            with self.assertRaises(ValueError):
                daily_positions("2026-10-31", pairs)
        with self.assertRaises(ValueError):
            daily_positions("2026-02-30", 4)


if __name__ == "__main__":
    unittest.main()

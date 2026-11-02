"""Nova Windows gameplay regression tests for fair memory hints."""
import unittest

from nova_desktop import MemoryGame


class MemoryHintSelectionTests(unittest.TestCase):
    def test_hint_uses_seeded_random_choice_without_changing_progress(self):
        first = MemoryGame(seed=123, pair_count=8)
        second = MemoryGame(seed=123, pair_count=8)
        pair_a = first.peek_pair()
        pair_b = second.peek_pair()
        self.assertEqual(pair_a, pair_b)
        self.assertIsNotNone(pair_a)
        self.assertEqual(first.cards[pair_a[0]], first.cards[pair_a[1]])
        self.assertEqual(first.hints_used, 1)
        self.assertEqual(first.moves, 0)
        self.assertEqual(first.matched, set())
        first.hide_hint()
        self.assertIsNotNone(first.peek_pair())
        first.hide_hint()
        self.assertIsNone(first.peek_pair())

    def test_second_hint_reveals_a_different_unmatched_pair(self):
        game = MemoryGame(seed=123, pair_count=8)
        first = game.peek_pair()
        self.assertIsNotNone(first)
        game.hide_hint()
        second = game.peek_pair()
        self.assertIsNotNone(second)
        self.assertNotEqual(first, second)
        self.assertEqual(game.hints_used, 2)
        self.assertIsNone(game.peek_pair())

    def test_hint_breaks_combo_but_never_erases_earned_points(self):
        game = MemoryGame(seed=9, pair_count=4)
        pair = [i for i, card in enumerate(game.cards)
                if card == game.cards[0]]
        self.assertEqual(game.choose(pair[0]), "first")
        self.assertEqual(game.choose(pair[1]), "match")
        self.assertEqual(game.match_streak, 1)
        old_score = game.score
        old_best = game.best_streak
        self.assertIsNotNone(game.peek_pair())
        self.assertEqual(game.match_streak, 0)
        self.assertEqual(game.best_streak, old_best)
        self.assertEqual(game.score, old_score)
        # Repeated hint while a hint is showing must not consume quota.
        self.assertIsNone(game.peek_pair())
        self.assertEqual(game.hints_used, 1)

    def test_hint_avoids_already_matched_pairs(self):
        game = MemoryGame(seed=42, pair_count=4)
        symbol = game.cards[0]
        game.matched = {i for i, card in enumerate(game.cards) if card == symbol}
        pair = game.peek_pair()
        self.assertIsNotNone(pair)
        self.assertFalse(set(pair) & game.matched)

    def test_invalid_indexes_cannot_be_treated_as_card_numbers(self):
        game = MemoryGame(seed=4, pair_count=4)
        for index in (True, False, 1.0, -1, 999, None, '1'):
            self.assertEqual(game.choose(index), 'ignored')
        self.assertEqual(game.moves, 0)
        self.assertEqual(game.flipped, [])


if __name__ == '__main__':
    unittest.main()

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

"""Headless tests for the Nova Windows preview state machine."""
import unittest
from collections import Counter

from nova_desktop import MemoryGame


class MemoryGameTests(unittest.TestCase):
    def test_deck_contains_eight_pairs(self):
        game = MemoryGame(seed=1)
        self.assertEqual(len(game.cards), 16)
        self.assertEqual(sorted(Counter(game.cards).values()), [2] * 8)

    def test_match_is_kept_and_cannot_be_selected_again(self):
        game = MemoryGame(seed=4)
        left = 0
        right = game.cards.index(game.cards[left], 1)
        self.assertEqual(game.choose(left), "first")
        self.assertEqual(game.choose(right), "match")
        self.assertEqual(game.matched, {left, right})
        self.assertEqual(game.moves, 1)
        self.assertEqual(game.choose(left), "ignored")

    def test_mismatch_blocks_input_until_hidden(self):
        game = MemoryGame(seed=4)
        right = next(i for i in range(1, 16) if game.cards[i] != game.cards[0])
        self.assertEqual(game.choose(0), "first")
        self.assertEqual(game.choose(right), "miss")
        self.assertEqual(game.choose(15), "ignored")
        game.hide_mismatch()
        self.assertEqual(game.flipped, [])
        self.assertEqual(game.choose(0), "first")

    def test_out_of_bounds_ignored(self):
        game = MemoryGame(seed=1)
        self.assertEqual(game.choose(-1), "ignored")
        self.assertEqual(game.choose(16), "ignored")


if __name__ == "__main__":
    unittest.main()

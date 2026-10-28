"""Headless tests for the Nova Windows preview state machine."""
import unittest
import random
from collections import Counter

from nova_desktop import MemoryGame, random_candidate_indices, memory_key_to_index, is_new_game_key, is_oversized_image


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



class KeyboardControlsTests(unittest.TestCase):
    def test_all_sixteen_cards_are_reachable(self):
        keys = "123456789abcdefg"
        self.assertEqual([memory_key_to_index(key) for key in keys], list(range(16)))

    def test_non_card_keys_do_not_flip_cards(self):
        for key in ("", "Escape", "space", "0", "h", "F5"):
            self.assertIsNone(memory_key_to_index(key))
        self.assertEqual(memory_key_to_index("A"), 9)


class RestartShortcutTests(unittest.TestCase):
    def test_n_restarts_but_other_keys_do_not(self):
        self.assertTrue(is_new_game_key("n"))
        self.assertTrue(is_new_game_key("N"))
        for key in ("", "Escape", "a", "Enter", "nn"):
            self.assertFalse(is_new_game_key(key))


class RandomGalleryTests(unittest.TestCase):
    def test_random_candidates_are_unique_and_bounded(self):
        indices = random_candidate_indices(100, limit=12, rng=random.Random(7))
        self.assertEqual(len(indices), 12)
        self.assertEqual(len(set(indices)), 12)
        self.assertTrue(all(0 <= index < 100 for index in indices))

    def test_empty_or_small_gallery(self):
        self.assertEqual(random_candidate_indices(0), [])
        self.assertEqual(sorted(random_candidate_indices(3, rng=random.Random(1))), [0, 1, 2])


class GalleryImageSizeTests(unittest.TestCase):
    def test_common_phone_photos_remain_supported(self):
        self.assertFalse(is_oversized_image(8000, 6000))
        self.assertFalse(is_oversized_image(10000, 8000))

    def test_extreme_and_invalid_dimensions_are_rejected(self):
        self.assertTrue(is_oversized_image(12000, 9000))
        self.assertTrue(is_oversized_image(0, 100))
        self.assertTrue(is_oversized_image(-1, 100))


if __name__ == "__main__":
    unittest.main()

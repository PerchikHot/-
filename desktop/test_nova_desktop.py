"""Headless tests for the Nova Windows preview state machine."""
import unittest
import random
import tempfile
from collections import Counter
from pathlib import Path

from nova_desktop import MemoryGame, random_candidate_indices, memory_key_to_index, is_new_game_key, is_oversized_image, gallery_matches_query, collect_gallery_images


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


class GalleryFilterTests(unittest.TestCase):
    def test_case_insensitive_filename_filter(self):
        image = Path('Моя_Эрза.PNG')
        self.assertTrue(gallery_matches_query(image, 'эрза'))
        self.assertTrue(gallery_matches_query(image, '  МОЯ  '))
        self.assertTrue(gallery_matches_query(image, ''))
        self.assertFalse(gallery_matches_query(image, 'Нами'))

    def test_multiple_words_match_regardless_of_order(self):
        image = Path('Лето_Эрза_2026.PNG')
        self.assertTrue(gallery_matches_query(image, 'эрза лето'))
        self.assertTrue(gallery_matches_query(image, '2026 ЭРЗА'))
        self.assertFalse(gallery_matches_query(image, 'эрза зима'))


class NestedGalleryTests(unittest.TestCase):
    def test_scans_nested_folders_without_touching_images(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "sub" / "deep").mkdir(parents=True)
            first = root / "top.PNG"
            second = root / "sub" / "deep" / "photo.webp"
            first.write_bytes(b"one")
            second.write_bytes(b"two")
            (root / "sub" / "notes.txt").write_text("ignore")
            self.assertEqual(set(collect_gallery_images(root)), {first, second})
            self.assertEqual(first.read_bytes(), b"one")
            self.assertEqual(second.read_bytes(), b"two")

    def test_caps_scanned_files_and_handles_missing_folder(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            for index in range(5):
                (root / f"photo_{index}.jpg").write_bytes(b"image")
            self.assertEqual(len(collect_gallery_images(root, max_images=3)), 3)
            self.assertEqual(collect_gallery_images(root, max_images=0), [])
            self.assertEqual(collect_gallery_images(root / "missing"), [])


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

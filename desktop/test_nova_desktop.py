"""Headless tests for the Nova Windows preview state machine."""
import unittest
import random
import tempfile
from collections import Counter
from pathlib import Path

from nova_desktop import MemoryGame, random_candidate_indices, unseen_candidate_indices, memory_key_to_index, is_new_game_key, is_oversized_image, gallery_matches_query, collect_gallery_images, natural_gallery_key


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


class NoRepeatGalleryTests(unittest.TestCase):
    def test_exhausts_gallery_before_starting_new_cycle(self):
        seen = set()
        picks = []
        for _ in range(4):
            candidates = unseen_candidate_indices(4, seen, rng=random.Random(8))
            self.assertTrue(candidates)
            chosen = candidates[0]
            self.assertNotIn(chosen, seen)
            seen.add(chosen)
            picks.append(chosen)
        self.assertEqual(set(picks), set(range(4)))
        # The next click starts a new cycle, even for a one-image folder.
        self.assertEqual(len(unseen_candidate_indices(4, seen, rng=random.Random(1))), 4)
        self.assertEqual(seen, set())

    def test_failed_candidates_are_not_retried_within_cycle(self):
        seen = set()
        first = unseen_candidate_indices(15, seen, limit=12, rng=random.Random(3))
        seen.update(first)  # All failed to decode.
        second = unseen_candidate_indices(15, seen, limit=12, rng=random.Random(4))
        self.assertEqual(len(second), 3)
        self.assertTrue(set(second).isdisjoint(first))
        self.assertEqual(unseen_candidate_indices(0, seen), [])
        self.assertEqual(unseen_candidate_indices(15, seen, limit=0), [])


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

    def test_search_matches_nested_folder_and_filename(self):
        root = Path('gallery')
        image = root / 'Эрза' / 'Лето' / '001.PNG'
        self.assertTrue(gallery_matches_query(image, 'эрза 001', root))
        self.assertTrue(gallery_matches_query(image, 'лето эрза', root))
        self.assertFalse(gallery_matches_query(image, 'зима', root))
        # Without a gallery root, do not accidentally search absolute paths.
        self.assertFalse(gallery_matches_query(image, 'эрза'))
        self.assertFalse(gallery_matches_query(image, 'other', Path('elsewhere')))



class FolderGalleryFilterTests(unittest.TestCase):
    def test_folder_filter_matches_only_relative_directory(self):
        root = Path('gallery')
        image = root / 'Эрза' / 'Лето' / '001.PNG'
        self.assertTrue(gallery_matches_query(image, 'folder:эрза ext:png', root))
        self.assertTrue(gallery_matches_query(image, 'folder:лето 001', root))
        self.assertFalse(gallery_matches_query(image, 'folder:001', root))
        self.assertFalse(gallery_matches_query(image, 'folder:', root))
        self.assertFalse(gallery_matches_query(image, 'folder:эрза'))

    def test_folder_filter_does_not_match_filename(self):
        root = Path('gallery')
        image = root / 'Нами' / 'Эрза.png'
        self.assertFalse(gallery_matches_query(image, 'folder:эрза', root))
        self.assertTrue(gallery_matches_query(image, 'folder:нами', root))


class FilenameGalleryFilterTests(unittest.TestCase):
    def test_name_filter_ignores_matching_folder_names(self):
        root = Path("gallery")
        file = root / "Эрза" / "001.png"
        self.assertTrue(gallery_matches_query(file, "name:001", root))
        self.assertTrue(gallery_matches_query(file, "folder:эрза name:001", root))
        self.assertFalse(gallery_matches_query(file, "name:эрза", root))
        self.assertFalse(gallery_matches_query(file, "name:", root))
        self.assertFalse(gallery_matches_query(file, "name:002", root))


class ExtensionGalleryFilterTests(unittest.TestCase):
    def test_filters_by_format_and_character(self):
        root = Path('gallery')
        image = root / 'Эрза' / '001.GIF'
        self.assertTrue(gallery_matches_query(image, 'эрза ext:gif', root))
        self.assertTrue(gallery_matches_query(image, 'ext:.gif 001', root))
        self.assertFalse(gallery_matches_query(image, 'ext:png', root))
        self.assertFalse(gallery_matches_query(image, 'ext:exe', root))
        self.assertFalse(gallery_matches_query(image, 'ext:', root))


class MultiExtensionGalleryFilterTests(unittest.TestCase):
    def test_multiple_extensions_can_be_selected_with_one_query(self):
        root = Path('gallery')
        self.assertTrue(gallery_matches_query(root / 'Эрза' / '001.PNG', 'эрза ext:png,jpg', root))
        self.assertTrue(gallery_matches_query(root / 'Эрза' / '002.JPG', 'ext:.png,.jpg', root))
        self.assertFalse(gallery_matches_query(root / 'Эрза' / '003.GIF', 'ext:png,jpg', root))

    def test_malformed_or_unsupported_formats_are_rejected(self):
        image = Path('001.png')
        for query in ('ext:', 'ext:png,', 'ext:,jpg', 'ext:png,,jpg', 'ext:exe,png'):
            self.assertFalse(gallery_matches_query(image, query), query)


class ExcludedGalleryTermsTests(unittest.TestCase):
    def test_exclusion_can_remove_unwanted_variants_without_hiding_character(self):
        root = Path('gallery')
        final = root / 'Эрза' / 'Лето' / '001.png'
        draft = root / 'Эрза' / 'Черновик' / '002.png'
        self.assertTrue(gallery_matches_query(final, 'эрза -черновик', root))
        self.assertFalse(gallery_matches_query(draft, 'эрза -черновик', root))
        self.assertFalse(gallery_matches_query(final, '-001', root))
        self.assertTrue(gallery_matches_query(final, '-002', root))
        self.assertFalse(gallery_matches_query(final, '-', root))
        self.assertFalse(gallery_matches_query(final, 'эрза -', root))
        self.assertTrue(gallery_matches_query(final, 'эрза', root))

    def test_exclusion_without_root_does_not_search_parent_paths(self):
        image = Path('Черновик') / '001.png'
        self.assertTrue(gallery_matches_query(image, '-черновик'))
        self.assertFalse(gallery_matches_query(image, '-001'))


class NaturalGalleryOrderTests(unittest.TestCase):
    def test_numbered_files_are_sorted_numerically(self):
        self.assertEqual(sorted(['10.png', '2.png', '1.PNG'], key=natural_gallery_key),
                         ['1.PNG', '2.png', '10.png'])

    def test_scanner_orders_nested_numbered_folders(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            for folder in ('10', '2', '1'):
                (root / folder).mkdir()
                (root / folder / '1.png').write_bytes(b'image')
            self.assertEqual([p.parent.name for p in collect_gallery_images(root)],
                             ['1', '2', '10'])


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



class GalleryRescanTests(unittest.TestCase):
    def test_rescan_detects_new_and_removed_files_without_changing_them(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            first = root / '001.png'
            first.write_bytes(b'first')
            self.assertEqual(collect_gallery_images(root), [first])
            second = root / '002.png'
            second.write_bytes(b'second')
            self.assertEqual(collect_gallery_images(root), [first, second])
            first.unlink()
            self.assertEqual(collect_gallery_images(root), [second])
            self.assertEqual(second.read_bytes(), b'second')


if __name__ == "__main__":
    unittest.main()

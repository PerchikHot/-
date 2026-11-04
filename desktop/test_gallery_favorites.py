import json
import tempfile
import unittest
from pathlib import Path
from gallery_favorites import favorite_key, load_favorites, save_favorites

class FavoritesTests(unittest.TestCase):
    def test_roundtrip_and_removal(self):
        with tempfile.TemporaryDirectory() as root:
            path = Path(root) / "settings" / "favorites.json"
            one = favorite_key(Path(root) / "света.png")
            two = favorite_key(Path(root) / "nova.gif")
            save_favorites(path, {one, two})
            self.assertEqual(load_favorites(path), {one, two})
            save_favorites(path, {two})
            self.assertEqual(load_favorites(path), {two})
            self.assertEqual(json.loads(path.read_text(encoding="utf-8")), [two])
    def test_corrupt_or_invalid_file_is_safe(self):
        with tempfile.TemporaryDirectory() as root:
            path = Path(root) / "favorites.json"
            self.assertEqual(load_favorites(path), set())
            path.write_text("{broken", encoding="utf-8")
            self.assertEqual(load_favorites(path), set())
            path.write_text('["ok", 1, null, ""]', encoding="utf-8")
            self.assertEqual(load_favorites(path), {"ok"})

if __name__ == "__main__":
    unittest.main()

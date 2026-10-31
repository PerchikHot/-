"""Nova Android/Windows parity: completed memory-board accuracy."""
import unittest
from nova_desktop import MemoryGame

class MemoryAccuracyTests(unittest.TestCase):
    def test_accuracy_is_display_only_and_requires_completion(self):
        game = MemoryGame(seed=17)
        self.assertEqual(game.completion_accuracy(), 0)
        game.matched = set(range(len(game.cards)))
        game.moves = 8
        self.assertEqual(game.completion_accuracy(), 100)
        game.moves = 12
        self.assertEqual(game.completion_accuracy(), 67)
        game.moves = 16
        self.assertEqual(game.completion_accuracy(), 50)
        game.failed = True
        self.assertEqual(game.completion_accuracy(), 0)

if __name__ == "__main__":
    unittest.main()

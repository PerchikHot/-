"""Local, privacy-preserving Nova memory-game records.

Only aggregate win count and best move count are persisted. Gallery paths,
images and individual game sessions are never written to this file.
"""
from __future__ import annotations

import json
import os
import tempfile
from pathlib import Path


def default_stats_path() -> Path:
    base = os.environ.get("LOCALAPPDATA")
    root = Path(base) if base else Path.home() / ".local" / "share"
    return root / "Veyrion" / "NovaGame" / "memory_stats.json"


def load_memory_stats(path: Path, pairs: int = 8) -> dict:
    fallback = {"wins": 0, "best_moves": None}
    try:
        if path.stat().st_size > 4096:
            return fallback
        data = json.loads(path.read_text(encoding="utf-8"))
        if not isinstance(data, dict):
            return fallback
        wins = data.get("wins")
        best = data.get("best_moves")
        if type(wins) is not int or not 0 <= wins <= 1_000_000:
            return fallback
        if best is not None and (type(best) is not int or not pairs <= best <= 100_000):
            return fallback
        if wins == 0 and best is not None:
            return fallback
        return {"wins": wins, "best_moves": best}
    except (OSError, ValueError, TypeError, UnicodeError):
        return fallback


def record_memory_win(stats: dict, moves: int, pairs: int = 8) -> dict:
    """Return updated stats for the selected completed board size."""
    if type(moves) is not int or not pairs <= moves <= 100_000:
        raise ValueError("Invalid completed-game move count")
    wins = stats["wins"]
    best = stats["best_moves"]
    return {
        "wins": min(1_000_000, wins + 1),
        "best_moves": moves if best is None else min(best, moves),
    }


def save_memory_stats(path: Path, stats: dict) -> None:
    """Atomic local write; never replace the record with partial JSON."""
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="w", encoding="utf-8", dir=path.parent,
            prefix=".memory-stats-", suffix=".tmp", delete=False
        ) as output:
            temporary = Path(output.name)
            json.dump(stats, output, ensure_ascii=False)
            output.flush()
            os.fsync(output.fileno())
        os.replace(temporary, path)
    finally:
        if temporary is not None:
            temporary.unlink(missing_ok=True)


def daily_challenge_seed(day: str, pairs: int = 8) -> int:
    """One deterministic 4x4 board per local calendar day."""
    from datetime import date
    parsed = date.fromisoformat(day)
    base = parsed.year * 10000 + parsed.month * 100 + parsed.day
    return base if pairs == 8 else base * 31 + pairs


def load_daily_challenge(path: Path, day: str, pairs: int = 8) -> dict:
    """Daily results never overwrite ordinary memory-game records."""
    daily_challenge_seed(day, pairs)
    fallback = {"date": day, "wins": 0, "best_moves": None}
    try:
        if path.stat().st_size > 4096:
            return fallback
        data = json.loads(path.read_text(encoding="utf-8"))
        if not isinstance(data, dict) or data.get("date") != day:
            return fallback
        wins, best = data.get("wins"), data.get("best_moves")
        if type(wins) is not int or not 0 <= wins <= 100000:
            return fallback
        if best is not None and (type(best) is not int or not pairs <= best <= 100000):
            return fallback
        if wins == 0 and best is not None:
            return fallback
        return {"date": day, "wins": wins, "best_moves": best}
    except (OSError, ValueError, TypeError, UnicodeError):
        return fallback


def record_daily_challenge_win(stats: dict, moves: int, day: str, pairs: int = 8) -> dict:
    """Store the best unassisted result for the current day's fixed board."""
    if stats.get("date") != day:
        stats = {"date": day, "wins": 0, "best_moves": None}
    result = record_memory_win(stats, moves, pairs)
    return {"date": day, **result}

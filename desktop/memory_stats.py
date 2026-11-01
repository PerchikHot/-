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


def _backup_path(path: Path) -> Path:
    return path.with_suffix(path.suffix + ".bak")


def _valid_record(data: object) -> bool:
    if not isinstance(data, dict):
        return False
    # Older builds may have extra metadata; never discard valid progress
    # merely because an unknown harmless field was saved alongside it.
    if "best_score" in data and "wins" not in data:
        score = data["best_score"]
        return type(score) is int and 0 <= score <= 10_000_000
    if "wins" in data and "best_moves" in data and "best_score" not in data:
        if "date" in data:
            from datetime import date
            day = data["date"]
            if not isinstance(day, str):
                return False
            try:
                if date.fromisoformat(day).isoformat() != day:
                    return False
            except ValueError:
                return False
        wins, best = data["wins"], data["best_moves"]
        return (type(wins) is int and 0 <= wins <= 1_000_000
                and (best is None or (type(best) is int and 2 <= best <= 100_000))
                and (wins != 0 or best is None))
    return False


def load_memory_stats(path: Path, pairs: int = 8) -> dict:
    """Recover the last valid record if the primary was interrupted or damaged."""
    fallback = {"wins": 0, "best_moves": None}
    for candidate in (path, _backup_path(path)):
        try:
            if candidate.stat().st_size > 4096:
                continue
            data = json.loads(candidate.read_text(encoding="utf-8"))
            if not _valid_record(data) or "wins" not in data:
                continue
            best = data["best_moves"]
            if best is not None and best < pairs:
                continue
            return {"wins": data["wins"], "best_moves": best}
        except (OSError, ValueError, TypeError, UnicodeError):
            continue
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


def _atomic_write(path: Path, raw: bytes) -> None:
    temporary = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="wb", dir=path.parent, prefix=".memory-stats-",
            suffix=".tmp", delete=False
        ) as output:
            temporary = Path(output.name)
            output.write(raw)
            output.flush()
            os.fsync(output.fileno())
        os.replace(temporary, path)
    finally:
        if temporary is not None:
            temporary.unlink(missing_ok=True)


def save_memory_stats(path: Path, stats: dict) -> None:
    """Preserve last valid JSON before an atomic replacement of the record."""
    if not _valid_record(stats):
        raise ValueError("Invalid memory-game record")
    path.parent.mkdir(parents=True, exist_ok=True)
    try:
        if path.stat().st_size <= 4096:
            previous = path.read_bytes()
            if _valid_record(json.loads(previous.decode("utf-8"))):
                _atomic_write(_backup_path(path), previous)
    except (OSError, ValueError, TypeError, UnicodeError):
        # Never overwrite a good backup with a corrupt primary.
        pass
    _atomic_write(path, json.dumps(stats, ensure_ascii=False).encode("utf-8"))


def daily_challenge_seed(day: str, pairs: int = 8) -> int:
    """One deterministic 4x4 board per local calendar day."""
    from datetime import date
    parsed = date.fromisoformat(day)
    base = parsed.year * 10000 + parsed.month * 100 + parsed.day
    return base if pairs == 8 else base * 31 + pairs


def load_daily_challenge(path: Path, day: str, pairs: int = 8) -> dict:
    """Recover the current day's result; never restore yesterday's challenge."""
    daily_challenge_seed(day, pairs)
    fallback = {"date": day, "wins": 0, "best_moves": None}
    for candidate in (path, _backup_path(path)):
        try:
            if candidate.stat().st_size > 4096:
                continue
            data = json.loads(candidate.read_text(encoding="utf-8"))
            if not _valid_record(data) or data.get("date") != day:
                continue
            best = data["best_moves"]
            if best is not None and best < pairs:
                continue
            return {"date": day, "wins": data["wins"], "best_moves": best}
        except (OSError, ValueError, TypeError, UnicodeError):
            continue
    return fallback


def record_daily_challenge_win(stats: dict, moves: int, day: str, pairs: int = 8) -> dict:
    """Store the best unassisted result for the current day's fixed board."""
    if stats.get("date") != day:
        stats = {"date": day, "wins": 0, "best_moves": None}
    result = record_memory_win(stats, moves, pairs)
    return {"date": day, **result}


def load_best_score(path: Path) -> int:
    """Normal-mode score only; recover valid backups after interrupted writes."""
    for candidate in (path, _backup_path(path)):
        try:
            if candidate.stat().st_size > 256:
                continue
            data = json.loads(candidate.read_text(encoding="utf-8"))
            if _valid_record(data) and "best_score" in data:
                return data["best_score"]
        except (OSError, ValueError, TypeError, UnicodeError):
            continue
    return 0


def updated_best_score(previous: int, score: int, eligible: bool) -> int:
    if type(previous) is not int or not 0 <= previous <= 10_000_000:
        raise ValueError("Invalid previous score")
    if type(score) is not int or not 0 <= score <= 10_000_000:
        raise ValueError("Invalid round score")
    return max(previous, score) if eligible else previous


def save_best_score(path: Path, score: int) -> None:
    if type(score) is not int or not 0 <= score <= 10_000_000:
        raise ValueError("Invalid score")
    save_memory_stats(path, {"best_score": score})

"""Crash-safe, local-only Nova ordinary-memory round checkpoints.

No gallery paths, photos, personal identifiers or special-mode rewards are
stored. A restored round retains its cards, matches and score but no transient
face-up cards. Corrupt checkpoints never change records or prevent startup.
"""
from __future__ import annotations

import json
import os
import tempfile
import zlib
from collections import Counter
from pathlib import Path

MAX_BYTES = 8192


def _checksum(data: dict) -> str:
    """Detect accidental checkpoint corruption; CRC32 is not authentication."""
    payload = {key: value for key, value in data.items() if key != "checksum"}
    raw = json.dumps(payload, ensure_ascii=False, sort_keys=True,
                     separators=(",", ":")).encode("utf-8")
    return f"{zlib.crc32(raw) & 0xffffffff:08x}"


def validate_session(data, symbols):
    if not isinstance(data, dict) or data.get("version") not in (1, 2):
        raise ValueError("Unsupported memory session")
    if data["version"] == 2:
        checksum = data.get("checksum")
        if (not isinstance(checksum, str) or len(checksum) != 8 or
                any(c not in "0123456789abcdef" for c in checksum) or
                checksum != _checksum(data)):
            raise ValueError("Memory session checksum mismatch")
    cards = data.get("cards")
    if not isinstance(cards, list) or len(cards) not in (4, 8, 12, 16):
        raise ValueError("Invalid board size")
    pairs = len(cards) // 2
    if not all(isinstance(card, str) for card in cards):
        raise ValueError("Invalid card")
    if Counter(cards) != Counter(list(symbols[:pairs]) * 2):
        raise ValueError("Board does not match selected difficulty")
    matched = data.get("matched")
    if (not isinstance(matched, list) or
            any(type(index) is not int or not 0 <= index < len(cards)
                for index in matched) or len(set(matched)) != len(matched)):
        raise ValueError("Invalid matched indices")
    if len(matched) % 2:
        raise ValueError("Incomplete matched pair")
    for symbol in set(cards):
        indices = {index for index, value in enumerate(cards) if value == symbol}
        if indices & set(matched) and not indices <= set(matched):
            raise ValueError("Half of a matched pair")
    moves = data.get("moves")
    hints = data.get("hints_used")
    streak = data.get("match_streak")
    best = data.get("best_streak")
    score = data.get("score")
    if (type(moves) is not int or not len(matched) // 2 <= moves <= 100000 or
            type(hints) is not int or not 0 <= hints <= 2 or
            type(streak) is not int or not 0 <= streak <= pairs or
            type(best) is not int or not streak <= best <= pairs or
            # A streak cannot exceed pairs already solved in this round.
            # Restoring forged progress would inflate the gameplay HUD.
            streak > len(matched) // 2 or best > len(matched) // 2 or
            type(score) is not int or not 0 <= score <= 10000000):
        raise ValueError("Invalid round counters")
    if len(matched) == len(cards):
        raise ValueError("Completed rounds are not resumed")
    result = {
        "version": data["version"], "cards": list(cards),
        "matched": sorted(matched), "moves": moves, "hints_used": hints,
        "match_streak": streak, "best_streak": best, "score": score,
    }
    if data["version"] == 2:
        result["checksum"] = checksum
    return result


def snapshot_session(game, symbols):
    if (game.perfect_mode or game.move_limit is not None or
            game.deadline is not None or game.failed):
        raise ValueError("Only unfinished ordinary rounds can be saved")
    # Validate all counters first, then persist v2 with a deterministic
    # checksum. Old v1 checkpoints remain readable for existing players.
    result = validate_session({
        "version": 1, "cards": list(game.cards),
        "matched": sorted(game.matched), "moves": game.moves,
        "hints_used": game.hints_used, "match_streak": game.match_streak,
        "best_streak": game.best_streak, "score": game.score,
    }, symbols)
    result["version"] = 2
    result["checksum"] = _checksum(result)
    return validate_session(result, symbols)


def restore_session(data, game_class, symbols):
    clean = validate_session(data, symbols)
    game = game_class(seed=0, pair_count=len(clean["cards"]) // 2)
    game.cards = clean["cards"]
    game.matched = set(clean["matched"])
    game.moves = clean["moves"]
    game.hints_used = clean["hints_used"]
    game.match_streak = clean["match_streak"]
    game.best_streak = clean["best_streak"]
    game.score = clean["score"]
    # Never restore a half-flipped mismatch or a stale timed hint.
    game.flipped = []
    game.hint_indices = set()
    return game


def _read(path, symbols):
    if path.stat().st_size > MAX_BYTES:
        raise ValueError("Checkpoint too large")
    return validate_session(json.loads(path.read_text(encoding="utf-8")), symbols)


def load_session(path: Path, game_class, symbols):
    for candidate in (path, path.with_suffix(".bak")):
        try:
            return restore_session(_read(candidate, symbols), game_class, symbols)
        except (OSError, ValueError, TypeError, UnicodeError):
            continue
    return None


def _atomic_session_write(path: Path, raw: bytes) -> None:
    """Replace either checkpoint without exposing a partially written JSON."""
    temporary = None
    try:
        with tempfile.NamedTemporaryFile(mode="wb", dir=path.parent,
                                         prefix=".nova-session-", suffix=".tmp",
                                         delete=False) as stream:
            temporary = Path(stream.name)
            stream.write(raw)
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(temporary, path)
    finally:
        if temporary is not None:
            temporary.unlink(missing_ok=True)


def save_session(path: Path, game, symbols):
    raw = json.dumps(snapshot_session(game, symbols), ensure_ascii=False).encode("utf-8")
    if len(raw) > MAX_BYTES:
        raise ValueError("Checkpoint too large")
    path.parent.mkdir(parents=True, exist_ok=True)
    # Keep the last valid primary as a backup. A crash while replacing the
    # backup must never truncate the previous backup or the current round.
    try:
        previous = _read(path, symbols)
        backup = json.dumps(previous, ensure_ascii=False).encode("utf-8")
        try:
            _atomic_session_write(path.with_suffix(".bak"), backup)
        except OSError:
            # A backup failure must not prevent the newer valid round from
            # being saved. The old backup is preserved by atomic replacement.
            pass
    except (OSError, ValueError, TypeError, UnicodeError):
        pass
    _atomic_session_write(path, raw)


def clear_session(path: Path):
    for candidate in (path, path.with_suffix(".bak")):
        try:
            candidate.unlink(missing_ok=True)
        except OSError:
            pass

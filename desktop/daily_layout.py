"""Shared deterministic daily memory card positions for Android and Windows."""
from datetime import date
import re


def parse_daily_challenge_code(code: str) -> tuple[str, int]:
    """Parse a public board code, without touching images or saved scores."""
    if not isinstance(code, str):
        raise ValueError("Challenge code must be text")
    if len(code) > 64:
        raise ValueError("Challenge code too long")
    code = code.strip()
    match = re.fullmatch(r"NOVA-(\d{4})(\d{2})(\d{2})-P([2-8])", code)
    if match is None:
        raise ValueError("Invalid Nova challenge code")
    day = "-".join(match.group(i) for i in (1, 2, 3))
    try:
        parsed = date.fromisoformat(day)
    except ValueError as exc:
        raise ValueError("Invalid challenge date") from exc
    if parsed.isoformat() != day:
        raise ValueError("Non-canonical challenge date")
    return day, int(match.group(4))


def daily_positions(day: str, pairs: int) -> list[int]:
    if not isinstance(pairs, int) or isinstance(pairs, bool) or not 2 <= pairs <= 8:
        raise ValueError("Pair count must be between 2 and 8")
    parsed = date.fromisoformat(day)
    state = (parsed.year * 10000 + parsed.month * 100 + parsed.day
             ^ (pairs * 0x9E3779B9)) & 0xffffffff
    result = list(range(pairs)) * 2
    for i in range(len(result) - 1, 0, -1):
        state ^= (state << 13) & 0xffffffff
        state ^= state >> 17
        state ^= (state << 5) & 0xffffffff
        state &= 0xffffffff
        j = state % (i + 1)
        result[i], result[j] = result[j], result[i]
    return result


def daily_challenge_code(day: str, pairs: int) -> str:
    """A portable board identifier, never a gallery path or private image."""
    if type(pairs) is not int or not 2 <= pairs <= 8:
        raise ValueError("Unsupported daily challenge difficulty")
    parsed = date.fromisoformat(day)
    if parsed.isoformat() != day:
        raise ValueError("Use ISO date for daily challenge")
    return f"NOVA-{day.replace('-', '')}-P{pairs}"


def shared_challenge_summary(day: str, pairs: int) -> str:
    """Human-readable confirmation; never includes local gallery or save data."""
    code = daily_challenge_code(day, pairs)
    return (f"Код: {code}\nДата: {day}\n"
            f"Карточек: {pairs * 2} ({pairs} пар)\n\n"
            "Текущая партия будет заменена.\n"
            "Это тренировка: рекорды и награды не изменятся.\n\n"
            "Начать испытание?")

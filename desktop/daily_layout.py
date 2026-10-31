"""Shared deterministic daily memory card positions for Android and Windows."""
from datetime import date


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

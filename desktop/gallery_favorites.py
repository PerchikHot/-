"""Local-only Nova gallery favorites. Never modify or upload original images."""
from __future__ import annotations
import json
import os
from pathlib import Path

def favorites_file() -> Path:
    base = Path(os.environ.get("APPDATA") or (Path.home() / ".nova-game"))
    return base / "NovaGame" / "gallery_favorites.json"

def load_favorites(path: Path) -> set[str]:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
        if not isinstance(data, list):
            return set()
        return {item for item in data if isinstance(item, str) and 0 < len(item) < 4096}
    except (OSError, ValueError, UnicodeError):
        return set()

def save_favorites(path: Path, favorites: set[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temp = path.with_suffix(".tmp")
    try:
        temp.write_text(json.dumps(sorted(favorites), ensure_ascii=False, indent=2), encoding="utf-8")
        os.replace(temp, path)
    finally:
        if temp.exists():
            temp.unlink()

def favorite_key(path: Path) -> str:
    return str(path.resolve(strict=False))

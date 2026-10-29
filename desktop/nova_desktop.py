"""Nova Game Windows preview: offline memory game and local-only image gallery.

This is a separate Windows prototype, not Android feature parity.
User images are read on demand and never uploaded or modified.
"""
from __future__ import annotations

import random
import os
import re
import shlex
from pathlib import Path
import tkinter as tk
from tkinter import filedialog, messagebox

from PIL import Image, ImageOps, ImageTk

BACKGROUND = "#090e1c"
PANEL = "#172443"
ACCENT = "#58c9ff"
TEXT = "#e7f0ff"
SYMBOLS = ("★", "◆", "✦", "☾", "♠", "♥", "☀", "♣")
IMAGE_TYPES = {".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp"}


def natural_gallery_key(name: str) -> tuple:
    """Sort 2.png before 10.png without reading image contents."""
    return tuple((1, int(part)) if part.isdigit() else (0, part.casefold())
                 for part in re.split(r"(\d+)", name))


def collect_gallery_images(root: Path, max_images: int = 20_000) -> list[Path]:
    """Include nested folders without following links or indexing endlessly."""
    if max_images <= 0 or not root.is_dir():
        return []
    images: list[Path] = []
    for current, dirs, files in os.walk(root, followlinks=False):
        # Do not follow junctions/symlinks to unrelated or cyclic directories.
        dirs[:] = sorted((d for d in dirs if not (Path(current) / d).is_symlink()),
                         key=natural_gallery_key)
        for name in sorted(files, key=natural_gallery_key):
            path = Path(current) / name
            if path.suffix.lower() in IMAGE_TYPES and not path.is_symlink() and path.is_file():
                images.append(path)
                if len(images) >= max_images:
                    return images
    return images


def random_candidate_indices(count: int, limit: int = 12, rng=None) -> list[int]:
    """Try distinct local files in random order, without retrying broken ones."""
    if count <= 0 or limit <= 0:
        return []
    return (rng or random).sample(range(count), min(count, limit))

def unseen_candidate_indices(count: int, seen: set[int], limit: int = 12, rng=None) -> list[int]:
    """Avoid repeats until the entire filtered gallery has been attempted.

    Failed images count as attempted, so broken files do not monopolize
    the random button. A new cycle begins only after all have been tried.
    """
    if count <= 0 or limit <= 0:
        return []
    remaining = [i for i in range(count) if i not in seen]
    if not remaining:
        seen.clear()
        remaining = list(range(count))
    return (rng or random).sample(remaining, min(limit, len(remaining)))


def gallery_matches_query(path: Path, query: str, root: Path | None = None) -> bool:
    """Search filenames and relative folder names without reading image data."""
    # A folder named after a character should be searchable even when
    # the files inside are named 001.png, 002.png, etc.
    try:
        searchable = str(path.relative_to(root)) if root is not None else path.name
    except ValueError:
        searchable = path.name
    text = searchable.casefold()
    folder_text = str(Path(searchable).parent).casefold() if root is not None else ''
    # Quoted filters support character/folder names containing spaces.
    # An unfinished quote is an invalid filter, not a match-all query.
    try:
        terms = shlex.split(query.casefold())
    except ValueError:
        return False
    for word in terms:
        # Exclude a format explicitly. Plain '-ext:gif' cannot be treated as
        # text because the filename normally contains no 'ext:' substring.
        if word.startswith('-ext:'):
            formats = word[5:].split(',')
            extensions = {f".{part.lstrip('.')}" for part in formats}
            if (any(not part or part == '.' for part in formats)
                    or not extensions.issubset(IMAGE_TYPES)):
                return False
            if path.suffix.casefold() in extensions:
                return False
            continue
        if word.startswith('-folder:'):
            excluded_folder = word[8:]
            if not excluded_folder or root is None or excluded_folder in folder_text:
                return False
            continue
        if word.startswith('-name:'):
            excluded_name = word[6:]
            if not excluded_name or excluded_name in path.name.casefold():
                return False
            continue
        # A leading minus excludes matching images or folders, e.g.
        # 'эрза -черновик'. Reject a lone minus instead of matching all.
        if word.startswith('-'):
            excluded = word[1:]
            if not excluded or excluded in text:
                return False
            continue
        if word.startswith('folder:'):
            # Restrict a filter to folder names, not a matching filename.
            if not word[7:] or word[7:] not in folder_text:
                return False
        elif word.startswith('name:'):
            # Search the filename only; character folders are excluded.
            if not word[5:] or word[5:] not in path.name.casefold():
                return False
        elif word.startswith('ext:'):
            # Accept a comma-separated set of formats, e.g. ext:png,jpg,
            # without scanning or decoding any image content.
            formats = word[4:].split(',')
            extensions = {f".{part.lstrip('.')}" for part in formats}
            if (any(not part or part == '.' for part in formats)
                    or not extensions.issubset(IMAGE_TYPES)
                    or path.suffix.casefold() not in extensions):
                return False
        elif word not in text:
            return False
    return True


def is_oversized_image(width: int, height: int, max_pixels: int = 80_000_000) -> bool:
    """Reject extreme or malformed image dimensions before full decoding."""
    return width <= 0 or height <= 0 or width * height > max_pixels

def is_safe_gallery_preview(path: Path, max_bytes: int = 128 * 1024 * 1024) -> bool:
    """Recheck files before opening: a scanned file may have been replaced.

    Avoid reading symlink targets outside the chosen folder or oversized
    files that would consume excessive memory. This never modifies photos.
    """
    return (max_bytes > 0 and not path.is_symlink() and path.is_file()
            and path.stat().st_size <= max_bytes)


def memory_key_to_index(key: str) -> int | None:
    """Map 1–9 and A–G to the sixteen cards, without touching gallery keys."""
    normalized = key.lower()
    if normalized in "123456789" and len(normalized) == 1:
        return int(normalized) - 1
    if len(normalized) == 1 and "a" <= normalized <= "g":
        return 9 + ord(normalized) - ord("a")
    return None


def is_new_game_key(key: str) -> bool:
    """Restart only from the memory tab; ignore unrelated shortcuts."""
    return key.lower() == "n"


class MemoryGame:
    """State machine kept independent of tkinter for deterministic tests."""

    def __init__(self, seed: int | None = None):
        self.cards = list(SYMBOLS) * 2
        random.Random(seed).shuffle(self.cards)
        self.matched: set[int] = set()
        self.flipped: list[int] = []
        self.moves = 0

    def choose(self, index: int) -> str:
        if index not in range(len(self.cards)) or index in self.matched:
            return "ignored"
        if index in self.flipped or len(self.flipped) >= 2:
            return "ignored"
        self.flipped.append(index)
        if len(self.flipped) == 1:
            return "first"
        self.moves += 1
        left, right = self.flipped
        if self.cards[left] == self.cards[right]:
            self.matched.update((left, right))
            self.flipped.clear()
            return "win" if len(self.matched) == len(self.cards) else "match"
        return "miss"

    def hide_mismatch(self) -> None:
        if len(self.flipped) == 2:
            self.flipped.clear()


def gallery_shortcut_action(keysym: str, state: int) -> str | None:
    """Windows-friendly gallery shortcuts, independent of focused widget."""
    if keysym.lower() == "f" and state & 0x4:  # Tk ControlMask
        return "focus"
    if keysym == "Escape":
        return "clear"
    return None


class NovaDesktop(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("Nova Game — Windows Preview")
        self.geometry("1080x720")
        self.minsize(800, 560)
        self.configure(bg=BACKGROUND)
        self.game = MemoryGame()
        self.buttons: list[tk.Button] = []
        self.gallery_files: list[Path] = []
        self.gallery_root: Path | None = None
        self.visible_gallery_indices: list[int] = []
        self._seen_random_indices: set[int] = set()
        self.gallery_filter = tk.StringVar()
        self.gallery_photo = None
        self._pending_hide = None
        self._build()
        self.bind("<KeyPress>", self._handle_memory_key)

    def _label(self, parent, title, size=13):
        return tk.Label(parent, text=title, bg=BACKGROUND, fg=TEXT,
                        font=("Segoe UI", size))

    def _button(self, parent, title, action):
        return tk.Button(parent, text=title, command=action, bg=PANEL, fg=TEXT,
                         activebackground="#2857a0", activeforeground=TEXT,
                         relief="flat", padx=14, pady=10, cursor="hand2")

    def _build(self):
        header = tk.Frame(self, bg=BACKGROUND)
        header.pack(fill="x", padx=24, pady=16)
        self._label(header, "VEYRION  /  NOVA GAME", 23).pack(side="left")
        self._label(header, "Windows preview • offline", 11).pack(side="right")
        tabs = tk.Frame(self, bg=BACKGROUND)
        tabs.pack(fill="x", padx=24)
        self._button(tabs, "Игра на память", lambda: self._show("memory")).pack(side="left", padx=4)
        self._button(tabs, "Моя галерея", lambda: self._show("gallery")).pack(side="left", padx=4)
        self.content = tk.Frame(self, bg=BACKGROUND)
        self.content.pack(fill="both", expand=True, padx=24, pady=12)
        self._show("memory")

    def _show(self, page: str):
        self.current_page = page
        # A delayed flip must never update widgets after switching tabs.
        if self._pending_hide is not None:
            self.after_cancel(self._pending_hide)
            self._pending_hide = None
            self.game.hide_mismatch()
        self.buttons = []
        for child in self.content.winfo_children():
            child.destroy()
        if page == "memory":
            self._memory_page()
        else:
            self._gallery_page()

    def _memory_page(self):
        top = tk.Frame(self.content, bg=BACKGROUND)
        top.pack(fill="x", pady=12)
        self.status = self._label(top, "", 15)
        self.status.pack(side="left")
        self._button(top, "Новая игра", self._new_game).pack(side="right")
        self._label(top, "Карточки: 1–9, A–G • N — новая игра", 10).pack(side="right", padx=12)
        grid = tk.Frame(self.content, bg=BACKGROUND)
        grid.pack(expand=True)
        self.buttons = []
        for i in range(16):
            button = tk.Button(
                grid, command=lambda index=i: self._flip(index),
                width=7, height=3, font=("Segoe UI Symbol", 24),
                bg=PANEL, fg=ACCENT, relief="flat",
                activebackground="#2857a0", activeforeground=TEXT,
            )
            button.grid(row=i // 4, column=i % 4, padx=6, pady=6)
            self.buttons.append(button)
        self._render_memory()

    def _render_memory(self):
        if not self.buttons:
            return
        self.status.config(text=f"Ходы: {self.game.moves}  •  Пары: {len(self.game.matched) // 2}/8")
        for i, button in enumerate(self.buttons):
            visible = i in self.game.flipped or i in self.game.matched
            button.config(
                text=self.game.cards[i] if visible else "?",
                bg="#225c83" if i in self.game.matched else PANEL,
                fg=TEXT if visible else ACCENT,
            )

    def _flip(self, index: int):
        result = self.game.choose(index)
        self._render_memory()
        if result == "miss":
            self._pending_hide = self.after(700, self._hide_mismatch)
        elif result == "win":
            messagebox.showinfo("Победа", f"Все пары найдены за {self.game.moves} ходов!")

    def _hide_mismatch(self):
        self._pending_hide = None
        self.game.hide_mismatch()
        self._render_memory()

    def _handle_memory_key(self, event):
        action = gallery_shortcut_action(event.keysym, event.state)
        if action == "focus":
            if self.current_page != "gallery":
                self._show("gallery")
            self.gallery_filter_entry.focus_set()
            self.gallery_filter_entry.select_range(0, tk.END)
            return "break"
        if self.current_page == "gallery" and action == "clear":
            self._clear_gallery_filter()
            return "break"
        if self.current_page != "memory":
            return
        if is_new_game_key(event.keysym):
            self._new_game()
            return
        index = memory_key_to_index(event.keysym)
        if index is not None:
            self._flip(index)

    def _new_game(self):
        # Cancel the old game's pending mismatch before replacing its state.
        if self._pending_hide is not None:
            self.after_cancel(self._pending_hide)
            self._pending_hide = None
        self.game = MemoryGame()
        self._show("memory")

    def _gallery_page(self):
        toolbar = tk.Frame(self.content, bg=BACKGROUND)
        toolbar.pack(fill="x", pady=8)
        self._button(toolbar, "Выбрать папку", self._choose_folder).pack(side="left", padx=4)
        self._button(toolbar, "Обновить список", self._reload_folder).pack(side="left", padx=4)
        self._button(toolbar, "Случайное без повторов", self._random_image).pack(side="left", padx=4)
        filter_box = tk.Entry(toolbar, textvariable=self.gallery_filter, width=20,
                              bg=PANEL, fg=TEXT, insertbackground=TEXT, relief="flat")
        filter_box.pack(side="left", padx=6, ipady=8)
        self.gallery_filter_entry = filter_box
        filter_box.bind("<KeyRelease>", lambda _event: self._refresh_gallery())
        self._button(toolbar, "Очистить", self._clear_gallery_filter).pack(side="left", padx=4)
        self.gallery_status = self._label(toolbar, "Выбери папку с изображениями", 12)
        self.gallery_status.pack(side="left", padx=15)
        panel = tk.Frame(self.content, bg=BACKGROUND)
        panel.pack(fill="both", expand=True)
        self.gallery_list = tk.Listbox(
            panel, bg=PANEL, fg=TEXT, selectbackground="#2758a4",
            relief="flat", width=32, font=("Segoe UI", 11),
        )
        self.gallery_list.pack(side="left", fill="y", padx=(0, 10))
        self.gallery_list.bind("<<ListboxSelect>>", self._selected_image)
        self.gallery_preview = tk.Label(
            panel, bg=PANEL, fg=TEXT, text="Изображения остаются на компьютере",
            font=("Segoe UI", 13),
        )
        self.gallery_preview.pack(side="left", fill="both", expand=True)
        tk.Label(self.content, text="Ctrl+F — поиск  •  Esc — очистить  •  Фильтры: ext:png,jpg  •  folder:Эрза  •  folder:\"Моя папка\"  •  name:001  •  -черновик  •  -ext:gif",
                 bg=BACKGROUND, fg=ACCENT, font=("Segoe UI", 10)).pack(anchor="w", pady=(0, 6))
        self._refresh_gallery()

    def _clear_gallery_filter(self):
        self.gallery_filter.set("")
        self._refresh_gallery()

    def _choose_folder(self):
        folder = filedialog.askdirectory(title="Папка с изображениями")
        if not folder:
            return
        root = Path(folder)
        try:
            files = collect_gallery_images(root)
        except OSError as exc:
            messagebox.showerror("Ошибка", str(exc))
            return
        self.gallery_root = root
        self.gallery_files = files
        self._refresh_gallery()

    def _reload_folder(self):
        """Pick up new/deleted local photos without reselecting the directory."""
        if self.gallery_root is None:
            self.gallery_status.config(text="Сначала выбери папку с изображениями")
            return
        try:
            updated = collect_gallery_images(self.gallery_root)
        except OSError as exc:
            messagebox.showerror("Ошибка обновления", str(exc))
            return
        self.gallery_files = updated
        self._refresh_gallery()

    def _refresh_gallery(self):
        if not hasattr(self, "gallery_list"):
            return
        self.gallery_list.delete(0, tk.END)
        # A different filter/folder starts a fresh random shuffle.
        self._seen_random_indices.clear()
        self.visible_gallery_indices = [
            index for index, path in enumerate(self.gallery_files)
            if gallery_matches_query(path, self.gallery_filter.get(), self.gallery_root)
        ]
        for index in self.visible_gallery_indices:
            path = self.gallery_files[index]
            self.gallery_list.insert(tk.END, str(path.relative_to(self.gallery_root)) if self.gallery_root else path.name)
        self.gallery_status.config(
            text=f"Показано: {len(self.visible_gallery_indices)} / {len(self.gallery_files)}"
        )

    def _selected_image(self, _event=None):
        selection = self.gallery_list.curselection()
        if selection:
            self._open_image(self.gallery_files[self.visible_gallery_indices[selection[0]]])

    def _random_image(self):
        if not self.gallery_files:
            messagebox.showinfo("Галерея", "Сначала выбери папку с изображениями.")
            return
        if not self.visible_gallery_indices:
            self.gallery_status.config(text="Нет изображений по запросу")
            return
        for visible_index in unseen_candidate_indices(
                len(self.visible_gallery_indices), self._seen_random_indices):
            self._seen_random_indices.add(visible_index)
            index = self.visible_gallery_indices[visible_index]
            if self._open_image(self.gallery_files[index]):
                self.gallery_list.selection_clear(0, tk.END)
                self.gallery_list.selection_set(visible_index)
                self.gallery_list.see(visible_index)
                self.gallery_status.config(
                    text=f"Без повторов: {len(self._seen_random_indices)} / {len(self.visible_gallery_indices)}"
                )
                return
        self.gallery_status.config(text="Не удалось открыть проверенные изображения — попробуй ещё раз")

    def _open_image(self, path: Path):
        try:
            if not is_safe_gallery_preview(path):
                raise ValueError("Файл слишком большой, отсутствует или стал ссылкой")
            with Image.open(path) as source:
                if is_oversized_image(*source.size):
                    raise ValueError("Изображение слишком большое для предпросмотра")
                image = ImageOps.exif_transpose(source)
                image.thumbnail((900, 650), Image.Resampling.LANCZOS)
                preview = image.convert("RGBA").copy()
            self.gallery_photo = ImageTk.PhotoImage(preview)
            self.gallery_preview.config(image=self.gallery_photo, text="")
            return True
        except (OSError, ValueError, Image.DecompressionBombError) as exc:
            self.gallery_photo = None
            self.gallery_preview.config(image="", text=f"Не удалось открыть {path.name}: {exc}")
            return False


if __name__ == "__main__":
    NovaDesktop().mainloop()

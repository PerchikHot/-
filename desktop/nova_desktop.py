"""Nova Game Windows preview: offline memory game and local-only image gallery.

This is a separate Windows prototype, not Android feature parity.
User images are read on demand and never uploaded or modified.
"""
from __future__ import annotations

import random
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


def random_candidate_indices(count: int, limit: int = 12, rng=None) -> list[int]:
    """Try distinct local files in random order, without retrying broken ones."""
    if count <= 0 or limit <= 0:
        return []
    return (rng or random).sample(range(count), min(count, limit))


def gallery_matches_query(path: Path, query: str) -> bool:
    """Match all filename search words in any order; never reads image contents."""
    filename = path.name.casefold()
    return all(word in filename for word in query.casefold().split())


def is_oversized_image(width: int, height: int, max_pixels: int = 80_000_000) -> bool:
    """Reject extreme or malformed image dimensions before full decoding."""
    return width <= 0 or height <= 0 or width * height > max_pixels


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
        self.visible_gallery_indices: list[int] = []
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
        self._button(toolbar, "Случайное фото", self._random_image).pack(side="left", padx=4)
        filter_box = tk.Entry(toolbar, textvariable=self.gallery_filter, width=20,
                              bg=PANEL, fg=TEXT, insertbackground=TEXT, relief="flat")
        filter_box.pack(side="left", padx=6, ipady=8)
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
            self.gallery_files = sorted(
                (p for p in root.iterdir()
                 if p.is_file() and p.suffix.lower() in IMAGE_TYPES),
                key=lambda p: p.name.lower(),
            )
        except OSError as exc:
            messagebox.showerror("Ошибка", str(exc))
            return
        self._refresh_gallery()

    def _refresh_gallery(self):
        if not hasattr(self, "gallery_list"):
            return
        self.gallery_list.delete(0, tk.END)
        self.visible_gallery_indices = [
            index for index, path in enumerate(self.gallery_files)
            if gallery_matches_query(path, self.gallery_filter.get())
        ]
        for index in self.visible_gallery_indices:
            self.gallery_list.insert(tk.END, self.gallery_files[index].name)
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
        for visible_index in random_candidate_indices(len(self.visible_gallery_indices)):
            index = self.visible_gallery_indices[visible_index]
            if self._open_image(self.gallery_files[index]):
                self.gallery_list.selection_clear(0, tk.END)
                self.gallery_list.selection_set(visible_index)
                self.gallery_list.see(visible_index)
                return
        self.gallery_status.config(text="Не удалось открыть доступные изображения")

    def _open_image(self, path: Path):
        try:
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

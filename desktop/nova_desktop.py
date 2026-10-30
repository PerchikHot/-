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
SLIDESHOW_DELAYS_MS = (3000, 5000, 10000)


def next_slideshow_delay_ms(current: int) -> int:
    """Cycle through safe, predictable intervals for the local slideshow."""
    try:
        index = SLIDESHOW_DELAYS_MS.index(current)
    except ValueError:
        return 5000
    return SLIDESHOW_DELAYS_MS[(index + 1) % len(SLIDESHOW_DELAYS_MS)]


def natural_gallery_key(name: str) -> tuple:
    """Sort 2.png before 10.png without reading image contents."""
    return tuple((1, int(part)) if part.isdigit() else (0, part.casefold())
                 for part in re.split(r"(\d+)", name))


def is_linked_gallery_directory(path: Path) -> bool:
    """Treat Windows junctions like symlinks to avoid leaving the chosen tree."""
    try:
        return path.is_symlink() or getattr(path, "is_junction", lambda: False)()
    except OSError:
        # Inaccessible junction metadata must not abort sibling scanning.
        return True


def collect_gallery_images(root: Path, max_images: int = 20_000) -> list[Path]:
    """Include nested folders without following links or indexing endlessly."""
    if max_images <= 0:
        return []
    try:
        if is_linked_gallery_directory(root) or not root.is_dir():
            return []
    except OSError:
        # A removed or permission-denied library should not crash the UI.
        return []
    images: list[Path] = []
    for current, dirs, files in os.walk(root, followlinks=False):
        # Do not follow junctions/symlinks to unrelated or cyclic directories.
        dirs[:] = sorted((d for d in dirs if not is_linked_gallery_directory(Path(current) / d)),
                         key=natural_gallery_key)
        for name in sorted(files, key=natural_gallery_key):
            path = Path(current) / name
            if path.suffix.lower() not in IMAGE_TYPES:
                continue
            try:
                # A removed or locked file must not abort the entire scan.
                if path.is_symlink() or not path.is_file():
                    continue
            except OSError:
                continue
            images.append(path)
            if len(images) >= max_images:
                return images
    return images


def gallery_step_index(current: int | None, count: int, direction: int) -> int | None:
    """Navigate filtered images with wraparound, including page-sized jumps."""
    if count <= 0 or direction == 0:
        return None
    if current is None or not 0 <= current < count:
        return 0 if direction > 0 else count - 1
    return (current + direction) % count


def gallery_preview_is_visible(previewed: Path | None, visible: list[Path]) -> bool:
    """Keep a preview only while its file belongs to the filtered gallery."""
    return gallery_preview_index(previewed, visible) is not None


def gallery_preview_index(previewed: Path | None, visible: list[Path]) -> int | None:
    """Preserve the selected image across rescan/filter refreshes."""
    if previewed is None:
        return None
    try:
        return visible.index(previewed)
    except ValueError:
        return None


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
    # Path separators differ between Windows and Linux CI; normalize only
    # the searchable representation, never the user's actual file paths.
    text = searchable.replace('\\', '/').casefold()
    # pathlib on Linux treats Windows backslashes as ordinary characters.
    # Derive the folder from normalized relative text for portable filters.
    folder_text = text.rsplit('/', 1)[0] if root is not None and '/' in text else ''
    # Quoted filters support character/folder names containing spaces.
    # An unfinished quote is an invalid filter, not a match-all query.
    try:
        # Accept pasted Windows-style folder paths in path:/folder: filters.
        # Normalize only the query, never the actual filesystem paths.
        terms = shlex.split(query.casefold().replace('\\', '/'))
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
        if word.startswith('-path:'):
            # A comma-separated list excludes any matching relative path.
            excluded_paths = word[6:].split(',')
            if (root is None or not all(excluded_paths)
                    or any(part in text for part in excluded_paths)):
                return False
            continue
        if word.startswith('path:'):
            # Match any supplied relative path, preserving AND across filters.
            included_paths = word[5:].split(',')
            if (root is None or not all(included_paths)
                    or not any(part in text for part in included_paths)):
                return False
            continue
        if word.startswith('-folder:'):
            excluded_folders = word[8:].split(',')
            if (root is None or not all(excluded_folders)
                    or any(folder in folder_text for folder in excluded_folders)):
                return False
            continue
        if word.startswith('-name:'):
            excluded_names = word[6:].split(',')
            if (not all(excluded_names)
                    or any(name in path.name.casefold() for name in excluded_names)):
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
            folders = word[7:].split(',')
            if not all(folders) or not any(folder in folder_text for folder in folders):
                return False
        elif word.startswith('name:'):
            # Search the filename only; character folders are excluded.
            names = word[5:].split(',')
            if not all(names) or not any(name in path.name.casefold() for name in names):
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
    if max_bytes <= 0:
        return False
    try:
        return (not path.is_symlink() and path.is_file()
                and path.stat().st_size <= max_bytes)
    except OSError:
        # Files can disappear or become unreadable after the gallery scan.
        return False


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
    if keysym.lower() in ("f", "l") and state & 0x4:  # Tk ControlMask
        return "focus"
    if keysym.lower() == "r" and state & 0x4:
        return "refresh"
    if keysym.lower() == "o" and state & 0x4:
        return "choose_folder"
    if keysym == "Escape":
        return "clear"
    if keysym == "F4":
        return "copy_path"
    if keysym == "F5":
        return "refresh"
    if keysym == "F6":
        return "toggle_slideshow"
    if keysym == "F7":
        return "cycle_speed"
    if keysym == "F8":
        return "random_image"
    if keysym == "F9":
        return "previous_image"
    if keysym == "F10":
        return "next_image"
    if keysym == "F11":
        return "toggle_fullscreen"
    if keysym == "Prior":  # PageUp in Tk
        return "previous_page"
    if keysym == "Next":  # PageDown in Tk
        return "next_page"
    if keysym == "Home":
        return "first_image"
    if keysym == "End":
        return "last_image"
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
        self._previewed_path: Path | None = None
        self._pending_hide = None
        self._slideshow_job = None
        self.slideshow_enabled = False
        self.slideshow_delay_ms = 5000
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
        # Cancel the timer before destroying the gallery widgets.
        self._stop_slideshow()
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
        if action == "toggle_fullscreen":
            # A reversible desktop-only display preference; no image files change.
            self.attributes("-fullscreen", not bool(self.attributes("-fullscreen")))
            return "break"
        if action == "focus":
            if self.current_page != "gallery":
                self._show("gallery")
            self.gallery_filter_entry.focus_set()
            self.gallery_filter_entry.select_range(0, tk.END)
            return "break"
        if action == "clear" and bool(self.attributes("-fullscreen")):
            # Escape should always offer a way out of immersive mode.
            self.attributes("-fullscreen", False)
            return "break"
        if self.current_page == "gallery" and action == "clear":
            self._clear_gallery_filter()
            return "break"
        if self.current_page == "gallery" and action == "choose_folder":
            self._choose_folder()
            return "break"
        if self.current_page == "gallery" and action == "copy_path":
            self._copy_gallery_path()
            return "break"
        if self.current_page == "gallery" and action == "refresh":
            self._reload_folder()
            return "break"
        if self.current_page == "gallery" and action == "toggle_slideshow":
            self._toggle_slideshow()
            return "break"
        if self.current_page == "gallery" and action == "cycle_speed":
            self._cycle_slideshow_speed()
            return "break"
        if self.current_page == "gallery" and action == "random_image":
            self._random_image()
            return "break"
        if self.current_page == "gallery" and action == "previous_image":
            return self._step_gallery(-1)
        if self.current_page == "gallery" and action == "next_image":
            return self._step_gallery(1)
        if self.current_page == "gallery" and action in ("previous_page", "next_page"):
            # Preserve ordinary PageUp/PageDown editing in the search field.
            if getattr(event, "widget", None) is getattr(self, "gallery_filter_entry", object()):
                return
            return self._step_gallery(-10 if action == "previous_page" else 10)
        if self.current_page == "gallery" and action in ("first_image", "last_image"):
            # Do not steal Home/End when editing the search field.
            if getattr(event, "widget", None) is getattr(self, "gallery_filter_entry", object()):
                return
            return self._jump_gallery(action == "first_image")
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
        self.slideshow_button = self._button(toolbar, "▶ Слайд-шоу", self._toggle_slideshow)
        self.slideshow_button.pack(side="left", padx=4)
        self.slideshow_speed_button = self._button(toolbar, "5с", self._cycle_slideshow_speed)
        self.slideshow_speed_button.pack(side="left", padx=2)
        self._button(toolbar, "◀", lambda: self._step_gallery(-1)).pack(side="left", padx=2)
        self._button(toolbar, "▶", lambda: self._step_gallery(1)).pack(side="left", padx=2)
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
        self.gallery_list.bind("<Left>", lambda _event: self._step_gallery(-1))
        self.gallery_list.bind("<Right>", lambda _event: self._step_gallery(1))
        self.gallery_preview = tk.Label(
            panel, bg=PANEL, fg=TEXT, text="Изображения остаются на компьютере",
            font=("Segoe UI", 13),
        )
        self.gallery_preview.pack(side="left", fill="both", expand=True)
        tk.Label(self.content, text="Ctrl+F — поиск  •  Esc — очистить  •  F4 — копировать путь  •  F5 — обновить  •  F6 — слайд-шоу  •  F7 — скорость  •  F8 — случайное  •  F11 — полный экран  •  ◀/▶ — листать  •  Фильтры: ext:png,jpg  •  folder:Эрза,Риас  •  folder:\"Моя папка\"  •  name:001  •  -черновик  •  -ext:gif",
                 bg=BACKGROUND, fg=ACCENT, font=("Segoe UI", 10)).pack(anchor="w", pady=(0, 6))
        self._refresh_gallery()

    def _clear_gallery_filter(self):
        self.gallery_filter.set("")
        self._refresh_gallery()

    def _choose_folder(self):
        # Reopen the picker in the current library rather than at the disk root.
        options = {"title": "Папка с изображениями"}
        if self.gallery_root is not None and self.gallery_root.is_dir():
            options["initialdir"] = str(self.gallery_root)
        folder = filedialog.askdirectory(**options)
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
        if not self.visible_gallery_indices and self.slideshow_enabled:
            self._stop_slideshow()
        # A removed or newly excluded image must not remain in the preview.
        visible_paths = [self.gallery_files[i] for i in self.visible_gallery_indices]
        if self._previewed_path is not None and not gallery_preview_is_visible(
                self._previewed_path, visible_paths):
            self._previewed_path = None
            self.gallery_photo = None
            self.gallery_preview.config(image="", text="Выбери изображение из списка")
        for index in self.visible_gallery_indices:
            path = self.gallery_files[index]
            self.gallery_list.insert(tk.END, str(path.relative_to(self.gallery_root)) if self.gallery_root else path.name)
        # Keep Next/Previous anchored to the same image after a rescan.
        selected = gallery_preview_index(self._previewed_path, visible_paths)
        if selected is not None:
            self.gallery_list.selection_set(selected)
            self.gallery_list.see(selected)
        self.gallery_status.config(
            text=f"Показано: {len(self.visible_gallery_indices)} / {len(self.gallery_files)}"
        )

    def _copy_gallery_path(self):
        """Copy the selected local file path on request, without opening it."""
        selection = self.gallery_list.curselection()
        if not selection or selection[0] >= len(self.visible_gallery_indices):
            self.gallery_status.config(text="Сначала выбери изображение в списке")
            return
        path = self.gallery_files[self.visible_gallery_indices[selection[0]]]
        self.clipboard_clear()
        self.clipboard_append(str(path.absolute()))
        self.gallery_status.config(text="Путь к изображению скопирован")

    def _selected_image(self, _event=None):
        selection = self.gallery_list.curselection()
        if selection:
            self._open_image(self.gallery_files[self.visible_gallery_indices[selection[0]]])

    def _step_gallery(self, direction: int):
        """Move through the filtered list without touching the source files."""
        selection = self.gallery_list.curselection()
        current = selection[0] if selection else None
        target = gallery_step_index(current, len(self.visible_gallery_indices), direction)
        if target is None:
            return "break"
        self.gallery_list.selection_clear(0, tk.END)
        self.gallery_list.selection_set(target)
        self.gallery_list.see(target)
        self._selected_image()
        return "break"

    def _jump_gallery(self, first: bool):
        """Jump to either edge of the current filtered list."""
        count = len(self.visible_gallery_indices)
        if not count:
            return "break"
        target = 0 if first else count - 1
        self.gallery_list.selection_clear(0, tk.END)
        self.gallery_list.selection_set(target)
        self.gallery_list.see(target)
        self._selected_image()
        return "break"

    def _stop_slideshow(self):
        if self._slideshow_job is not None:
            self.after_cancel(self._slideshow_job)
            self._slideshow_job = None
        self.slideshow_enabled = False
        button = getattr(self, "slideshow_button", None)
        if button is not None and button.winfo_exists():
            button.config(text="▶ Слайд-шоу")

    def _toggle_slideshow(self):
        if self.slideshow_enabled:
            self._stop_slideshow()
            return
        if not self.visible_gallery_indices:
            self.gallery_status.config(text="Нет изображений для слайд-шоу")
            return
        self.slideshow_enabled = True
        self.slideshow_button.config(text="■ Стоп")
        self._slideshow_tick()

    def _cycle_slideshow_speed(self):
        self.slideshow_delay_ms = next_slideshow_delay_ms(self.slideshow_delay_ms)
        self.slideshow_speed_button.config(text=f"{self.slideshow_delay_ms // 1000}с")
        # Apply changes immediately without accumulating multiple timers.
        if self.slideshow_enabled and self._slideshow_job is not None:
            self.after_cancel(self._slideshow_job)
            self._slideshow_job = self.after(self.slideshow_delay_ms, self._slideshow_tick)

    def _slideshow_tick(self):
        self._slideshow_job = None
        if (not self.slideshow_enabled or self.current_page != "gallery"
                or not self.visible_gallery_indices):
            self._stop_slideshow()
            return
        # Do not decode new photos while the preview window is minimized.
        # Keep the slideshow enabled so it resumes on the next tick.
        if self.state() == "iconic":
            self._slideshow_job = self.after(getattr(self, "slideshow_delay_ms", 5000), self._slideshow_tick)
            return
        # Reuse the no-repeat selector and its file safety checks.
        self._random_image()
        self._slideshow_job = self.after(getattr(self, "slideshow_delay_ms", 5000), self._slideshow_tick)

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
            self._previewed_path = path
            self.gallery_preview.config(image=self.gallery_photo, text="")
            return True
        except (OSError, ValueError, Image.DecompressionBombError) as exc:
            self.gallery_photo = None
            self._previewed_path = None
            self.gallery_preview.config(image="", text=f"Не удалось открыть {path.name}: {exc}")
            return False


if __name__ == "__main__":
    NovaDesktop().mainloop()

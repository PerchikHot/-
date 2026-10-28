# Новая игра
Android mini-game collection (Kotlin, Android SDK 26+).
Version 0.1.0. Includes playable memory, tap-to-reveal, puzzle, match-3 and spot-the-difference prototypes. Saves unlocked progress locally.
Build: GitHub Actions → Android APK → artifact NovaGame-debug-apk.
The user's image archive is not committed to this public repository. Only import images for which distribution is authorized.


## Windows preview

GitHub Actions → **Nova Windows EXE** → artifact `NovaGame-Windows-preview`.
This is a **separate, early Windows prototype**, not a port of every Android game:
it includes a 4×4 memory game and an offline gallery that reads images from
a folder selected by the user. It does not import Android saves or sync files.
The Windows executable is packaged with PyInstaller and is not an installer.
Run the EXE from the downloaded artifact; no user images are bundled in it.

To test locally: install Python 3.12 and Pillow, then run
`python desktop/nova_desktop.py`. To run the headless game-state tests:
`python -m unittest discover -s desktop -p "test_*.py"`.

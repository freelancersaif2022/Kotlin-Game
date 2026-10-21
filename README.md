# Snake Attack 🐍

A casual puzzle-shooter for Android written in **Kotlin** with **libGDX**.

A giant dragon-snake crawls down the road towards the girl. Tap the arrow
blocks on the board: a block can only slide out if nothing stands in the way
of its arrow. Freed blocks fly into the cannon slots and become cannons that
shoot fireballs at body segments of the **same colour**. Destroy the whole
body before the dragon reaches her!

## Features

- Endless procedurally generated levels (every board is guaranteed solvable, see `LevelGenTest`)
- Skeletal (cut-out) animation for the girl and the dragon head: running,
  waving, cheering, panic, jaw bites, blinking, ponytail and frill physics
- Spring physics for squash & stretch, recoil, knock-back and wobble
- Particles: fire trails, explosions, sparks, smoke, confetti, coins
- Synthesised sound effects and two music loops
- Stars, coins, hints, an unlockable 5th slot, pause menu, saved progress
- Sound / music / vibration toggles

## Download the APK

Every push builds a signed APK with GitHub Actions
(`.github/workflows/build-apk.yml`). Grab the latest one from the
repository's **Releases** page (or the run's *SnakeAttack-apk* artifact),
open it on your phone and allow installing from unknown sources.

## Project layout

| Path | What |
|---|---|
| `core/` | All game code (Kotlin) |
| `android/` | Android launcher, manifest, icons |
| `lwjgl3/` | Desktop launcher for quick testing |
| `assets/` | Generated runtime assets (atlas, fonts, sounds, music) |
| `art/` | Source artwork, font |
| `tools/build_assets.py` | Builds the sprite atlas, fonts and launcher icons from `art/` |
| `tools/make_sounds.py` | Synthesises every sound effect and music track |

## Building

```bash
# desktop (no Android SDK needed)
./gradlew :lwjgl3:run

# Android APK (needs the Android SDK, ANDROID_HOME set)
./gradlew :android:assembleRelease

# unit tests
./gradlew :core:test

# regenerate assets after changing art/ or the sound scripts
python3 tools/build_assets.py
python3 tools/make_sounds.py   # needs numpy + ffmpeg
```

The release build is signed with `android/snakeattack.keystore` so updates
install over each other. Use your own private key before publishing to
Google Play.

## Credits

- Font: [Lilita One](https://fonts.google.com/specimen/Lilita+One) by Juan Montoreano, SIL Open Font License (`art/fonts/OFL.txt`)
- Engine: [libGDX](https://libgdx.com) (Apache 2.0)

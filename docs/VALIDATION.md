# Validation report

Checks that were actually run, with their results. Environment: Linux container, OpenJDK 21,
Gradle 8.14.3, Xvfb + Mesa llvmpipe (software OpenGL) and no sound device. There was no Android
device or emulator, and `dl.google.com` (Android SDK manager / Google Maven) was blocked.

## Summary

| Check | Result |
|---|---|
| Kotlin compile: `core`, `lwjgl3`, Android launcher (offline path) | pass |
| Unit tests: `./gradlew :core:test` | **26 / 26 pass** |
| Asset references resolve; atlas pages decode; regions inside pages | pass (`AssetIntegrityTest`) |
| Animations use the pack's exact frame order and timing (all 210, compared with `assets.zip`) | pass |
| Pivot / trim offset / collision conversion (incl. mirroring) | pass (`pivotAndCollisionConversion`) |
| Every runner, world, enemy, boss, obstacle, collectible and power-up has art and a gameplay use | pass |
| Fair generation: bot plays 8 worlds × 4 seeds to 3,200 m with **zero hits**, 2 bosses each | pass |
| Each runner beats the first boss with the bot | pass |
| Pattern validator: no gap below the physical minimum; same seed gives the same level | pass |
| No hazard spawns inside the visible screen | pass |
| Pause freezes the simulation; resume causes no jump | pass |
| Boss uses both lanes, reaches phase 2, rewards once; run report built once | pass (all 8 worlds) |
| Saves persist across restarts; corrupt save → backup → defaults; validation clamps | pass |
| Purchases, upgrades, daily cycle (same day / gap / clock behind), missions and achievements reward once | pass |
| Mystery box table sums to 100%; sampled key rate 15% ± 1% | pass |
| Audio files exist, decode and measure as below | pass (`tools/validate_audio.py`) |
| Desktop smoke test: tour of every screen plus a bot-played run with boss, victory, pause and game over | pass (screenshots in `docs/screenshots`) |
| Layouts at 16:9, 20:9 (1600×720) and 4:3 (1024×768) | checked visually |
| Debug APK: `tools/build_apk_offline.sh` | built, signed (v1+v2+v3), `apksigner verify` pass |
| `./gradlew :android:assembleDebug` (AGP) | **not run**: Google Maven blocked here |
| No class referenced at runtime is missing (`tools/check_missing_classes.py`) | pass: only `androidx.fragment` (unused fragment embedding) and the backend's text-dialog `R` remain |
| No unguarded call newer than Android 5.0 (`tools/check_api_level.py` vs API 21) | pass for game code; LibGDX's newer calls are behind its own `SDK_INT` checks |
| Running on an Android device or emulator | **not done** |

## Commands and outputs

### Unit tests

```
./gradlew :core:test
AssetIntegrityTest  8 tests, 0 failures
FairnessTest        2 tests, 0 failures
ProgressionTest     8 tests, 0 failures
WorldTest           8 tests, 0 failures
```

Bot statistics from `FairnessTest` (excerpt): every run reached 3,200 m with 2 bosses defeated,
74–103 jumps and 36–211 slides, and no hit taken.

### APK

```
tools/build_apk_offline.sh
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): true
package: name='com.bramble.dash' versionCode='1' versionName='1.0.0'
sdkVersion:'21'  targetSdkVersion:'34'
launchable-activity: name='com.bramble.dash.android.AndroidLauncher'
native-code: 'arm64-v8a' 'armeabi-v7a' 'x86' 'x86_64'
```

`dist/bramble-dash-debug.apk` (about 22 MB) holds a single `classes.dex`, all 34 atlases and the
audio. OGG, WAV and PNG files are stored uncompressed, as Android's `MediaPlayer` requires for
streamed music.

### Texture memory (computed from the packed pages, RGBA8888, no mipmaps)

| Situation | Resident textures |
|---|---|
| Source pack, all pages | 608 MiB |
| Shared groups (UI, panels, items, obstacles, effects, previews) | 56.6 MiB |
| Lobby (shared + one runner + one world) | about 79 MiB |
| Run, jungle … city | 100–102 MiB |
| Run, volcano / fantasy (3 / 5 bosses preloaded) | 108 / 125 MiB |

When the runner or world changes, the atlases that are no longer needed are unloaded.

### Frame rate

The tour log reports 49–61 FPS in a 1280×720 Xvfb window with **software** rendering (llvmpipe).
This is not a device measurement. No Android profiling was possible.

### Audio (`python3 -I tools/validate_audio.py`)

| file | duration | peak | RMS | loop seam | result |
|---|---|---|---|---|---|
| sfx/boss_attack.wav | 0.50 s | 0.89 | 0.214 | 0.0000 | OK |
| sfx/boss_defeat.wav | 1.05 s | 0.89 | 0.197 | 0.0000 | OK |
| sfx/boss_warning.wav | 1.02 s | 0.89 | 0.356 | 0.0000 | OK |
| sfx/chest_open.wav | 0.70 s | 0.89 | 0.174 | 0.0000 | OK |
| sfx/coin.wav | 0.28 s | 0.89 | 0.502 | 0.0000 | OK |
| sfx/damage.wav | 0.25 s | 0.89 | 0.211 | 0.0000 | OK |
| sfx/death.wav | 1.08 s | 0.89 | 0.476 | 0.0000 | OK |
| sfx/double_jump.wav | 0.18 s | 0.89 | 0.265 | 0.0000 | OK |
| sfx/enemy_attack.wav | 0.22 s | 0.89 | 0.210 | 0.0000 | OK |
| sfx/enemy_defeat.wav | 0.30 s | 0.89 | 0.168 | 0.0000 | OK |
| sfx/energy_shot.wav | 0.20 s | 0.89 | 0.297 | 0.0000 | OK |
| sfx/explosion.wav | 0.80 s | 0.89 | 0.191 | 0.0000 | OK |
| sfx/gem.wav | 0.43 s | 0.89 | 0.182 | 0.0000 | OK |
| sfx/jump.wav | 0.16 s | 0.89 | 0.400 | 0.0000 | OK |
| sfx/land.wav | 0.12 s | 0.89 | 0.222 | 0.0000 | OK |
| sfx/mission_complete.wav | 0.65 s | 0.89 | 0.567 | 0.0000 | OK |
| sfx/powerup_activate.wav | 0.36 s | 0.89 | 0.365 | 0.0000 | OK |
| sfx/powerup_pickup.wav | 0.35 s | 0.89 | 0.187 | 0.0000 | OK |
| sfx/purchase_fail.wav | 0.38 s | 0.89 | 0.406 | 0.0000 | OK |
| sfx/purchase_ok.wav | 0.49 s | 0.89 | 0.235 | 0.0000 | OK |
| sfx/revive.wav | 0.75 s | 0.89 | 0.244 | 0.0000 | OK |
| sfx/shield_break.wav | 0.40 s | 0.89 | 0.117 | 0.0000 | OK |
| sfx/slide.wav | 0.30 s | 0.89 | 0.189 | 0.0000 | OK |
| sfx/spring.wav | 0.25 s | 0.89 | 0.237 | 0.0000 | OK |
| sfx/ui_back.wav | 0.09 s | 0.89 | 0.346 | 0.0000 | OK |
| sfx/ui_click.wav | 0.05 s | 0.89 | 0.209 | 0.0000 | OK |
| sfx/unlock.wav | 0.90 s | 0.89 | 0.305 | 0.0000 | OK |
| sfx/victory.wav | 2.10 s | 0.89 | 0.299 | 0.0000 | OK |
| music/amb_eerie.ogg | 11.00 s | 0.62 | 0.175 | 0.0135 | OK |
| music/amb_hum.ogg | 11.00 s | 0.61 | 0.174 | 0.0173 | OK |
| music/amb_nature.ogg | 11.00 s | 0.61 | 0.068 | 0.0211 | OK |
| music/amb_rumble.ogg | 11.00 s | 0.60 | 0.139 | 0.0245 | OK |
| music/amb_wind.ogg | 11.00 s | 0.60 | 0.103 | 0.1266 | OK |
| music/boss.ogg | 24.00 s | 0.95 | 0.235 | 0.0011 | OK |
| music/menu.ogg | 38.40 s | 0.81 | 0.180 | 0.0011 | OK |
| music/run.ogg | 27.43 s | 0.85 | 0.220 | 0.0191 | OK |

* Effects are 22.05 kHz mono 16-bit WAV, normalised to a 0.89 peak.
* Music and ambience are 32 kHz mono OGG Vorbis (q3). Loops are rendered on an exact beat grid
  with note tails wrapped to the start; ambience uses a 1 s crossfade.
* Playback, mixing and latency were not heard or measured: the container had no audio device,
  and LibGDX fell back to mock audio.

## Remaining limitations

* No on-device test. Touch feel, cutout insets, haptics, lifecycle (home button, calls) and
  real frame rate need checking on hardware. The code paths exist: `GameScreen.pause()`,
  `AudioService.suspend/resume`, safe insets and `renderUnderCutout`.
* The AGP Gradle build is configured but was not executed here.
* The art limitations listed in the README apply: cutout animations and simpler supporting art.

## Fixed: crash on launch (Android 11+)

The first APK crashed immediately on Android 11 and newer. On API 30+ LibGDX's
`AndroidApplication` creates `AndroidXKeyboardHeightProvider`, which needs `androidx.core`, and
the APK did not contain AndroidX, so the app died with `NoClassDefFoundError`.

* The AGP build now depends on `androidx.core:core:1.13.1`.
* The offline build adds a minimal, API-30-based replacement for the five members LibGDX uses
  (`android-offline/src/main/java/androidx/core`), because Google Maven is unreachable there.

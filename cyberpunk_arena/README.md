# Cyberpunk Arena (Godot 4.4 · Android)

Third-person 3D action game for Android phones. Your **cyberpunk warrior GLB**
is the player in a large clean white arena, with a smooth touch camera,
a virtual joystick, run and attack buttons, physics movement and melee combat.

- Engine: **Godot 4.4.1-stable**, GDScript, Compatibility (OpenGL ES 3) renderer
- Target: Android 5.0+ (API 21), arm64-v8a + armeabi-v7a, landscape only, immersive fullscreen

## Install the prebuilt APK

1. Copy `release/CyberpunkArena-debug.apk` (in the repository root) to the phone, or with USB debugging on:
   `adb install -r ../release/CyberpunkArena-debug.apk`
2. If asked, allow "Install unknown apps" for your file manager or browser.
3. Launch **Cyberpunk Arena**. It always opens in landscape and fullscreen.

## Controls

| Touch | Keyboard / gamepad (desktop testing) |
|---|---|
| Left side: floating joystick (move; push further to go faster) | WASD / arrows · left stick |
| **RUN** button: toggles sprint (switches off when you stop) | Shift (hold) · L1 / L3 |
| **ATTACK** button: tap to attack, hold to keep attacking | J / K / Enter · X / A |
| Drag anywhere else: rotate the camera | Q/E/R/F · right stick |
| | F3 shows or hides the FPS counter |

The joystick, both buttons and camera drag track each finger separately,
so they all work at the same time (multitouch).

## Your GLB: what it contains and how it is used

I inspected the file with `tools/inspect_glb.gd`. It contains:

| Item | Found | Handling |
|---|---|---|
| Geometry | 27 rigid mesh parts | Used as-is, auto-scaled to 1.8 m, feet placed exactly on the floor |
| Skeleton / skin | **None** | Not needed. The clips animate the parts themselves (node rotations) |
| Animations | `Idle` 2.4 s, `Walk` 1.2 s, `Run` 0.75 s, `Attack` 1.0 s | All four are used. Idle/Walk/Run are set to loop (they import without looping) |
| Materials / textures | **None**. Colours are per-vertex (`COLOR_0`) | Materials are built from the model's own vertex colours. Visor, reactor, light strips and blade glow in their own cyan; crest and shoulders get a soft violet glow |
| Facing | +Z | Rotated 180° so it faces Godot's forward (−Z) |

**Incompatibility found and fixed:** all 27 parts are flat siblings in the GLB.
Played as-is, the `Attack` clip rotates `R_upper_arm` while the forearm, glove
and blade stay behind in mid-air. The same happens to the legs and torso.
`WarriorModel` rebuilds the limb hierarchy at load time (keeping every world
transform) and rewrites the animation track paths, so the original clips move
whole limbs.

**Fallbacks** (for a different GLB):
- Clips are detected by name keywords. If Run is missing, Walk is played faster; if Walk is missing, Run is played slower.
- If a clip is missing or its tracks don't match the model, a procedural animation is used instead (limb swing and bob, or an arm swing for the attack).
- What was detected is printed at startup.

## Project layout

```
project.godot            settings, input map, landscape, physics, renderer
export_presets.cfg       Android preset (debug, immersive, arm64 + armv7)
build_android.sh         command-line APK build
assets/characters/       cyberpunk_warrior.glb (your model)
scenes/                  main, player, camera_rig, arena/, props/, ui/, characters/
scripts/core/            game_input.gd (autoload: merges touch + keyboard + gamepad)
scripts/player/          player.gd (CharacterBody3D), warrior_model.gd (animation, rig, materials)
scripts/camera/          camera_rig.gd (smooth orbit camera, touch drag, spring arm)
scripts/combat/          attack_hitbox.gd, hurtbox.gd, slash_effect.gd
scripts/ui/              virtual_joystick.gd, touch_button.gd, mobile_controls.gd
scripts/props/           training_dummy.gd (hit target and template for enemies)
shaders/                 arena_floor.gdshader (procedural grid and glowing rings)
materials/               shared arena materials
tests/                   automated gameplay tests (excluded from the APK)
tools/                   GLB inspector (excluded from the APK)
```

## Key systems

- **Movement:** camera-relative, with acceleration and deceleration, gravity, floor snapping, slopes up to 46°, and smooth turning toward the movement direction. Walk speed is 2.6 m/s and run is 6 m/s. Joystick tilt sets the speed.
- **Animation:** playback speed follows ground speed to reduce foot sliding, with crossfades between clips and a forward lean when running.
- **Camera:** smooth position and rotation, touch sensitivity that is the same at any screen resolution, and a pitch limit. A sphere-cast spring arm keeps it out of the floor, walls and pillars. It slowly swings back behind you when you run forward.
- **Combat:**
  - Attacks are queued if you press early, and a playing attack can't be cut off by movement. There is a short cooldown between attacks.
  - You turn to face the nearest training dummy when you attack.
  - The hitbox is only active during the strike. It uses its own collision layers (`player_attack` and `enemy_hurtbox`).
  - Hits give camera shake, a slash effect and vibration.
- **Future enemies:** add a `Hurtbox` (collision layer 4) to any enemy, put the enemy in the `enemies` group, and connect `hurt(damage, knockback, source)`. `TrainingDummy` is a working example.
- **Mobile performance:**
  - Compatibility renderer, with 2× MSAA.
  - One directional shadow (2 splits, 42 m), a small sky radiance map, and only primitive collision shapes.
  - No textures to stream: the floor and slash effect are procedural.
  - 60 Hz physics with physics interpolation, so motion is smooth on 90/120 Hz screens.
- **Screens:** the canvas stretches to fit any aspect ratio (16:9 to 21:9), the controls are anchored to the corners, and safe-area insets keep them clear of notches.

## Build from source

Requirements: Godot 4.4.1, its export templates, an Android SDK
(platform-tools and build-tools), and JDK 17+.

**In the editor:** open `project.godot`. In Editor Settings → Export → Android,
set the SDK and JDK paths. Then go to Project → Export → Android → Export Project.

**Command line:**
```bash
export GODOT=/path/to/Godot_v4.4.1-stable_linux.x86_64
export ANDROID_SDK=$HOME/Android/Sdk
./build_android.sh ../build/CyberpunkArena-debug.apk
# This is equivalent to:
#   godot --headless --path . --export-debug "Android" ../build/CyberpunkArena-debug.apk
```

## Tests

```bash
xvfb-run -s "-screen 0 1600x720x24" godot --path . --resolution 1600x720 \
    --fixed-fps 60 -s res://tests/test_runner.gd -- /tmp/shots
```
The tests run the real game with simulated multitouch: the joystick, run
button and camera drag held at the same time. 55 checks cover:
- landscape and immersive settings
- clip detection and rig retargeting
- grounding
- walk and run speeds and animations
- simultaneous fingers
- attack lock, cooldown, hitbox hits and the blade following the arm
- camera floor and wall collision
- climbing the ramp

Last run: **55 passed, 0 failed**.

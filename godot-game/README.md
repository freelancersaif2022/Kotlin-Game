# Bone Forest (Godot 4 + GDScript)

A 3D third-person action game for Android and desktop. You play the Skeleton Knight
and must survive 10 waves of undead skeletons in an ancient forest, including two
fights against the giant **Bone King** (waves 5 and 10).

## Features
- Third-person camera on a spring arm, plus touch-drag and mouse look
- Three-hit sword combo with soft lock-on and lunge, a **Slam** area attack, and jumping
- Four enemy types: Minion (axe), Rogue (fast blade), Mage (homing magic bolts) and the Bone King boss.
  Purple-tinted elite variants appear from wave 4
- Enemies find their way around trees and rocks using a navigation mesh baked at runtime
- Combo scoring, heal-orb drops and a saved best score
- Hit-stop, screen shake, damage numbers, sparks, dust, a shockwave ring and a hurt vignette
- Procedurally placed forest built from the KayKit pack, with grass, bushes and rocks drawn
  as MultiMeshes so it runs smoothly on phones
- Sound effects, music and forest ambience (generated, royalty-free)
- Mobile controls: floating joystick, camera drag, ATTACK / JUMP / SLAM buttons and an Android back button that pauses the game

## Controls
| Action | Touch | Keyboard / Mouse | Gamepad |
|---|---|---|---|
| Move | Left side joystick | WASD / Arrows | Left stick |
| Look | Drag on right side | Mouse | - |
| Attack | ATTACK | Left click / J | X |
| Slam | SLAM | Right click / E | Y |
| Jump | JUMP | Space | A |
| Pause | II button / Back | Esc / P | Start |

## Project layout
```
assets/characters  KayKit Skeletons (.glb)      assets/animations  Rig_Medium animation .glb files
assets/weapons     KayKit weapons (.gltf)        assets/forest      KayKit Forest Nature Pack (.gltf)
assets/audio       sound effects and music       scenes/main.tscn   entry scene
scripts/game.gd            game flow, waves, score, effects
scripts/world.gd           forest generation, lighting, navigation mesh
scripts/player.gd          Skeleton Knight controller
scripts/enemy.gd           skeleton AI (minion / rogue / mage / boss)
scripts/character_model.gd attaches the shared animations and weapons to any KayKit skeleton
scripts/camera_rig.gd      third-person camera     scripts/touch_controls.gd  mobile controls
scripts/hud.gd             HUD and menus           scripts/anim_lib.gd / sfx.gd  autoloads
tests/autoplay.gd          automated bot playtest that saves screenshots
```

## Running and exporting
1. Open this folder in **Godot 4.4+**. The project uses the Compatibility renderer, which supports the most phones.
2. Press F5 to play.
3. To build an APK, open Project > Export > Android. Install the Android export templates, set the
   Android SDK and your keystore in Editor Settings, then export.

## Credits
- 3D art: KayKit Skeletons and KayKit Forest Nature Pack by Kay Lousberg (www.kaylousberg.com), licensed CC0
- Code, sound and music: made for this project

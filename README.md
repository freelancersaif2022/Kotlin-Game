# PyMaster — Learn Python A to Z (Android, Kotlin)

A professional, dark-themed Android app for learning Python, written in **Kotlin + Jetpack Compose**.
It runs **real Python 3.11 on the phone** (via Chaquopy). No ads, no premium, no account: everything is stored on the device.

## Download the APK

Every push builds a signed APK with GitHub Actions:

1. Open the repository's **Releases** page (or **Actions → Build APK → latest run → Artifacts**).
2. Download `PyMaster-v1.0.0-buildN.apk` on your Android phone (Android 7.0+).
3. Allow "Install unknown apps" for your browser/file manager and install it.

New builds are signed with the same key (`app/pymaster.keystore`), so they install over older ones.

## Features

| Area | What's inside |
|---|---|
| **Splash & onboarding** | Animated splash, 4 onboarding pages |
| **Profile setup** | 5 programmer avatars, camera photo or gallery photo, name, level, goal, daily study goal. Saved locally |
| **Home** | Overall progress hero, "continue learning" card, daily quiz (3–4 new questions every day), stats (learning time, streak, lessons, XP), today's goal ring, learning path, notification bell with unread badge |
| **Course** | Donut progress chart, weekly study bar chart, module progress chart, running modules, 12 modules / 64 lessons with real code examples and outputs, a quiz per module (unlock rules apply) |
| **Terminal** | Real Python editor: syntax highlighting, line numbers, auto-indent, symbol keys, undo/redo, examples menu, run/stop, live console with `input()` support |
| **Projects** | 15 real-world projects (calculator → snake game), each with an overview, step-by-step build guide and full code. Locked until the course is complete, then unlocked in order |
| **Certificate** | Professional certificate with the learner's name, date, ID and the signature **"Saif A."**. Save to gallery, share as PNG or PDF |
| **Notifications** | Lesson, module, course, project and certificate notifications plus a daily quiz reminder (WorkManager). All saved in the in-app inbox |
| **Settings** | Edit profile, 4 dark themes, font size, terminal font size, vibration, notifications, daily reminder, auto-indent, daily goal, clear notifications, reset progress, about |

> Tip for testing: in **Settings → About**, tap the version 7 times to turn on developer mode, which unlocks everything (including a certificate preview).

## Project structure

```
app/src/main/java/com/saif/pymaster/
  data/        models, local storage (DataStore), progress rules, learning actions
  notify/      notifications + daily quiz worker
  python/      Python runner bridge (Chaquopy)
  cert/        certificate renderer (PNG/PDF export)
  ui/          navigation, view models, theme, components, screens
app/src/main/python/pm_runner.py   executes terminal code (stdout/stderr/input/stop)
app/src/main/assets/*.json         course, projects and daily-quiz content (generated)
content-src/                       content source; every example is executed to produce real output
```

### Editing the course content

Edit the files in `content-src/` and regenerate the JSON:

```bash
python3.11 content-src/build.py
```

### Building locally

Use Android Studio (Ladybug or newer) or run `./gradlew assembleRelease` with JDK 17 and the Android SDK (API 35).
`./gradlew testDebugUnitTest` renders screenshots of every main screen into `app/build/screenshots/`.

---
Designed & developed by **Saif A.**

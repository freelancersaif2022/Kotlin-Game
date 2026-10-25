# Twin Space

A professional dual-app ("Dual Space"-style) Android app built with **Kotlin + Jetpack Compose**.
Run a second copy of WhatsApp, Instagram, games or any other app, each with its own account and data.

**No ads, no in-app purchases, no tracking.**

## Features

- **Clone apps** – pick one or many apps and clone them into your private space.
- **Launch clones** with a tap; long-press for every action.
- **Rename** – give any clone your own name (shown in Twin Space and on its shortcut).
- **Home-screen shortcuts** – pin a clone to the launcher with a "2" badge and your custom name. Renaming updates pinned shortcuts.
- **Freeze / unfreeze** – stop a clone from running in the background without losing its data.
- **Remove clone** – deletes only that clone (Android asks you to confirm). The original app is never touched.
- **Pause / resume space** – put all clones to sleep at once.
- **App lock** – fingerprint, face or screen lock to open Twin Space.
- **Search, filters, multi-select**, light and dark themes, smooth animations.
- **Nothing is removed automatically.** Clones and the space stay until *you* remove them.

## How it works

Twin Space uses Android's built-in **work profile**, the same mechanism used by Shelter and Island, rather than a fragile
virtualization engine. That makes it fast, stable and battery-friendly:

- Cloning uses `DevicePolicyManager.installExistingPackage`, so a clone shares the original's APK (almost no extra
  storage) but has completely separate data. Clones update together with the original.
- The copy of Twin Space inside the space is the profile owner. The main app talks to it through the system's
  cross-profile forwarder (`SpaceGatewayActivity`). Every request is checked against the caller and a secret token that is
  shared when the space is created.
- Clones are launched with `LauncherApps`, so you get the normal app transition and Android's own work badge.

### Requirements and limits

- Android 9 (API 28) or newer.
- Android allows **one work profile per device**. If another app (for example a company MDM) already has one,
  Twin Space can't create its space.
- A few manufacturers disable work profiles. Twin Space detects this and tells you.

## Build

Open the project in Android Studio (Ladybug or newer) and run, or from the command line:

```bash
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease    # minified; signed with the debug key until you add your own
```

Every push also builds both APKs on GitHub Actions (see the **Build APK** workflow artifacts).

## Project layout

```
app/src/main/java/com/twinspace/app/
├── MainActivity.kt, MainViewModel.kt   UI host, state, requests into the space
├── ShortcutLauncherActivity.kt         Invisible trampoline for home-screen shortcuts
├── core/                               SpaceManager, IconCache, ShortcutHelper, protocol
├── admin/                              Runs inside the space: setup, gateway, admin receiver
├── data/                               Settings (names, frozen apps, app lock)
└── ui/                                 Compose theme, components and screens
```

# Ghost Detector 👻 (Prank / Entertainment app)

Ghost-hunting prank app for Android, built with **Kotlin + Jetpack Compose**.

## Features
- **Splash**: animated radar, boot log, flickering neon title, synthesized boot sound
- **Home**: full-screen animated haunted-lab UI, live EMF/TEMP/EVP tiles, big *FIND GHOST* radar button, native ad, banner ad
- **Scanner** (CameraX back camera, full-screen and immersive):
  - Calibration, then random entities appear as **green tracking frames** (no ghost image is ever drawn)
  - Frames lock on, move smoothly, flicker and vanish at random, and new ones appear (1–3 at once)
  - Each entity has a random **name (620 names)**, **approx. age**, type, threat level, mood, era, energy, distance
  - Tap a frame to open a detailed entity profile with **Listen EVP**
  - Warnings: *Paranormal entity detected*, *High threat*, cold spots, EMF spikes, *Signal lost*
  - HUD: EMF meter (also reacts to the phone's **real magnetometer**), temperature, frequency, mini radar, EVP oscilloscope, timer
  - Night-vision mode, torch, pause/resume, geiger clicks, heartbeat on high threat, vibration
- **Ghost Log**: history of every detection
- **Premium**: one-time Google Play purchase (`premium_remove_ads`) that removes all ads, plus restore purchase
- **Ads**: AdMob banner, native, and interstitial (frequency-capped, shown only when leaving a scan), plus UMP consent
- **Settings**: sound, vibration, default night vision, rate, share, privacy policy, disclaimer

All sounds are synthesized in-house (`app/src/main/res/raw`). Fonts: Orbitron, Share Tech Mono, Rajdhani (SIL OFL).

## Download the APK
Every push runs **GitHub Actions → Build APK**. Get the APK from:
- the **Releases** page (`build-N` pre-releases), or
- the run's **Artifacts** (`GhostDetector-apk`).

## Build locally
```bash
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease
```
Requires JDK 17 and the Android SDK (compileSdk 35).

## Before publishing to Google Play
1. Replace the AdMob **test** IDs in `res/values/strings.xml` (App ID) and `ads/AdIds.kt` (units).
2. Create the in-app product `premium_remove_ads` in Play Console.
3. Add a release keystore as GitHub secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
4. Set your privacy policy URL in `ui/settings/SettingsScreen.kt`.
5. Keep the "prank / entertainment only" disclaimer in the app and store listing (Google Play policy).

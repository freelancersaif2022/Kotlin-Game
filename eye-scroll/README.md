# EyeScroll

Scroll any Android app with your eyes. Look at the top of the screen to scroll up and the bottom to scroll down.

Built with Kotlin, Jetpack Compose, CameraX and MediaPipe Face Landmarker (478 face points with iris and blendshapes). Everything runs on the device.

## Features

- **Camera on demand.** The camera runs only while eye scrolling is on. You can turn it on or off with the floating dot, the "Eye scroll" quick-settings tile, the notification's *Turn off* button, or the app itself.
- **Auto-off** after 30 s, 1 m, 2 m or 5 m without scrolling (or never). Tracking also stops when the screen turns off or no face is seen for 20 s.
- **10-second calibration.** You look at dots at the top, middle and bottom, and a personal model is fitted to your face, phone position and lighting.
- **15–30 checks per second** (you choose), smoothed with a One Euro filter.
- **Scroll logic built to avoid accidents.** Blinks are ignored, a look-and-hold time is required before scrolling, hysteresis keeps the zones stable, opposite scrolls are blocked for a moment after each scroll, and slow drift is corrected.
- **Floating dot.** The eye inside follows your gaze, and a ring fills up before each scroll. Drag it to move it (it snaps to the screen edge); long-press it to open the app.
- **Smart mode** (eyes + a tiny head movement) or **Eyes only** mode.
- **Practice feed** and a **live test** screen with a gaze meter.

## How the gaze signal works

`tracking/GazeFeatures.kt` combines four signals from every frame:

1. the iris position relative to the eye corners
2. the gap between the eyelids (the lids drop when you look down)
3. MediaPipe's `eyeLookUp` / `eyeLookDown` blendshapes
4. head pitch

`tracking/GazeModel.kt` fits a standardised ridge regression from the calibration samples. `tracking/GazeController.kt` turns the result into scroll decisions.

## Build

GitHub Actions (`.github/workflows/eye-scroll-android.yml`) builds a release APK and an AAB on every push and attaches both to a GitHub release.

To build locally (Android Studio or the command line, JDK 17):

```
cd eye-scroll
./gradlew assembleRelease
```

## Publishing to Google Play

1. Create an upload key once and keep it safe:
   `keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000`
2. In the GitHub repo, go to *Settings → Secrets → Actions* and add these secrets:
   - `EYESCROLL_KEYSTORE_BASE64` (output of `base64 -w0 upload.jks`)
   - `EYESCROLL_KEYSTORE_PASSWORD`
   - `EYESCROLL_KEY_ALIAS`
   - `EYESCROLL_KEY_PASSWORD`

   Without these secrets, builds are signed with a temporary debug key. Those builds are fine for testing, but you have to uninstall the old build before installing a new one.
3. Upload the `.aab` from the release to Play Console.
4. In Play Console, fill in:
   - the **Accessibility API declaration**: the app uses gestures to scroll for users who can't or don't want to touch the screen, and it does not read screen content
   - the **foreground service (camera) declaration**, with a short demo video
   - a privacy policy stating that camera frames are processed on-device and never stored or shared
5. New personal developer accounts must run a closed test with at least 12 testers for 14 days before going to production.

## Installing the APK directly (Android 13+)

When you turn the accessibility service on, Android may say **"Restricted setting"**. Go to *App info → ⋮ → Allow restricted settings*, then switch EyeScroll on again. The setup screen in the app walks through this.

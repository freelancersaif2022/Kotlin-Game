# AirScroll

Scroll any Android app **without touching the screen**. The front camera watches your hand; moving your index finger up or down in the air scrolls the app you are using.

The app is built with Kotlin, Jetpack Compose, CameraX and the MediaPipe Hand Landmarker (21 points per hand). All processing happens on the device.

## How it works

- **Camera on demand.** The camera runs only while air scrolling is on. You can turn it on or off with the floating dot, the "Air scroll" quick-settings tile, the notification's *Turn off* button, or the app itself. It turns off automatically when idle (30 s to 5 min, or never), when the screen turns off, or when no hand is seen for 30 s.
- **Swipe mode (the default).** Flick your finger up or down by about one hand-length, and one smooth scroll happens. The return movement is ignored, so it never scrolls back by accident.
- **Point & hold mode.** The app learns where your finger rests. Hold the finger above that point and it keeps scrolling up; hold it below and it keeps scrolling down, until you bring it back.
- Thresholds are measured in hand sizes, so detection works whether your hand is 30 cm or 1.5 m from the phone.
- **Settings:** mode, direction (natural or reversed), sensitivity, scroll distance, hold repeat speed, auto-off time, camera speed (15/24/30 checks per second), the floating dot, and vibration.
- **Gesture test screen.** It shows a camera preview, the fingertip position and every gesture the app detects.

Code:
- `gesture/HandTracker.kt`: camera and hand model
- `gesture/GestureDetector.kt`: turns movement into gestures (unit tested)
- `service/GestureTrackingService.kt`: the camera foreground service
- `service/ScrollAccessibilityService.kt`: scroll gestures and the floating dot

## Build

GitHub Actions (`.github/workflows/eye-scroll-android.yml`) builds a release APK and an AAB on every push and attaches both to a GitHub release.

To build locally with JDK 17:

```
cd eye-scroll
./gradlew assembleRelease
```

## Publishing to Google Play

1. Create an upload key and keep it safe:
   `keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000`
2. In the GitHub repo, go to *Settings → Secrets → Actions* and add these secrets:
   - `EYESCROLL_KEYSTORE_BASE64` (output of `base64 -w0 upload.jks`)
   - `EYESCROLL_KEYSTORE_PASSWORD`
   - `EYESCROLL_KEY_ALIAS`
   - `EYESCROLL_KEY_PASSWORD`

   Without these secrets, builds are signed with the public test key in `signing/eyescroll-test.jks`. Each test build installs over the previous one, but never upload a build signed with that key to Play.
3. Upload the `.aab` from the release to Play Console.
4. In Play Console, fill in:
   - the **Accessibility API declaration**: the app performs scroll gestures for users who can't or don't want to touch the screen, and it does not read screen content
   - the **foreground service (camera) declaration**, with a short demo video
   - a privacy policy stating that camera frames are processed on-device and never stored or shared
5. New personal developer accounts must run a closed test with at least 12 testers for 14 days before going to production.

## Installing the APK directly (Android 13+)

When you turn the accessibility service on, Android may say **"Restricted setting"**. Go to *App info → ⋮ → Allow restricted settings*, then switch AirScroll on again. The setup screen in the app walks through this.

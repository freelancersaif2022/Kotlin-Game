# FingerScroll

Scroll any Android app with a tiny thumb movement. A small floating pad sits at the edge of the screen. Move your thumb slightly up or down on it and the app underneath scrolls for you.

The app is built with Kotlin and Jetpack Compose. It has no camera access, no internet access and collects no data.

## How it works

| On the pad | Result |
|---|---|
| Move slightly up/down, then release | One smooth scroll (an injected swipe gesture). A longer movement scrolls further. |
| Move, then keep holding | Keeps scrolling step by step (accessibility scroll actions). Pulling further makes it faster; letting go stops it. |
| Long-press without moving, then drag | Moves the pad. It snaps to the screen edge. |

- **Natural** direction (the default): finger up moves the page up, like swiping the screen. **Joystick** direction is the reverse.
- Settings: sensitivity, scroll distance, scroll speed, hold-to-scroll speed, pad size, idle opacity and haptics.
- There is a quick-settings tile called "Scroll pad" to show or hide the pad, and an in-app practice feed.

The app has to use two different scrolling methods because Android cancels injected gestures while a real finger touches the screen. While you hold the pad, the app scrolls the main scrollable view with accessibility scroll actions instead. After you release, it injects a smooth swipe that ends with a short hold, so the list stops exactly where the swipe ends instead of flinging.

Code:
- `service/FloatingPad.kt`: the pad and its touch handling
- `service/ScrollAccessibilityService.kt`: the scrolling
- `scroll/ScrollMath.kt`: the movement-to-scroll math (unit tested)

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
   - the **Accessibility API declaration**: the app performs scroll gestures and scroll actions for users who want one-handed or low-effort scrolling, and it does not collect or share screen content
   - a privacy policy stating that no data is collected
5. New personal developer accounts must run a closed test with at least 12 testers for 14 days before going to production.

## Installing the APK directly (Android 13+)

When you turn the accessibility service on, Android may say **"Restricted setting"**. Go to *App info → ⋮ → Allow restricted settings*, then switch FingerScroll on again. The setup screen in the app walks through this.

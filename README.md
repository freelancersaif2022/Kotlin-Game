# VoxCast – Wireless Mic

Turn one Android phone into a wireless microphone and another into a speaker box.
Built with **Kotlin + Jetpack Compose + Material 3**. No backend, no API, no account:
audio travels directly between the two phones over the local network.

## How it works
1. On the speaker phone tap **Speaker** and choose *Same Wi-Fi / Hotspot* or *Wi-Fi Direct*.
   A unique QR code (fresh secret token every session) is shown.
2. On the microphone phone tap **Microphone** and scan the code.
3. Talk – your voice plays on the speaker phone in real time (~40–80 ms).

Both phones must be on the **same Wi-Fi**, **one phone's hotspot**, or **Wi-Fi Direct**
(the speaker creates the group; Android 10+ microphones join it automatically from the QR code).

## Audio
- 16 / 24 / 48 kHz 16-bit PCM over UDP, 10 ms frames, adaptive 20–80 ms jitter buffer
- TCP control channel with token handshake, heartbeat, round-trip latency and auto-reconnect
- Noise chain: hardware NoiseSuppressor + echo canceller, 85 Hz rumble filter, hiss filter,
  voice clarity EQ, adaptive noise gate (*Ultra Clean* = full silence between words), soft limiter
- Foreground service with Wi-Fi low-latency lock, keeps working with the screen off

## Monetisation
- Google AdMob banner, interstitial (only between sessions, frequency-capped) and native ads
  – uses Google's **test ad unit ids**, replace them in `ads/AdsManager.kt` and the manifest
- **Premium $1.99 / month** removes all ads and unlocks Studio 48 kHz + 250% speaker boost.
  Billing is **mocked** in `data/PremiumManager.kt`; swap in Google Play Billing before release.

## Build
GitHub Actions builds debug and release APKs on every push (see the *Actions* tab → artifacts).
Locally: `./gradlew assembleDebug` (JDK 17, Android SDK 35).

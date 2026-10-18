# WiFi Scope

A Flutter app that shows live Wi-Fi signal, tests real internet speed, and
builds a coverage heatmap of your home from real measurements.

## Features

- **Signal**: live RSSI gauge, quality %, a 60-second signal chart, link speed,
  band, channel, Wi-Fi standard, IP and BSSID, and an estimated distance to the
  router (indoor log-distance path-loss model).
- **Speed test**: ping, jitter, download and upload with 4 parallel streams
  against Cloudflare's public speed test servers, plus result history.
- **Heatmap survey**: set the room size (and optionally a floor plan photo),
  mark the router, then walk around and tap where you stand. Each point
  averages 6 readings. The map uses inverse-distance weighting and fades out
  areas that were not measured, so it never invents coverage.
- **Placement insights** (rewarded ad): coverage score, best and weakest
  spots, dead zones, and suggested router and extender positions drawn on the
  map.
- **Networks**: nearby networks, a channel congestion chart, and a best-channel
  advisor (rewarded ad, unlocked for 24 hours).

## Ads

`lib/services/ad_service.dart` handles everything:

- Google UMP consent (GDPR / US states) before any ad request, with a privacy
  options entry in Settings when required.
- **Interstitial**: shown only at natural breaks (after a finished speed test,
  when leaving a survey). Never on the first break of a session, and at most
  once every 90 seconds.
- **Rewarded**: unlocks placement insights and the channel advisor. If no ad
  can be loaded, the reward is granted anyway so users are never blocked.
- Ads are preloaded and reloaded after each show, with retry and backoff.

## Before publishing

1. Replace the **test** AdMob IDs with your own:
   - `lib/services/ad_service.dart` (`AdIds`)
   - `android/app/src/main/AndroidManifest.xml` (`APPLICATION_ID`)
   - `ios/Runner/Info.plist` (`GADApplicationIdentifier`)
2. Change `applicationId` in `android/app/build.gradle.kts` if you want a
   different package name, and set up release signing.
3. In Play Console, declare location permission use ("read Wi-Fi network
   information") and fill in the Data Safety form: no location is collected,
   and the advertising ID is used by AdMob.
4. iOS: after the first `pod install`, add `PERMISSION_LOCATION=1` to the
   `permission_handler` preprocessor definitions in `ios/Podfile`. Note that
   iOS does not let apps read Wi-Fi signal strength, so only the speed test
   works there. The app is built for Android first.

## Run

```sh
flutter pub get
flutter run          # on a real Android phone; emulators have no real Wi-Fi
flutter test
```

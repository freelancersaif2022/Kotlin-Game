# Universal Remote — Smart TV Remote (Kotlin)

A complete, Play-Store-ready Android app (Kotlin + Jetpack Compose) that turns a phone into a remote
for most smart TVs from 2016 onward, over Wi-Fi.

## Supported TVs and how pairing works

| TV system | Brands | Pairing | Power on from off |
|---|---|---|---|
| Android TV / Google TV | Sony, TCL, Hisense, Philips, Xiaomi, Sharp, Chromecast, Shield | 6-character code on TV (Remote v2 protocol, TLS 6466/6467) | Wake-on-LAN (MAC entered once) or power key in network standby |
| Samsung Tizen (2016+) | Samsung | "Allow" popup on TV (WebSocket 8001 / 8002 + token) | Wake-on-LAN, MAC saved automatically |
| LG webOS | LG | "Accept" popup on TV (SSAP WebSocket 3000 / 3001) | Wake-on-LAN, MAC saved automatically |
| Roku | Roku players, Roku TVs (TCL, Hisense, Onn…) | None (ECP HTTP 8060) | `PowerOn` command (Fast TV start) + Wake-on-LAN |
| Vizio SmartCast | Vizio | 4-digit PIN on TV (HTTPS 7345 / 9000) | Power-on command (Quick Start mode) |
| Amazon Fire TV | Fire TV Stick / Cube / Fire TV Edition TVs | Enable ADB debugging once, then "Allow" popup (ADB 5555) | Wake from sleep while connected |

TVs are found automatically via mDNS (Android TV, Fire TV) and SSDP (Samsung, LG, Roku, Vizio).
TVs can also be added by brand + IP address.

## Monetisation

* **Interstitial ads**: only at natural breaks (after pairing, when switching TVs, when leaving Settings), at most one every 3 minutes, never while the remote is being used. No banner ads.
* **Rewarded ads**: watch 3 → Premium free for 24 hours.
* **Premium** (Google Play Billing): `premium_monthly`, `premium_yearly` (subscriptions) and `premium_lifetime` (one-time).
  Premium removes ads and unlocks keyboard, touchpad, app shortcuts, power on and unlimited TVs.

## Build

The APK is built by GitHub Actions (`.github/workflows/tvremote-apk.yml`) on every push that touches
`TVRemote/`. Download it from the workflow run's artifacts or from the repository's Releases page.

Locally: open `TVRemote/` in Android Studio, or run `./gradlew assembleRelease`.

### Before publishing on Google Play

0. **Billing test mode**: `billing.mock=true` in `gradle.properties` simulates purchases so Premium can be tested.
   **Set `billing.mock=false` before publishing**, otherwise purchases are free.

1. **AdMob**: create an app and ad units at https://admob.google.com and put the ids in
   `gradle.properties` (or the GitHub secrets `ADMOB_APP_ID`, `ADMOB_INTERSTITIAL_ID`, `ADMOB_REWARDED_ID`).
   The defaults are Google's test ids.
2. **Signing key**: create one keystore and keep it safe forever:
   `keytool -genkeypair -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias tvremote`
   Then add GitHub secrets `KEYSTORE_BASE64` (`base64 -w0 release.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
   Without these, CI signs with a temporary debug key (fine for testing, not for Play).
3. **Play Console products**: subscriptions `premium_monthly` (base plan `monthly`), `premium_yearly`
   (base plan `yearly`, optionally a 3-day free-trial offer) and in-app product `premium_lifetime`.
4. **Privacy policy**: host `PRIVACY_POLICY.md` publicly and update `AppConfig.PRIVACY_POLICY_URL`.
5. Play Store uploads need an **.aab**: run `./gradlew bundleRelease`.

# WiFi Analyzer – Speed Test

Android app (Kotlin + Jetpack Compose) with a speed test, a device scanner ("Who's on my Wi-Fi"),
a signal meter, a channel analyzer, a signal heatmap, intruder alerts, data usage, router admin and home screen widgets.

## Money
| Item | Setting |
|---|---|
| Monthly subscription | `premium_monthly` – $1.99 |
| Yearly subscription | `premium_yearly` – $14.99 (add a 3-day free trial offer if you want) |
| Free Premium | Watch 3 rewarded ads → 12 hours of Premium |
| Interstitial | After a speed test, after a device scan, when leaving History / Channel analyzer. Max one per 75 s, skipped on the first test and the first scan |
| Banner | Bottom of the main tabs and tool screens (not on the speed test screen) |
| App open | Only after the app was in the background for 4+ hours |

Premium users never see ads.

## Before you publish (checklist)
1. **AdMob IDs** – put your real IDs in `gradle.properties` (`admob.appId`, `admob.bannerId`, `admob.interstitialId`, `admob.rewardedId`, `admob.appOpenId`). Debug builds always use Google's test ads.
2. **Subscriptions** – in Play Console > Monetize > Subscriptions create `premium_monthly` and `premium_yearly` with an active base plan each.
3. **Links** – change `PRIVACY_POLICY_URL`, `TERMS_URL` and `SUPPORT_EMAIL` in `app/src/main/java/com/netscope/wifianalyzer/AppConfig.kt`. You can host `PRIVACY_POLICY.md` for free with GitHub Pages.
4. **Package name** – `com.netscope.wifianalyzer`. To change it, edit `applicationId` in `app/build.gradle.kts` before your first upload, because it can't change later.
5. **Signing** – add these repository secrets (Settings > Secrets and variables > Actions) so CI signs the release with your upload key:
   `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`.
   Without them the release build is signed with a debug key, which is fine for testing but **can't be uploaded to Play**.
6. **Play Console forms** – Data safety: the app shares data with AdMob (device or other IDs) and collects no personal data itself. Ads: yes. Target audience: 13+. The *Usage access* permission is used for the Data usage screen.

## Build
CI (`.github/workflows/wifi-analyzer.yml`) builds on every push. Download the files from the run's **Artifacts**:
- `app-debug.apk` – install on a phone to test (test ads)
- `app-release.apk` – release build
- `app-release.aab` – upload this one to Google Play

Locally: `./gradlew assembleDebug` (needs Android SDK 36 and JDK 17).

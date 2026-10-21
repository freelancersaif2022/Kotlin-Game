# Nova Cleaner

A storage cleaner and phone manager for Android, built with Kotlin and Jetpack Compose.

## Features

| Area | What it does |
|---|---|
| **Splash → Onboarding → Permissions** | Animated brand splash, 4-page onboarding, a permission screen with clear reasons (All files access, Usage access, Notifications) |
| **Home + guided tour** | Storage ring, RAM, storage breakdown by category, tool grid, battery and last-cleanup insights. On first launch a spotlight tour walks through the main parts of the screen |
| **Junk Clean** | Radar scan of shared storage for cache folders, thumbnails, temp files, logs, empty folders and leftover APKs. Choose by category or item, see a cleaning animation, then a result screen |
| **Large Files** | Size filters (10 MB to 1 GB+), type filters, sorting, real thumbnails, select several and delete |
| **Duplicate Photos** | Exact duplicates (content hash) and similar shots (perceptual hash). The best copy is kept automatically and "Smart select" is available per group |
| **Screenshots** | Grouped by month, select per month or all, delete in bulk |
| **APK Cleaner** | Finds APKs, shows installed / outdated / not installed status, pre-selects packages already installed |
| **App Manager** | App, data and cache size, install date, update date, last used, plus an "unused 30+ days" filter. Search, sort, a details sheet, and uninstalling several apps one after another |
| **Data Usage** | Mobile and Wi-Fi totals for today, 7 days, this month or 30 days. Monthly mobile limit, 7-day chart, per-app usage |
| **Network Speed** | Live speedometer, 60-second chart, session and since-boot totals, and an optional speed meter in the status bar (foreground service) |
| **Battery Info** | Level, health, temperature, voltage, current, estimated capacity, time to full |
| **Settings** | Theme (system / light / dark), reminders, speed meter, permission shortcuts, ad privacy choices, rate, share, support, restore purchase |

## Monetisation

* **AdMob**, all ads managed in `ads/`:
  * **App Open**: shown on cold start from the splash (never on the very first launch), and when the user returns after 15+ seconds in the background (at most once every 3 minutes). Returns from screens the app opened itself are skipped.
  * **Interstitial**: always preloaded. Shown after every clean / delete / uninstall (before the result screen), on every 2nd tool open and on every 2nd tool exit, with a 30-second gap between full-screen ads.
  * **Adaptive banner**: at the bottom of home, every tool, the result screen and settings.
  * **Native**: on the result screen.
  * All pacing values are in `ads/AdConfig.kt`.
  * Google **UMP** consent runs on the splash before any ad request.
* **Premium = lifetime "Remove Ads"** (`remove_ads_lifetime`, one-time in-app product). Every feature is free. Buying removes every ad type immediately, and the purchase is restored automatically and from Settings.
* **Random paywall**: the Remove Ads screen can appear by itself on launch (40%) or after a result (30%). It shows at most once per session and at most once every 6 hours, never before the tour is done, and never for premium users.

## Before publishing

1. **AdMob IDs**: the project uses Google's *test* IDs. Put your real IDs in `gradle.properties` (or pass them with `-P`):
   ```properties
   ADMOB_APP_ID=ca-app-pub-xxxxxxxx~yyyyyyyy
   AD_BANNER_ID=ca-app-pub-xxxxxxxx/1111111111
   AD_INTERSTITIAL_ID=ca-app-pub-xxxxxxxx/2222222222
   AD_APP_OPEN_ID=ca-app-pub-xxxxxxxx/3333333333
   AD_NATIVE_ID=ca-app-pub-xxxxxxxx/4444444444
   ```
2. **Play Billing**: create a one-time in-app product with id `remove_ads_lifetime` in Play Console.
3. **Signing**: `keystore/novacleaner-release.jks` is a *development* key so CI can produce an installable release APK. For Play, create your own upload key and provide `SIGNING_STORE_FILE`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD` as environment variables or CI secrets.
4. **Policy declarations** in Play Console: *All files access* (`MANAGE_EXTERNAL_STORAGE`, used for file management and cleaning), *QUERY_ALL_PACKAGES* (app manager), *Usage access*, and the *special-use foreground service* (status-bar speed meter).
5. Update `privacy_policy_url` and `support_email` in `res/values/strings.xml`.

## Build

```bash
cd CleanerApp
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
./gradlew bundleRelease     # app/build/outputs/bundle/release/app-release.aab
```

GitHub Actions (`.github/workflows/nova-cleaner.yml`) builds the signed release APK and AAB on every push, uploads them as artifacts and attaches them to a GitHub pre-release.

## Tech

Kotlin 2.0 · Compose (Material 3) · Navigation Compose · ViewModel + StateFlow · DataStore · WorkManager · Coil · Google Mobile Ads + UMP · Play Billing 7. R8 minification and resource shrinking keep the release APK small. Font: Plus Jakarta Sans (OFL).

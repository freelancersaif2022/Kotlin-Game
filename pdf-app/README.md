# PDF Master

An all-in-one PDF toolkit for Android, built with Kotlin and Jetpack Compose.
White-and-red brand UI, animated throughout, with AdMob ads, Google Play Billing and a
5-hour Premium pass unlocked by rewarded ads.

## Features

| Area | What's included |
|---|---|
| Reader | Smooth continuous scroll, pinch / double-tap zoom, fast scrollbar, page thumbnails, bookmarks, search in document, go to page, night reading, resume where you left off, print, share, open PDFs from other apps |
| Create & convert | Image → PDF (page size, orientation, margins), Scan document (ML Kit scanner with edge detection), Text → PDF, PDF → JPG/PNG, Extract text, OCR (Pro) |
| Organize | Merge, Split (extract / ranges / every N pages), Organize pages (reorder, rotate, delete) |
| Edit & sign | Pen, highlighter, text boxes, sticky notes, eraser, undo/redo, reusable drawn signatures with drag + resize, page numbers, watermark (Pro) |
| Optimize | Compress (Low / Recommended / Extreme-Pro) with before/after comparison |
| Security | Lock with AES-256 password (Pro), Unlock (Pro), open password-protected files |
| Files | Device scan (all-files access), Recent, Favourites, Created, sort, search, rename, delete |
| Monetisation | Adaptive banner, native ads (list + result screens), interstitial after tasks / leaving the reader (60 s cap), rewarded ad → 5-hour Premium pass with live countdown, monthly / yearly (trial) / lifetime purchases, restore, UMP consent |

Free users get 10 standard tasks per day. Premium (or an active pass) removes ads, limits and unlocks Pro tools.

## Project layout

```
app/src/main/java/com/pdfmaster/app
├── ads/          AdsManager (consent, interstitial, rewarded, native), BannerAd, NativeAdCard
├── billing/      BillingManager (Play Billing 7)
├── premium/      PremiumManager (purchase + 5 h reward pass + daily limits)
├── pdf/          PdfTools (PdfBox), PdfDocumentSession (fast renderer), OCR, thumbnails
├── data/         DataStore prefs, Room (recent/favourites/bookmarks), storage
└── ui/           Compose screens: splash, onboarding, home, files, tools, viewer,
                  editor, tool screens, result, premium, settings, search
```

## Build

Open the `pdf-app` folder in Android Studio (Ladybug or newer) and run, or:

```
./gradlew assembleRelease
```

The release APK is signed with the debug key unless you add a `keystore.properties`
(see below), so it installs directly for testing. GitHub Actions builds it on every push
(`.github/workflows/pdf-app.yml`) and attaches the APK to the run.

## Before publishing

1. **AdMob** – replace the Google test IDs in `app/build.gradle.kts`
   (`admobAppId` and the five `AD_*_ID` fields) with your own.
2. **Play Billing** – create these products in Play Console:
   - subscriptions `pdfmaster_premium_monthly`, `pdfmaster_premium_yearly` (add a 3-day free-trial offer)
   - in-app product `pdfmaster_premium_lifetime`
3. **Package name** – change `applicationId` if `com.pdfmaster.app` is taken.
4. **Links** – set `PRIVACY_URL`, `TERMS_URL` and `SUPPORT_EMAIL` in `ui/settings/SettingsScreen.kt`.
5. **All-files access** – the Device tab uses `MANAGE_EXTERNAL_STORAGE`. Google Play only allows it
   for apps whose core purpose is managing documents; fill in the permission declaration form,
   or remove the permission (users can still open files through the system picker).
6. **Signing** – create `pdf-app/keystore.properties`:
   ```
   storeFile=release.jks
   storePassword=...
   keyAlias=...
   keyPassword=...
   ```

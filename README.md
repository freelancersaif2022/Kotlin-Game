# PDF Studio

A fast, minimal Android app (Kotlin + Jetpack Compose) to **preview, split, crop and convert PDFs**.

## Features

- **Splash screen**: animated logo, then the main screen.
- **Upload & preview**: pick a PDF and see every page rendered sharply. Scroll, pinch or double-tap to zoom, and drag with one finger to move around while zoomed.
- **Split**: type the pages you want (`1-3, 5, 8`) and press **Split**. The result opens in the preview right away.
- **Crop**: drag the corners, the edges, or the whole frame to crop the page freely, then press **Crop**.
- **Convert**: exports every page as a high-quality **JPG** (selected by default) or **PNG**, with an animated progress dialog.
- **Dark / light mode** toggle in the top bar, remembered between launches.

Split and crop copy the original PDF objects, so text, vectors and images keep their full quality.

## Where files are saved

Converted images go to `Pictures/PDF Studio/`, so they show up in the Gallery. Split and crop only change the document open in the app; they don't write any files.

## Build

Every push runs **GitHub Actions → Build APK**. The workflow uploads `PDF-Studio.apk` as an artifact and publishes it to **Releases**.

To build locally (JDK 17 + Android SDK 35):

```bash
./gradlew assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

The release APK is minified with R8 and signed with the debug key, so you can install it directly. Use your own keystore before publishing to the Play Store.

Min Android 8.0 (API 26) · Target API 35

# PDF Studio

A fast, minimal Android app (Kotlin + Jetpack Compose) to **preview, split, crop and convert PDFs**.

## Features

- **Splash screen**: animated logo, then the main screen.
- **Upload & preview**: pick a PDF and see every page rendered sharply. Scroll, pinch to zoom, or double-tap to zoom.
- **Split**: a bottom sheet shows every page with its number. Tap pages or type them (`1-3, 5, 8`), then press **Split**. The new PDF opens in the preview right away.
- **Crop**: drag the corners, the edges, or the whole frame to crop the page freely, then press **Crop**.
- **Convert**: exports every page as **JPG** (selected by default) or **PNG** at 300 DPI.
- **Dark / light mode** toggle in the top bar, remembered between launches.

Split and crop copy the original PDF objects, so text, vectors and images keep their full quality.

## Where files are saved

| Output | Location |
| --- | --- |
| Split / cropped PDF | `Download/PDF Studio/` |
| Converted images | `Pictures/PDF Studio/` (Gallery) **and** `Download/PDF Studio/` |

## Build

Every push runs **GitHub Actions → Build APK**. The workflow uploads `PDF-Studio.apk` as an artifact and publishes it to **Releases**.

To build locally (JDK 17 + Android SDK 35):

```bash
./gradlew assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

The release APK is minified with R8 and signed with the debug key, so you can install it directly. Use your own keystore before publishing to the Play Store.

Min Android 8.0 (API 26) · Target API 35

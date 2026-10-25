# Image to PDF (Kotlin + Jetpack Compose)

Turns a photo of a document into a **real PDF**, not a picture pasted onto a page. Text is
re-typed as selectable, searchable PDF text, in the closest matching font, size, weight, slant and
colour. Background colours, boxes, bands and lines are redrawn as vector shapes.
Everything runs on the phone: no backend, no API, no internet.

## Flow
Splash → Upload image → Processing (progress) → Preview (PDF / Original / Fonts tabs, pinch-zoom) → Download or Share.

## How the conversion works
| Step | File | What it does |
|---|---|---|
| Load | `engine/ImageLoader.kt` | Decodes the image upright (EXIF) and resizes it for OCR |
| OCR | `engine/Converter.kt` | Google ML Kit text recognition, model bundled in the APK |
| Ink | `engine/InkAnalyzer.kt` | Separates letters from background: text colour, background colour, stroke mask |
| Fonts | `engine/TextAnalyzer.kt` | **Family**: serif (foot width of n/m/h/i/r/l…), monospace (even letter advance), sans. **Weight**: stem thickness ÷ font size → Light/Regular/Medium/Bold/Black. **Italic**: the shear that makes strokes upright. **Size**: fitted so the rendered glyphs match the photo's ink height exactly |
| Design | `engine/BackgroundBuilder.kt` | Page colour, coloured regions and thin rule/table lines as vector rectangles |
| PDF | `engine/PdfBuilder.kt` | Android `PdfDocument`; every word placed and stretched to its original width |

## Build
Open the `ImageToPdf` folder in Android Studio, or run:

```
./gradlew assembleRelease
```

You get one APK per CPU type (`app/build/outputs/apk/release/`), roughly 5–7 MB each.
GitHub Actions (`.github/workflows/build-apk.yml`) builds them on every push and attaches them to a release.

## Limits
- The output looks very close, not pixel-identical. Fonts are matched to the families on the phone
  (Roboto, Noto Serif, Droid Sans Mono). The original font file can't be recovered from a photo.
- OCR supports Latin-script languages (English, Spanish, French…).
- Photos, logos and signatures are rebuilt as blocks of colour, so they come out coarse.
- Clear, flat, well-lit photos give the best results.

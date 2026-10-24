PULSE — PRODUCTION ANDROID ASSET PACK
266 images across seven production batches. Original Pulse speech-bubble / pulse-tail identity.

PALETTE
Primary #0E9F8A; dark primary #0A7A6A; accent #34D1B6; sparing amber #FFB547.
Ink #111B21; muted #667781; light #FFFFFF / #F0F2F5;
dark #0B141A / #111B21 / #202C33.
Doodle tiles use the exact requested beige/gray and dark tile colors.
Photo colors and natural skin tones are intentionally unrestricted.
Call gradients include the requested indigo/plum tones.

FONTS
Use Android system Roboto or bundle Inter for Compose typography.
Wordmark is rasterized DejaVu Sans. No font files are included.

ANDROID INTEGRATION
Copy image files individually to drawable-nodpi or load them from assets/.
Number-prefixed organizational folder names are not Android res directory names.
All image basenames follow Android resource rules.
432px adaptive layers represent 108dp at xxxhdpi. Reference foreground and
background in adaptive icon XML; use monochrome on Android 13+.
The painted foreground is entirely inside the central 288x288 safe zone.
Splash mark fits inside a 192px-diameter circle on the 288px canvas.
Legacy icon has rounded transparent corners; Play Store icon is square RGBA.
The QR frame leaves an exactly 600x600 transparent central area (x/y 240..839).
Render the real QR code in Compose. No generated QR payload is included.
Doodle tiles repeat at exactly 1024px; light/dark versions share one geometry.
Story subjects leave calm overlay areas at the top and bottom.

FORMATS AND OPTIMIZATION
Photos use WebP with the per-category requested quality settings.
Transparent illustration art and stickers are encoded as lossless WebP.
For the app-size budget, illustration pixels use an optimized 64-color RGB
palette and 32-level alpha; sticker palettes adapt as needed to stay <=100000
bytes. No brand/icon dimensions or precision were reduced.
This pre-encoding color/alpha optimization preserves final pixel dimensions;
lossless refers to the WebP encoding of the optimized pixels.
All deliverables have EXIF, XMP and ICC metadata removed.
Sticker sheets are not included: each emotion is its own transparent file,
with an 8px white outline, subtle shadow and at least 16px transparent margin.
Tray images are derived from each pack's hello sticker at 96x96.

RESOLVED SPEC CONFLICT
40 avatar_person files with 36..40 reserved for casual non-face avatars leave
35 portrait slots, so 20 women AND 20 men cannot fit. The pack includes 18
women and 17 men, diverse fictional people ages 18–70, then the requested
pet, landscape, flower, car and beach avatars at indices 36..40.

SCOPE
Includes all requested assets. No small UI icon set, emoji, sound files,
real brands, real-map marks or watermarks are included.
Photo people are synthetic fictional characters, not identity references.
The README.txt casing is retained as requested; it is documentation, not a
resource image. manifest.json lists every packaged file including itself,
with null width/height for non-image files. Paths are relative to pulse_assets/.

CREATION
Built-in image generation created each standalone illustration, photograph,
background, logo-emblem and sticker. Python/Pillow resized, fitted, encoded,
stripped metadata, made sticker outlines, assembled previews and ZIPs.
Original brand geometry and seamless doodles were rasterized from deterministic
native vector geometry. No third-party imagery was incorporated.
Generation prompts follow the asset descriptions in manifest.json, with the
shared no-text/no-brand constraints and the palette/style brief above.

BUDGET ADJUSTMENT: photographic assets were re-encoded at quality 70 where originally above 70.

DIMENSION EXCEPTION: only media_photo_01..30 use a 1024px longest edge instead of 1280px, under the explicit total-size budget rule. All other 236 images retain their exact requested dimensions. Final sizes are listed in the manifest.

# Vaultly — Receipts, Returns & Warranties

Scan a receipt and Vaultly reads the store, date, item and total on the device, works out the
return window and warranty, and reminds you before either one ends. Export a polished PDF
"claim pack" for a return desk or warranty claim.

Kotlin · Jetpack Compose · Room · WorkManager · ML Kit (text recognition + document scanner,
via Google Play services) · AdMob · Google Play Billing.

## Features
- **Scan / import / manual entry** – ML Kit document scanner (auto-crop, enhance) or photo picker.
- **On-device OCR** – store, date, line items, total, currency, category and store return policy.
- **Deadlines** – return window + warranty with countdown rings, urgency colours and sorting.
- **Reminders** – return: 7, 3, 1 days before and last day; warranty: 30, 7 days before and last day.
- **Claim pack PDF** – summary page + receipt + proof photos, shared via any app.
- **Search & filters** – All, Returns, Warranty, Expired, Archived.
- **Light & dark themes**, Manrope typeface, adaptive + themed launcher icon.

## Monetisation
| Placement | Behaviour |
|---|---|
| Interstitial | After saving a new item; never on the first 2 saves; at most once every 3 minutes |
| Rewarded | Export one claim pack free, or unlock +5 vault slots |
| Free limit | 15 active items (+5 per rewarded video) |
| `vaultly_pro` subscription | Base plans `monthly` and `yearly` – unlimited items, no ads, unlimited PDFs |
| `vaultly_pro_lifetime` (in-app) | Pro forever |
| `vaultly_remove_ads` (in-app) | Ads off only |

Create those product IDs in Play Console → Monetize before release.

### AdMob
The build uses Google's **test** ad IDs unless these repository secrets are set:
`VAULTLY_ADMOB_APP_ID`, `VAULTLY_ADMOB_INTERSTITIAL_ID`, `VAULTLY_ADMOB_REWARDED_ID`.

### Signing
Without secrets the release is signed with the public test key in `signing/` (fine for sideloading,
never for Play). For Play, add `VAULTLY_KEYSTORE_BASE64`, `VAULTLY_KEYSTORE_PASSWORD`,
`VAULTLY_KEY_ALIAS`, `VAULTLY_KEY_PASSWORD`.

## Build
GitHub Actions (`.github/workflows/vaultly-android.yml`) runs unit tests, builds the APK and AAB,
and publishes them as a GitHub release. Locally: `./gradlew assembleRelease`.

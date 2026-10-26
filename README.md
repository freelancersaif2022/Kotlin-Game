# Zyro Pay — crypto card wallet (Android, Kotlin + Jetpack Compose)

A complete, fully-working mock of a RedotPay-style crypto wallet + payment card app.
No backend and no APIs: everything runs on an in-app mock store that persists across restarts.

**Download APK:** https://github.com/freelancersaif2022/Kotlin-Game/releases/download/zyropay-v1.0.0/ZyroPay.apk

## Features

- **Splash → onboarding → welcome**: animated brand splash, 4-page onboarding, hero welcome screen
- **Auth**: sign up / log in by email or phone, simulated SMS/email OTP (tap *Autofill*), forgot & reset password,
  6-digit passcode, biometric unlock (real fingerprint/face prompt), notification permission, app lock after 60s in background
- **Home**: animated total balance (hide/show), 24h PnL, quick actions, shortcuts, KYC banner, card widget,
  auto-scrolling promo carousel, live markets with sparklines, recent activity, pull-to-refresh
- **Card**: virtual & physical cards with real artwork, swipeable card carousel, flip to reveal number/CVV (passcode-protected),
  freeze/unfreeze, top-up with custom keypad, spending limits, change PIN, online/contactless/ATM toggles,
  add to mobile wallet, replace & cancel card, full apply flow (type → design → shipping → pay)
- **Pay**: animated QR scanner, pay merchants from card or wallet, 2% cashback, personal receive QR
- **Wallet**: portfolio donut chart, assets & markets lists, coin detail with interactive price chart,
  deposit (network picker, real QR code, address copy/share, simulated deposit), withdraw (address validation,
  paste/scan, fee, confirm sheet), internal send with user lookup, instant swap with live quote, P2P marketplace & order flow
- **Transactions**: filters, search, grouped by day, monthly in/out, detailed receipts with status timeline, share receipt
- **Notifications**: categories, unread badges, mark all read
- **Me**: profile & avatar, KYC flow (document, capture, selfie, details, review → auto-approve), security center
  (passcode, biometrics, 2FA with QR, password, anti-phishing, whitelist, devices), **light / dark / system theme**,
  language, display currency (10 fiats), notification settings, rewards & VIP, referral, help center, live support chat, about & legal
- Professional top snackbars, bottom toasts, dialogs and bottom sheets throughout

## Demo tips

- Any email + password (8+ chars) logs in to a fully set-up demo account; **Create account** starts a fresh account.
- The OTP code arrives as an in-app message banner — tap **Autofill**.
- KYC is approved automatically ~12 seconds after submitting.

## Build

```bash
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
```

GitHub Actions (`.github/workflows/android.yml`) builds the signed release APK on every push and publishes it to the
`zyropay-v1.0.0` release. The bundled `app/zyropay-release.jks` is a demo signing key — replace it before publishing to Google Play.

Fonts: Inter (SIL OFL 1.1). Brand assets: `docs/CHATGPT_ASSET_PROMPT.md`.

# ChatGPT Asset Generation Prompt — "Zyro Pay" (crypto card wallet app)

> **How to use:** Copy everything inside the box below (from `=== START PROMPT ===` to `=== END PROMPT ===`) into ChatGPT.
> If ChatGPT stops halfway (it often limits images per message), reply **"Continue with the next batch"** until all 8 batches are done, then ask: **"Now package everything into one ZIP exactly following the folder structure."**
>
> **Brand name:** `Zyro Pay` is a placeholder original brand. To rename, find-and-replace `Zyro Pay` / `zyro` everywhere in the prompt before sending.
> We copy RedotPay's **features, screen flow and quality level**, but NOT its name, logo or exact artwork (that would be trademark infringement and Google Play would reject the app).

---

```
=== START PROMPT ===

You are a senior brand designer + mobile UI asset designer working for a top fintech company.
Create a COMPLETE, production-ready brand and asset kit for a new Android fintech app.
The assets will be dropped directly into a Kotlin + Jetpack Compose project, so follow the
file names, formats, sizes and folder structure EXACTLY. At the end, give me everything as ONE ZIP file.

────────────────────────────────────────
1. THE PRODUCT
────────────────────────────────────────
App name: Zyro Pay
Tagline: "Spend crypto like cash. Anywhere."
Category: Crypto wallet + crypto-funded virtual & physical payment card (like RedotPay, Crypto.com, Revolut).
Core features: crypto wallet (USDT, USDC, BTC, ETH, TRX, BNB, SOL), deposit / withdraw / send / receive,
swap/convert, virtual card + physical card, card top-up, scan-to-pay QR, P2P, transaction history,
KYC identity verification, referral & rewards, security (passcode, biometrics, 2FA), 24/7 support.
Target users: global, 20–45 years old, tech-savvy, freelancers, travelers.
Personality: premium, trustworthy, modern, fast, minimal, confident. NOT playful, NOT cartoonish.
Visual references for quality level: RedotPay, Revolut, Crypto.com, Wise, Apple Wallet.
IMPORTANT: The brand must be 100% ORIGINAL. Do not copy or imitate the RedotPay logo, red dot, or any
existing brand's logo. Do not include Visa, Mastercard, Apple, Google or any third-party trademark.

────────────────────────────────────────
2. BRAND IDENTITY (design this first, use it everywhere)
────────────────────────────────────────
2.1 Logo
- Design an original, simple, geometric, memorable symbol (a "Z" monogram mark is preferred —
  e.g. a bold stylized Z built from two parallel strokes / a lightning-fast motion feel / or a
  Z inside a rounded-square). Must read clearly at 24px and at 1024px.
- Deliver: symbol only, wordmark only, horizontal lockup (symbol + "Zyro Pay"), stacked lockup.
- Each in: full color on light, full color on dark, pure white, pure black.
- Wordmark font: geometric sans (Inter / Manrope / SF Pro style), semibold, slightly tight tracking.

2.2 Color palette (give exact HEX; you may refine but keep the structure)
Primary brand:      #5B3DF5 (electric violet)        — primary buttons, active states
Primary dark:       #3E25C9
Primary light:      #EEEAFE                          — tinted backgrounds, chips
Accent gradient:    #5B3DF5 → #8F5BFF → #00D1FF      — card art, hero areas, splash
Success:            #16C47F      Warning: #FFB020      Error: #F04438      Info: #2E90FA
Light mode:  background #F7F8FA, surface #FFFFFF, surfaceVariant #F1F3F7, outline #E4E7EC,
             textPrimary #0B0F19, textSecondary #5D6679, textTertiary #98A2B3
Dark mode:   background #0A0B10, surface #14161F, surfaceVariant #1C1F2B, outline #2A2E3D,
             textPrimary #F5F6FA, textSecondary #A3AAC0, textTertiary #6B7290
Deliver this palette as: a palette image (PNG) AND a file `colors.json` with every token for light and dark.

2.3 Typography
Use "Inter" (Google Fonts, OFL licence). Provide a type scale in `typography.json`:
display 34/40 bold, h1 28/34 bold, h2 22/28 semibold, h3 18/24 semibold, body 16/24 regular,
bodySmall 14/20 regular, label 13/16 medium, caption 12/16 regular, balance numbers 36/42 bold tabular.
(You do NOT need to include the font files — I will add them.)

2.4 Style rules for ALL assets
- Corner radius: cards 20dp, buttons 14dp, chips full-pill, bottom sheets 28dp top.
- Icon style: outlined, 2px stroke (on 24px grid), rounded caps & joins, consistent optical size,
  plus a "filled" version for the selected bottom-nav state.
- Illustrations: premium 3D-ish soft-gradient style (glassmorphism, soft shadows, brand gradient),
  clean, no text inside images, no people's faces, transparent background.
- Everything must look correct on BOTH light and dark backgrounds.

────────────────────────────────────────
3. TECHNICAL RULES (VERY IMPORTANT — the app must stay under 35 MB)
────────────────────────────────────────
- File names: lowercase, snake_case, only a–z, 0–9 and _ (Android resource rules). No spaces, no dashes.
- ICONS & LOGOS: deliver as clean SVG (write real SVG code, single-color paths, viewBox="0 0 24 24"
  for icons, no embedded raster images, no fonts — convert text to paths). Use currentColor or #000000
  for fills/strokes so I can tint them in code.
- ILLUSTRATIONS, CARD ART, ONBOARDING, BANNERS: PNG with transparent background where relevant,
  at the exact size listed. Keep every PNG under 300 KB (I will convert to WebP).
- No watermarks, no text baked into illustrations (all text lives in the app).
- Include a `manifest.json` listing every file: path, format, size, purpose, which screen uses it.

────────────────────────────────────────
4. ASSET LIST (create ALL of these)
────────────────────────────────────────

BATCH 1 — BRAND & APP ICON  → folder: brand/ and app_icon/
- brand/logo_symbol.svg, logo_symbol_white.svg, logo_symbol_black.svg
- brand/logo_horizontal.svg, logo_horizontal_white.svg
- brand/logo_stacked.svg, logo_stacked_white.svg
- brand/wordmark.svg, wordmark_white.svg
- brand/palette.png (1600x1000 color palette sheet), brand/colors.json, brand/typography.json
- brand/brand_guide.png (1600x2400 one-page brand guide: logo, clear space, colors, type, do/don't)
- app_icon/ic_launcher_foreground.png  1024x1024, transparent, symbol centered inside the
  inner 66% safe zone (Android adaptive icon)
- app_icon/ic_launcher_foreground.svg  (same, vector)
- app_icon/ic_launcher_background.png  1024x1024 (brand gradient or solid #5B3DF5)
- app_icon/ic_launcher_monochrome.svg  (single color symbol for Android 13 themed icons)
- app_icon/ic_launcher_full.png 1024x1024 (complete icon, rounded-square preview)
- app_icon/play_store_icon.png  512x512, 32-bit PNG, no transparency
- app_icon/notification_icon.svg  (white-only silhouette of the symbol, 24x24)

BATCH 2 — SPLASH & ONBOARDING → folder: splash/ and onboarding/
- splash/splash_logo.svg  (symbol only, for Android 12+ SplashScreen API — fits inside a circle)
- splash/splash_background_light.png 1080x2400 (very subtle brand gradient glow on #FFFFFF)
- splash/splash_background_dark.png  1080x2400 (subtle glow on #0A0B10)
- onboarding/onboarding_1.png 1200x1200 — "Your crypto, one card": floating 3D payment card with
  coins (generic BTC/ETH/USDT style coins) orbiting it
- onboarding/onboarding_2.png 1200x1200 — "Pay anywhere worldwide": card + globe + contactless waves
- onboarding/onboarding_3.png 1200x1200 — "Instant top-up & swap": coins flowing into a wallet with arrows
- onboarding/onboarding_4.png 1200x1200 — "Bank-grade security": shield + lock + fingerprint
- onboarding/welcome_hero.png 1200x1400 — hero for Welcome screen (stack of 3 premium cards, tilted)

BATCH 3 — PAYMENT CARD DESIGNS → folder: cards/
All 1712x1080 (credit-card ratio 1.585:1), rounded corners 64px, transparent outside the card.
Include: Zyro Pay logo top-left, contactless symbol, EMV chip, a generic network-mark PLACEHOLDER
area bottom-right (an empty rounded rectangle — NO Visa/Mastercard logo). NO card number text
(I render numbers in code).
- cards/card_virtual_violet.png  (brand gradient, subtle wave pattern)
- cards/card_virtual_midnight.png (deep navy/black with holographic violet lines)
- cards/card_virtual_aurora.png  (violet → cyan aurora)
- cards/card_physical_black.png  (matte black metal look, embossed logo)
- cards/card_physical_silver.png (brushed metal silver)
- cards/card_physical_gold.png   (premium gold, for VIP tier)
- cards/card_frozen_overlay.png  (frosted ice overlay, semi-transparent, same size)
- cards/card_back.png            (back side: magnetic stripe, signature strip, no text)
- cards/card_chip.svg, cards/contactless.svg

BATCH 4 — CRYPTO & CURRENCY ICONS → folder: coins/
Flat, circular, 128x128 SVG, modern simplified versions recognizable by color & symbol:
- coins/coin_usdt.svg, coin_usdc.svg, coin_btc.svg, coin_eth.svg, coin_trx.svg, coin_bnb.svg,
  coin_sol.svg, coin_ton.svg, coin_matic.svg
- Network badges (small 48x48 SVG): network_trc20.svg, network_erc20.svg, network_bep20.svg,
  network_solana.svg, network_polygon.svg, network_ton.svg
- Fiat flags circular 64x64 SVG: flag_usd.svg, flag_eur.svg, flag_gbp.svg, flag_hkd.svg,
  flag_sgd.svg, flag_bdt.svg, flag_inr.svg, flag_aed.svg, flag_jpy.svg, flag_cny.svg

BATCH 5 — UI ICON SET (outlined + filled) → folder: icons/
24x24 SVG, 2px stroke, rounded. Each icon as ic_<name>.svg (outlined). For bottom-nav icons also
ic_<name>_filled.svg.
Bottom navigation: home, card, pay (scan/QR in a circle), wallet, profile  (+ _filled versions)
Actions: deposit, withdraw, send, receive, swap, top_up, transfer, buy, sell, p2p, scan_qr, qr_code,
  copy, share, download, upload, add, plus_circle, minus, close, check, check_circle, arrow_left,
  arrow_right, arrow_up, arrow_down, arrow_up_right, arrow_down_left, chevron_right, chevron_down,
  chevron_up, more_horizontal, more_vertical, search, filter, sort, refresh, history, calendar, clock
Card: card_freeze, card_unfreeze, card_lock, card_settings, card_limit, card_pin, card_details,
  card_add, card_physical, card_virtual, contactless, atm, online_payment, apple_wallet_generic
  (generic phone-wallet icon), replace_card
Security: lock, unlock, shield, shield_check, fingerprint, face_id, passcode, key, two_fa,
  eye, eye_off, device, logout, warning, info, help
Account: user, user_verify (KYC), id_card, passport, selfie_camera, camera, gallery, document,
  address, email, phone, globe_language, moon (dark mode), sun (light mode), bell, bell_badge,
  settings, gift (referral), rewards, star, crown (VIP), support_chat, headset, faq, terms,
  privacy, about, rate, invite, link, edit, delete, trash
Transactions: category_shopping, category_food, category_travel, category_transport,
  category_entertainment, category_subscription, category_utilities, category_transfer,
  category_refund, category_fee, category_cashback, category_atm, status_pending,
  status_success, status_failed
Misc: wifi_off (no internet), empty_box, chart, trending_up, trending_down, percent, merchant,
  store, location, sparkle

BATCH 6 — STATE / EMPTY / RESULT ILLUSTRATIONS → folder: illustrations/
800x800 PNG, transparent, same 3D-soft style as onboarding:
- illustrations/success_check.png (big glowing check — payment / transfer success)
- illustrations/failed_cross.png  (soft red cross — failed)
- illustrations/pending_clock.png (processing)
- illustrations/empty_transactions.png (empty receipt list)
- illustrations/empty_notifications.png (sleeping bell)
- illustrations/empty_cards.png (no card yet — card outline with plus)
- illustrations/no_internet.png (cloud with broken wifi)
- illustrations/kyc_intro.png (ID card + shield)
- illustrations/kyc_id_scan.png (ID card inside scan frame)
- illustrations/kyc_selfie.png (face outline inside scan circle — NO real face)
- illustrations/kyc_review.png (documents with magnifier — under review)
- illustrations/kyc_approved.png (ID with green check badge)
- illustrations/card_apply.png (card coming out of a phone)
- illustrations/card_shipping.png (parcel box with card — physical card delivery)
- illustrations/security_setup.png (shield + phone + lock)
- illustrations/biometric.png (fingerprint glowing)
- illustrations/referral_gift.png (gift box with coins bursting)
- illustrations/rewards_trophy.png (trophy with coins)
- illustrations/vip_crown.png (crown on gold card)
- illustrations/maintenance.png (gears)
- illustrations/update_app.png (rocket)
- illustrations/support_agent.png (headset + chat bubbles, no face)
- illustrations/swap_coins.png (two coins with circular arrows)
- illustrations/deposit_coins.png (coins dropping into a wallet)
- illustrations/withdraw.png (coins leaving a wallet)
- illustrations/p2p.png (two phones exchanging coins)
- illustrations/notification_permission.png (bell with sparkle)
- illustrations/logout.png (door with arrow)

BATCH 7 — HOME BANNERS & PROMO → folder: banners/
1600x640 PNG, rounded 40px corners, no text (text added in-app), left 55% kept clean for text:
- banners/banner_cashback.png   (coins + percent sign, violet)
- banners/banner_referral.png   (gift + friends icons, cyan)
- banners/banner_physical_card.png (black metal card floating)
- banners/banner_zero_fee.png   (0% with sparkles)
- banners/banner_travel.png     (card + plane + globe)
- banners/banner_security.png   (shield)
Also:
- banners/rewards_header_bg.png 1080x720 (gradient abstract shapes for Rewards screen header)
- banners/profile_header_bg_light.png and profile_header_bg_dark.png 1080x600 (subtle abstract)
- banners/card_screen_bg_light.png and card_screen_bg_dark.png 1080x1400 (soft glow behind card)

BATCH 8 — AVATARS, MERCHANTS, STORE GRAPHICS → folders: avatars/, merchants/, store/
- avatars/avatar_default.svg (generic person silhouette in brand-tinted circle)
- avatars/avatar_1.png … avatar_8.png 256x256 — abstract gradient blob/geometric avatars (NO real faces)
- merchants/merchant_<name>.svg 64x64 — FICTIONAL generic merchant icons in colored circles for mock
  transactions: shopping_bag, coffee, burger, taxi, airplane, hotel, streaming, music, gaming,
  grocery, pharmacy, electronics, fuel, gym, cloud_service, mobile_topup
- store/play_feature_graphic.png 1024x500 (logo + tagline + 3D card, for Google Play)
- store/screenshot_frame_1.png … screenshot_frame_5.png 1080x1920 — background frames with headline
  area at top for Play Store screenshots (headlines: "One card for all your crypto",
  "Spend in 170+ countries", "Instant swap & top-up", "Bank-grade security", "Rewards on every spend")

────────────────────────────────────────
5. ZIP STRUCTURE (exactly)
────────────────────────────────────────
zyro_pay_assets.zip
├── manifest.json
├── README.md            (short brand guide: colors, fonts, spacing, usage rules)
├── brand/
├── app_icon/
├── splash/
├── onboarding/
├── cards/
├── coins/
├── icons/
├── illustrations/
├── banners/
├── avatars/
├── merchants/
└── store/

────────────────────────────────────────
6. QUALITY CHECKLIST (verify before delivering)
────────────────────────────────────────
[ ] All assets share one consistent style, palette and lighting direction (light from top-left).
[ ] Every SVG opens in a browser, uses viewBox, has no <image> or <text> tags, is under 10 KB.
[ ] All icons on the same 24px grid with 2px stroke; outlined and filled versions match.
[ ] No third-party trademarks (no Visa, Mastercard, RedotPay, Apple, Google logos), no watermarks.
[ ] No text inside illustrations / banners / cards (except the Zyro Pay logo on cards & store graphics).
[ ] Transparent backgrounds where specified; PNGs each under 300 KB.
[ ] File names exactly as listed, lowercase snake_case.
[ ] Looks good on #FFFFFF and on #0A0B10.
[ ] manifest.json lists every file.

Work batch by batch (Batch 1 → 8). After each batch, show me previews, then continue.
When all batches are done, package everything into ONE ZIP with the structure above.

=== END PROMPT ===
```

---

## Notes for after you get the ZIP

- **Fonts:** Inter will be bundled by me (OFL licence) — no need from ChatGPT.
- **Animations:** splash animation, card flip, success check, shimmer loading, pull-to-refresh, etc.
  will be built natively in Jetpack Compose (smaller and smoother than Lottie files).
- **Size budget:** all PNGs get converted to WebP and SVGs to Android Vector Drawables, so the
  final APK/AAB should land well under 30–40 MB.
- **Card network logo:** if you later get a real card-issuing partner, use their official logo files
  from their brand portal — never AI-generated versions.
- If any batch looks off (wrong style, text inside images, bad icon), ask ChatGPT to regenerate only
  that file before zipping.

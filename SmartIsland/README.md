# Smart Island (Android, Kotlin)

A Dynamic-Island style app for Android: a live island at the top of the screen that morphs for calls,
music, charging, volume, ringer changes and notifications, plus 12 real-time home-screen widgets,
software power / volume buttons, a premium tier with mock subscriptions, and rewarded-ad widget unlocks.

> Name note: Apple owns "Dynamic Island". Keep the store listing, icon and screenshots free of Apple
> names and logos ("Smart Island" is used throughout).

## Open & run

1. Open the `SmartIsland/` folder in Android Studio (Ladybug or newer). Let it sync. AGP 8.7, Kotlin 2.0,
   compileSdk 35, minSdk 26.
2. Run on a device (overlays and accessibility don't behave well on most emulators).
3. Walk through onboarding → **Setup** → grant **Accessibility** and **Notification access**.
4. Home → **Try a live activity** chips fire demo events onto the real overlay.

## What's inside

| Area | Where |
|---|---|
| Island state engine (priority: call > transient > media > idle) | `island/IslandEngine.kt` |
| Calls, media sessions, notification alerts | `island/IslandNotificationListener.kt`, `island/MediaTracker.kt` |
| Island UI + spring animations (idle / compact / expanded) | `island/ui/IslandView.kt`, `island/ui/IslandContent.kt` |
| Overlay host, volume-key HUD, battery & ringer events | `overlay/IslandAccessibilityService.kt`, `overlay/IslandOverlay.kt` |
| Floating power/volume edge panel | `overlay/EdgePanel.kt` |
| Power menu / lock / screenshot actions | `system/SystemActions.kt` |
| 0–100% volume per stream | `system/VolumeController.kt` |
| Permissions + Xiaomi/Samsung/OPPO/vivo/Huawei/ASUS autostart guides | `system/Permissions.kt`, `system/OemGuide.kt` |
| 12 widgets (RemoteViews, TextClock / AnalogClock / Chronometer for real-time ticking) | `widget/`, `res/layout/widget_*.xml` |
| Premium state, mock billing | `data/PremiumRepository.kt`, `billing/BillingGateway.kt` |
| UMP consent + rewarded ads | `ads/AdsManager.kt` |
| Quick Settings tiles (Lock, Island) | `tile/Tiles.kt` |
| Screens (Compose) | `ui/screens/` |

### Free vs PRO

* **Free:** the island (music, calls, charging, ringer), all size/position controls, power & volume controls,
  6 widgets (Digital Clock, Analog Clock, Battery Ring, Calendar, Power Buttons, Volume Control).
* **PRO (monthly / yearly):** notification alerts, island volume HUD, edge panel, accent colors + glow, and
  the 6 premium widgets.
* **Rewarded ad:** unlocks a single premium widget **forever**.

Change the split in `PremiumFeature` / `WidgetKind.premium` and `IslandEngine.allowed`.

## Before publishing to Google Play

1. **AdMob:** replace the Google *test* IDs in `app/build.gradle.kts` (`admobAppId`,
   `REWARDED_AD_UNIT_ID`) with your own. Set up the UMP consent message (GDPR) in AdMob → Privacy & messaging.
2. **Billing:** `MockBillingGateway` grants subscriptions locally. Add
   `com.android.billingclient:billing-ktx`, implement `BillingGateway` with `BillingClient`
   (`queryProductDetails`, `launchBillingFlow`, acknowledge purchases, `queryPurchasesAsync` for restore), create
   `smart_island_monthly` / `smart_island_yearly` subscriptions in Play Console, then set `MOCK_BILLING` to false.
   The Settings → "Test mode" section only shows with mock billing.
3. **Privacy policy:** host one and set `PRIVACY_POLICY_URL` in `ui/screens/SettingsScreen.kt` and in Play Console.
4. **Accessibility declaration:** Play Console → App content → Accessibility API. Explain the island overlay,
   power/lock/screenshot actions and volume-key HUD, and attach a short video showing the in-app prominent
   disclosure (Setup screen dialog) and each feature. `isAccessibilityTool` is false on purpose.
5. **Notification listener:** describe it in the Data safety form (processed on device, not collected).
6. **New personal developer accounts:** run a closed test with at least 12 testers for 14 days before production.
7. Generate a signed release bundle (`Build → Generate Signed App Bundle`). R8 minify and resource shrinking are on.

## Device notes

* Xiaomi/Redmi/POCO, OPPO/Realme/OnePlus, vivo/iQOO, Huawei/Honor and Samsung kill background services. The
  in-app guide (Settings → Keep running in background) detects the brand and deep-links to the right screens.
* Android 13+: sideloaded APKs need App info → ⋮ → *Allow restricted settings* before the accessibility toggle
  works. The Setup screen detects this and explains it.
* The hardware power key can't be remapped without root. The app gives software replacements (lock, power menu,
  screenshot) in the Controls tab, the Power widget, the edge panel and the Lock Quick Settings tile.

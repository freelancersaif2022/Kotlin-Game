# FlashShare – ultra-fast offline file transfer (Kotlin)

A SHAREit-style Android app written in Kotlin with Jetpack Compose. Phones connect directly over
**Wi-Fi Direct forced to 5 GHz or 6 GHz**. No internet and no mobile data are used.

## Flow
Splash → animated onboarding (Next / Skip) → profile setup (avatar or custom photo, device name) → Home.

* **Send:** pick files (apps, photos, videos, music, files, whole folders) → **scan** the receiver's QR code → transfer.
* **Receive:** a QR code is generated automatically, with your avatar in the centre.
* Both sides see each other's avatar and device name, plus live speed, ETA and per-file progress.
  Either side can tap **Send more**.

## Speed design
* The receiver hosts an autonomous Wi-Fi Direct group pinned to **5 GHz** (or 6 GHz on Wi-Fi 6E/7 phones).
  The fallback order is 6 → 5 → auto. The sender joins on the exact frequency from the QR code, so there is no discovery delay and no system pairing dialog.
* **4 parallel TCP streams**. Big files are split into 8 MB chunks spread across the streams.
* Files are sent with **zero-copy `FileChannel.transferTo`** (sendfile). The receiver writes with positional writes into 1 MB direct buffers.
* 4 MB socket buffers, TCP_NODELAY, a high-perf + low-latency Wi-Fi lock, and a wake lock in a foreground service.
* **Free plan is capped at 25 MB/s.** **Premium is uncapped**, so the only limit is the Wi-Fi chip.
  Typical speeds: Wi-Fi 5 ≈ 30–50 MB/s, Wi-Fi 6 ≈ 50–100 MB/s, Wi-Fi 6E/7 can go higher.

## Premium (monthly / yearly subscription) and ads
| Feature | Free | Premium |
|---|---|---|
| Transfer speed | up to 25 MB/s | uncapped |
| Interstitial ad after each transfer | yes | no |
| Group Share (1 → up to 8 phones) | watch a rewarded ad each time | ✓ |
| Phone Clone | watch a rewarded ad each time | ✓ |
| Cleaner | watch a rewarded ad each time | ✓ |
| File manager | ✓ | ✓ |
| AES-256 encryption with pairing PIN | – | ✓ |

If no rewarded ad can load (for example offline), the feature is unlocked for that use instead of blocking the user.

### Play Console setup
Create two subscriptions: `flashshare_premium_monthly` and `flashshare_premium_yearly`.
**Restore purchases** is in Settings and on the Premium screen.

Builds signed with the test key use a **mock store**, so you can try Premium without Play Console.
In Settings → Manage subscription you can switch back to Free.

### Release builds
Add these GitHub secrets to sign with your own key and use real AdMob IDs:
`FLASHSHARE_KEYSTORE_BASE64`, `FLASHSHARE_KEYSTORE_PASSWORD`, `FLASHSHARE_KEY_ALIAS`, `FLASHSHARE_KEY_PASSWORD`,
`FLASHSHARE_ADMOB_APP_ID`, `FLASHSHARE_ADMOB_INTERSTITIAL_ID`, `FLASHSHARE_ADMOB_REWARDED_ID`.
Without them, Google's test ad IDs and the public test key in `signing/` are used. Never upload a test-key build to Play.

## Tests
`engine-test/` compiles the real transfer engine on the plain JVM and runs real TCP transfers over loopback.
It checks: mixed sizes (0 B to 90 MB) with SHA-256 verification, PIN encryption and wrong-PIN rejection,
reverse direction with folders, group 1→2, cancel cleanup, busy rejection, and the free-plan cap.

```
./gradlew -p engine-test test
```

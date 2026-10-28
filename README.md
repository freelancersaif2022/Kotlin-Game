# Inkly — Notes, beautifully kept

A fast, lightweight note-taking app for Android, built with **Kotlin + Jetpack Compose (Material 3)**.

## Features

- **Splash → Welcome → Notes** flow. The animated splash shows on every launch; the 3-page welcome pager shows only on first launch.
- **First-run guided tour.** Spotlight coach marks point out *New note*, *Search*, *Settings* and *long-press*. You can replay it from Settings.
- **Lined writing box.** Tap **+** and you get a page ruled with dashed lines. Press Enter and a new line opens: the box grows and the dashed rules move down with it.
- **Autosave.** Every keystroke is saved a moment after you stop typing (*Saving… → Saved*). Edits are also flushed when you go back or leave the app.
- Pinned notes, six note colours, instant search, long-press to pin or delete, and *Undo* after a delete.
- **Google Drive backup:**
  - **Connect Google Drive** from Settings (⚙ at the top right).
  - **Back up now**, or let **automatic daily backup** run in the background through WorkManager. It runs even when the app is closed, needs a network connection, retries on failure, and survives reboots.
  - **Restore** merges your latest backup with what is on the device. The newest edit of each note wins and nothing is deleted.
  - Backups go to the private, hidden *appDataFolder* in your own Drive (scope `drive.appdata`). Only Inkly can read it, and the 5 most recent snapshots are kept.
- **Light / Dark / System** theme. Light is the default.
- Custom animated popups, sheets and progress dialogs, spring animations, and edge-to-edge UI.
- **Small APK.** R8 full mode, resource shrinking and no heavy Google API client: Drive is called through its REST API directly.

## Download the APK

Every push builds the app on GitHub Actions (`.github/workflows/android.yml`):

- **Releases → "Inkly – latest build"** has `Inkly.apk`, which you can install directly on a phone.
- Each workflow run also uploads the APK as an artifact. The run summary shows its size and signing certificate.

## ⚠️ One-time Google Cloud setup (required for Drive)

Google only lets an Android app use Drive after its package name and signing-certificate SHA-1 are registered as an OAuth client. Until you do this, *Connect Google Drive* shows *"Google sign-in isn't set up for this build yet"*. You only need to do it once:

1. Open <https://console.cloud.google.com/> and create a project (or pick an existing one).
2. Go to **APIs & Services → Library**, find **Google Drive API** and click **Enable**.
3. Go to **APIs & Services → OAuth consent screen** (Google Auth Platform → Branding / Audience):
   - Choose User type **External** and enter the app name *Inkly* and your email.
   - Under **Data access / Scopes**, add `.../auth/drive.appdata`.
   - While the app is in *Testing*, add your Google account under **Test users**. Or click **Publish app**: `drive.appdata` is not a restricted scope.
4. Go to **APIs & Services → Credentials → Create credentials → OAuth client ID** and fill in:
   - Application type: **Android**
   - Package name: `com.inkly.notes`
   - SHA-1 certificate fingerprint: the SHA-1 of the key that signs your APK. The GitHub Actions run summary prints it under *Signer #1 certificate SHA-1 digest*.
5. Save. After a few minutes, **Connect Google Drive** works. You don't need to change any code or download a JSON file.

### Signing key (keeps the SHA-1 stable)

Google Drive access is tied to the signing certificate. CI therefore signs release APKs with a key from **repository secrets**:

| Secret | Value |
|---|---|
| `INKLY_KEYSTORE_BASE64` | `base64 -w0 my.jks` |
| `INKLY_KEYSTORE_PASSWORD` | keystore password |
| `INKLY_KEY_ALIAS` | key alias |
| `INKLY_KEY_PASSWORD` | key password |

To create a key, run `keytool -genkeypair -keystore my.jks -alias inkly -keyalg RSA -keysize 2048 -validity 10000`. Then add the secrets under **Settings → Secrets and variables → Actions**.

If the secrets are missing, CI signs with an auto-generated debug key. That key is cached between runs, but GitHub can evict the cache, which changes the SHA-1. Use the secrets for anything you rely on.

## Build locally

```bash
./gradlew assembleRelease      # -> app/build/outputs/apk/release/app-release.apk
```

You need JDK 17+ and the Android SDK (compileSdk 35).

## Tech

Kotlin 2.0 · Jetpack Compose (BOM 2024.12) · Material 3 · Navigation Compose · Room · WorkManager · Google Identity AuthorizationClient · Drive REST v3 · minSdk 26 / targetSdk 35.

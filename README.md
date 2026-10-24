# Pulse Messenger

A complete WhatsApp-style messenger for Android, built with **Kotlin + Jetpack Compose (Material 3)**.
It runs fully offline on realistic mock data — no server or backend needed.

## Features

**Onboarding** – welcome carousel, language picker, phone number with country search, number confirmation,
OTP with auto-detect + resend, profile name/photo, backup restore with progress, permissions intro.

**Chats** – chat list with search, filters (All / Unread / Favourites / Groups), archived row, pin (max 3),
mute, archive with undo, delete with undo, mark read/unread, favourites, chat lock, typing indicators, drafts.

**Conversation** – bubbles with tails and inline timestamps, ticks (sending → sent → delivered → read),
auto replies, typing bubble, date chips, unread divider, encryption banner, swipe-to-reply, reply quotes
(tap to jump), reactions (quick bar + full picker + who-reacted sheet), multi-select (reply, star, copy,
forward, delete for me / everyone with undo, edit, info), in-chat search with highlights, scroll-to-bottom
badge, emoji & sticker panel, hold-to-record voice notes with slide-to-cancel, attachments (gallery
multi-select with caption, camera, documents, location & live location, contacts, audio, polls),
link previews, disappearing messages, per-chat wallpaper, export chat, block/report, clear chat.

**Updates** – status cards with segmented rings, story viewer (tap/hold/swipe, progress bars, replies,
quick reactions, viewers list, mute, delete), text status composer, channels (follow, reactions, mute),
channel directory with categories.

**Communities** – community list, community page with announcements and groups, create community.

**Calls** – call log with filters, favourites, call links, voice & video call screen (ringing, timer,
mute, speaker, camera, draggable self view), incoming call UI.

**More** – new chat / group / broadcast / contact, add members, forward to up to 5 chats, contact & group
info (admins, invite link, media strip, common groups), message info, media viewer with pinch-zoom and
mock video playback, media/docs/links gallery, global search with type filters, starred messages, QR code
(real QR via ZXing) and scanner, linked devices.

**Settings** – profile, about, account (passkeys, email, two-step verification, change number, delete
account, log out), privacy (last seen, photo, about, status, read receipts, groups, blocked, app lock with
PIN/fingerprint), chats (theme light/dark/system, wallpapers + dimming, font size, enter-is-send, backup),
notifications, storage & data (manage storage, auto-download, upload quality), help, app info.

All popups use a shared design system: animated dialogs with icon badges, radio/checkbox/input dialogs,
bottom sheets and typed snackbars (success / error / info / undo).

## Build

Requirements: JDK 17 and the Android SDK (API 35).

```bash
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
./gradlew assembleDebug
```

GitHub Actions (`.github/workflows/android.yml`) builds the release APK on every push and uploads it as
the `pulse-release-apk` artifact. The release build is minified (R8 + resource shrinking) and is signed
with the debug key so it can be installed directly — replace the signing config before publishing.

For the smoothest scrolling test the **release** build: debug Compose builds are much slower.

## Project layout

```
app/src/main/java/com/pulse/messenger/
  data/        models, mock data, in-memory repository, persisted settings
  ui/theme/    colors, typography, light/dark theme
  ui/components/  avatars, dialogs, sheets, snackbars, settings rows, wallpaper
  ui/navigation/  routes and animated NavHost
  ui/onboarding, home, chat, contact, media, status, camera, channel,
  ui/community, call, newchat, search, settings
```

Image assets (avatars, media, stickers, wallpapers, illustrations) live in `res/drawable-nodpi`.
Store listing graphics are in `docs/store/`.

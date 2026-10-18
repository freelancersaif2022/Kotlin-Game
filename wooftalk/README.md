# WoofTalk 🐶 – Animal Voice Translator (prank app)

Speak in **any language** and WoofTalk turns your voice into a cartoon animal
voice. Everything runs **on the phone**: no backend, no login, nothing uploaded
and nothing saved.

## Features

- **20 voices with real animal recordings**: Buddy Dog (free), plus 19 locked
  voices: Tiny Puppy, Kitty Cat, Night Wolf, Silly Duck, Moo Cow, Loud Rooster,
  Fluffy Sheep, Happy Pig, Wild Horse, Silly Goat, Froggy, Big Tiger, King Lion,
  Cool Zebra, Sweet Deer, Tweety Bird, Big Elephant, Cheeky Monkey and Night Owl.
- **Unlock with rewarded ads**: watch 2 rewarded videos to unlock a voice forever.
- **Interstitial ad** after every 2nd translation (free users only).
- **PRO screen** with mock Monthly ($2.99) / Yearly ($19.99) plans that unlock
  all voices and remove ads. There is also a mock restore and cancel.
- **Real sounds, always different**: every syllable you speak becomes a real
  recorded animal call (from Wikimedia Commons), picked at random and slightly
  re-pitched, so no two translations sound the same.
- 2–3 second "translating…" animation after you stop, then auto-play.
- Animated cartoon characters that blink, wiggle their ears and lip-sync.
- Animated splash screen, native splash, custom popups and snackbars, and a
  funny app icon.
- Share the result as a `.wav` file.

## How it works

1. `record` streams 16 kHz PCM from the mic into memory.
2. `lib/audio/translator_engine.dart` finds syllables from the loudness envelope.
   It only uses rhythm and energy, which is why every language works.
3. `arrangeCalls` places real recorded calls from `assets/sounds/<animal>/`
   on that rhythm, with random clip choice and pitch. It runs in a background
   isolate.
4. The WAV is written to the temp folder, played with `just_audio`, and the
   previous one is deleted.

## Before publishing to Google Play

1. **AdMob**: replace the Google test ids in `lib/services/ad_service.dart` and
   the `APPLICATION_ID` in `android/app/src/main/AndroidManifest.xml`.
2. **Billing**: the PRO purchase is a mock (`AppState.activatePremium`). Swap in
   Google Play Billing (`in_app_purchase`) before going live.
3. **Signing**: create an upload keystore and either add `android/key.properties`
   locally or add the `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and
   `KEY_PASSWORD` repository secrets for CI.
4. Add a privacy policy (microphone and ads) to the store listing.

## Build

```bash
flutter pub get
flutter test
flutter build apk --release
```

The GitHub Actions workflow `.github/workflows/wooftalk-android.yml` builds the
release APK on every push. It uploads the APK as an artifact and publishes it as
a GitHub Release.

## Sounds

`assets/sounds/` holds short clips cut from freely licensed recordings on
Wikimedia Commons (Public domain, CC0, CC BY, CC BY-SA). Authors and licenses
are listed in `assets/sounds/CREDITS.md` and in the app under
**? → Sound credits**. To refresh them, edit `tool/sound_sources.json` and run
the "WoofTalk fetch real animal sounds" workflow.

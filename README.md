# Lexora — Master English, A to Z

A complete, offline, ad-free English learning app for Android, built with Kotlin and Jetpack Compose.

## Features

- **Splash, onboarding and profile setup:** choose one of 10 avatars or a camera/gallery photo, then set your name, level, goal and daily target.
- **Home:** course progress ring, Start/Continue, streak, total course time, XP, today's goal, a daily 5-question challenge, a course carousel, locked real-life projects and certificate status. The notification bell shows an unread badge. A first-run guided tutorial explains each part of the screen.
- **Learn:** a weekly activity bar chart, a course donut chart, and a 12-module A-to-Z course (48 lessons). Every lesson has explanations, visual example cards with audio, and a pro tip. Each module ends with a quiz that needs 70% to pass. Modules unlock one after another.
- **15 real-life projects:** unlocked after the course. Each one has a brief, key phrases, vocabulary, a spoken dialogue, a quiz and a writing task with instant feedback.
- **Certificate:** a professional certificate that shows the learner's name, the issue date and a certificate ID, and carries the signature of **Saif A.** It can be saved as PNG or PDF, or shared.
- **Practice:** timed grammar test, Writing Lab with offline feedback, speaking (speech recognition plus a similarity score), fill the gap, sentence builder, listening dictation and flashcards.
- **Notifications:** sent for lesson, module, course, project and certificate milestones, practice sessions, streaks and the new daily quiz, which arrives at a random time each day. All notifications are saved on the phone and shown in the in-app notification centre.
- **Settings:** profile editing, light/dark/system theme (light is the default), font size, vibration, voice speed, notification switches, tutorial replay, share, privacy, about and progress reset.
- Everything is stored locally on the phone. There is no account, no ads and no premium tier.

## Build

```
./gradlew assembleRelease
```

The GitHub Actions workflow `.github/workflows/lexora-apk.yml` builds a signed release APK on every push to this branch. It publishes the APK as a workflow artifact and as a GitHub pre-release.

The release build is signed with `app/lexora-debug.keystore` so that each new build installs over the previous one. Replace it with your own private key before publishing on Google Play.

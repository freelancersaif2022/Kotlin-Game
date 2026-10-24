# ChatGPT Asset Generation Prompt — "Pulse" Messenger

Copy everything inside the block below into ChatGPT (a model with image generation + the Python/code tool).
If ChatGPT stops partway, reply: **"Continue with the next batch. Keep the same style, names, and specs."**

```text
You are a senior product designer and Android asset engineer. Produce a complete, production-quality
asset pack for a native Android messaging app (Kotlin + Jetpack Compose). The app is a WhatsApp-style
messenger, but it has its OWN brand — do NOT reproduce WhatsApp's logo, phone-bubble mark, exact green
(#25D366 / #075E54), wordmark, or any Meta trademark. No real brands, logos, celebrities, or watermarks
anywhere.

═══════════════════════════════════════
1. BRAND
═══════════════════════════════════════
App name: Pulse
Concept: fast, private, warm conversations.
Logo idea: a rounded speech bubble whose tail forms a small heartbeat/pulse line. Simple, geometric,
reads clearly at 24px.
Palette (use consistently in every illustration):
  Primary     #0E9F8A (deep teal)
  Primary dark#0A7A6A
  Accent      #34D1B6 (mint)
  Highlight   #FFB547 (warm amber, use sparingly)
  Ink         #111B21
  Muted       #667781
  Surface lt  #FFFFFF / #F0F2F5
  Surface dk  #0B141A / #111B21 / #202C33
Illustration style: modern flat vector-look with soft gradients and subtle shadows, rounded shapes,
2–3 palette colors per scene plus neutrals, diverse friendly characters, generous whitespace,
NO text inside images. Must look good on both light (#FFFFFF) and dark (#111B21) backgrounds.
Photo style (avatars, media, status): natural, realistic smartphone photography, good light,
varied ages/ethnicities/settings, fictional people only.

═══════════════════════════════════════
2. TECHNICAL RULES (MANDATORY)
═══════════════════════════════════════
- File names: lowercase a–z, 0–9 and underscore only, must start with a letter
  (Android resource rule). Example: avatar_person_01.webp
- Formats: photos → WebP quality 75–80. Transparent art → WebP lossless or PNG-32 (only where noted).
- Use the Python tool to resize, crop, convert, compress and zip. Strip all EXIF/metadata.
- Exact pixel sizes as listed. Square items must be truly square and centered.
- TOTAL ZIP SIZE ≤ 15 MB (the final app must stay under 40 MB). If over, lower photo quality to 70
  and reduce photo dimensions, never the icon/brand assets.
- No text, letters, UI chrome, or watermarks baked into any image unless stated.
- Transparent images must have clean edges (no white halos, no checkerboard baked in).

═══════════════════════════════════════
3. FOLDER STRUCTURE & ASSET LIST
═══════════════════════════════════════
pulse_assets/
│
├── 01_brand/
│   ├── ic_launcher_foreground.png    432×432 PNG transparent. Logo in white, centered, fully inside
│   │                                 the central 288×288 safe zone (adaptive icon).
│   ├── ic_launcher_background.png    432×432 PNG, teal gradient #0E9F8A → #0A7A6A (top-left to bottom-right).
│   ├── ic_launcher_monochrome.png    432×432 PNG transparent, single-color white silhouette of the logo
│   │                                 (Android 13 themed icon), same safe zone.
│   ├── ic_launcher_legacy.png        512×512 PNG, full rounded-square icon (bg + logo combined).
│   ├── splash_logo.png               288×288 PNG transparent, logo inside a 192px-diameter circle area
│   │                                 (Android 12 SplashScreen API).
│   ├── logo_full_color.png           1024×1024 PNG transparent, logo in brand colors.
│   ├── logo_white.png                1024×1024 PNG transparent, all white.
│   ├── wordmark_light.png            1200×300 PNG transparent, logo + "Pulse" text, dark ink (text allowed here).
│   ├── wordmark_dark.png             1200×300 PNG transparent, white version (text allowed here).
│   ├── play_store_icon.png           512×512 PNG 32-bit.
│   └── play_feature_graphic.png      1024×500 PNG/JPG, brand banner with phone mockup, no small text.
│
├── 02_onboarding/  (1080×1080 WebP transparent, illustrations, no text)
│   ├── onb_welcome.webp              Diverse friends chatting across phones, bubbles floating.
│   ├── onb_privacy_e2e.webp          Padlock + shield wrapping a chat bubble (end-to-end encryption).
│   ├── onb_calls.webp                Voice/video call between two people, waves/connection lines.
│   ├── onb_status.webp               Story rings / status circles, photo cards.
│   ├── onb_communities.webp          Several groups linked to one hub.
│   ├── onb_verify_phone.webp         Phone with OTP/SMS bubble (digits as abstract dots, no text).
│   ├── onb_profile_setup.webp        Person holding a camera/frame to set a profile photo.
│   ├── onb_backup_restore.webp       Cloud with circular arrows and chat bubbles.
│   ├── perm_contacts.webp            Address book cards.
│   ├── perm_notifications.webp       Bell with notification badges.
│   ├── perm_camera_mic.webp          Camera and microphone.
│   ├── perm_storage_media.webp       Folder with photos/videos/docs.
│   └── perm_location.webp            Map pin on stylized map.
│
├── 03_empty_states/  (720×720 WebP transparent, soft, muted, friendly)
│   ├── empty_chats.webp              empty_calls.webp            empty_status.webp
│   ├── empty_communities.webp        empty_channels.webp         empty_archived.webp
│   ├── empty_starred.webp            empty_search.webp           empty_media.webp
│   ├── empty_docs.webp               empty_links.webp            empty_blocked.webp
│   ├── empty_linked_devices.webp     empty_broadcast.webp        empty_notifications.webp
│   ├── state_no_internet.webp        state_error_generic.webp    state_storage_full.webp
│   └── state_success_check.webp      (big friendly success badge for confirmation sheets)
│
├── 04_wallpapers/
│   ├── wp_doodle_light_tile.png      1024×1024 SEAMLESS tileable doodle pattern (chat-themed line
│   │                                 doodles: bubbles, hearts, stars, coffee, music notes, planes),
│   │                                 lines #D9DBD5 on #EFEAE2-ish beige, very low contrast.
│   ├── wp_doodle_dark_tile.png       Same pattern, lines #1F2C33 on #0B141A, very low contrast.
│   ├── wp_bright_01..06.webp         1080×2340 WebP q70, soft abstract gradients/blobs (bright set).
│   ├── wp_dark_01..06.webp           1080×2340 WebP q70, deep moody abstract gradients (dark set).
│   └── wp_photo_01..06.webp          1080×2340 WebP q70, calm blurred nature/city photos, low detail.
│
├── 05_avatars/
│   ├── avatar_person_01..40.webp     512×512 WebP q78, realistic head-and-shoulders portraits of fictional
│   │                                 people. 20 women / 20 men, ages 18–70, diverse ethnicities, varied
│   │                                 backgrounds (cafe, office, outdoors, studio), centered face so it
│   │                                 crops perfectly into a circle. Include 5 "casual" non-face avatars
│   │                                 (pet, landscape, flower, car, beach) as avatar_person_36..40.
│   ├── avatar_group_01..12.webp      512×512, group photos/themes: family dinner, office team, football
│   │                                 team, college friends, travel crew, gym, book club, cousins, wedding
│   │                                 planning, neighbours, gaming squad, music band.
│   ├── avatar_channel_01..10.webp    512×512, FICTIONAL brand/channel logos on solid color: news, tech,
│   │                                 sports, cooking, travel, music, fitness, finance, movies, science.
│   │                                 Simple symbols, no readable words.
│   ├── avatar_business_01..06.webp   512×512, fictional small-business logos (bakery, salon, pharmacy,
│   │                                 courier, electronics shop, restaurant).
│   └── avatar_community_01..04.webp  512×512, community hub images (school parents, apartment society,
│                                     startup network, sports club).
│
├── 06_chat_media/  (photos inside conversations)
│   ├── media_photo_01..30.webp       Longest edge 1280px, WebP q75. Mix of portrait & landscape:
│   │                                 food, pets, travel, sunsets, selfies, birthday cake, receipts
│   │                                 (blurred, no readable text), whiteboard (blurred), shoes, books,
│   │                                 coffee, concert, beach, mountains, city night, baby feet, plants.
│   ├── media_video_thumb_01..10.webp 1280×720 WebP q75, frames that look like video stills (no play icon).
│   ├── media_link_preview_01..08.webp 1200×630 WebP q75, generic article/hero images (tech, recipe,
│   │                                 travel blog, sports, product, music, news, science).
│   ├── media_map_preview_01..04.webp 800×400 WebP, stylized fictional map tiles (streets/parks/water),
│   │                                 no labels, no real-map branding.
│   └── media_doc_thumb_01..04.webp   600×800 WebP, first-page previews of documents (abstract lines
│                                     instead of text: invoice, resume, report, presentation slide).
│
├── 07_status_stories/
│   ├── status_01..20.webp            1080×1920 WebP q72, vertical story photos: travel, food, gym,
│   │                                 pets, sunsets, events, nature, cityscapes, hobbies. Leave top and
│   │                                 bottom 15% visually calm (UI overlays sit there).
│   └── status_text_bg_01..08.webp    1080×1920 WebP q70, solid/gradient backgrounds for text-status.
│
├── 08_stickers/  (WhatsApp-sticker-style spec)
│   ├── pack_pulse_buddy/             Mascot: a cute rounded teal speech-bubble character with eyes.
│   │   ├── tray.png                  96×96 PNG transparent.
│   │   └── stk_01..16.webp           512×512 WebP transparent, each ≤ 100 KB, 8px white outline +
│   │                                 soft shadow, 16px transparent margin. Emotions: hello, thanks, LOL,
│   │                                 love, sad, angry, OK, wow, sleepy, party, thinking, facepalm,
│   │                                 good night, good morning, on my way, high five.
│   └── pack_cozy_cats/
│       ├── tray.png                  96×96
│       └── stk_01..16.webp           Same spec, a chubby cat character, same 16 emotions.
│
├── 09_misc/
│   ├── illus_e2e_lock_banner.webp    1080×480 transparent, small lock+bubbles (shown at top of chats).
│   ├── illus_qr_frame.webp           1080×1080 transparent, decorative frame for "My QR code" screen
│   │                                 (empty center area 600×600 for the QR drawn in code).
│   ├── illus_linked_devices.webp     1080×720 transparent, phone + laptop + tablet connected.
│   ├── illus_two_step.webp           1080×720 transparent, PIN dots + shield.
│   ├── illus_disappearing.webp       1080×720 transparent, timer clock + fading bubbles.
│   ├── illus_chat_lock.webp          1080×720 transparent, chat locked with fingerprint.
│   ├── illus_invite_friends.webp     1080×720 transparent, people sharing invite link.
│   ├── illus_storage_data.webp       1080×720 transparent, storage pie + files.
│   ├── illus_backup_cloud.webp       1080×720 transparent, cloud backup progress.
│   ├── illus_new_feature.webp        1080×720 transparent, sparkle/gift reveal ("what's new" sheet).
│   └── call_bg_blur_01..03.webp      1080×2340 WebP q65, dark blurred gradient backgrounds for the
│                                     in-call screen (teal/indigo/plum tones).
│
├── manifest.json                     Array of { "path", "width", "height", "bytes", "description" }
│                                     for EVERY file.
└── README.txt                        Palette, fonts suggestion, and any notes/deviations.

═══════════════════════════════════════
4. WHAT NOT TO MAKE
═══════════════════════════════════════
Do NOT generate small UI icons (send, mic, camera, attach, ticks, phone, video, search, menu, etc.).
The developer will use Material Symbols vector icons in code. Do NOT generate emoji sets (system emoji
is used). Do NOT include sound files.

═══════════════════════════════════════
5. WORKFLOW
═══════════════════════════════════════
Because there are many images, work in batches and keep the style identical across batches:
  Batch A: 01_brand + 02_onboarding
  Batch B: 03_empty_states + 09_misc
  Batch C: 04_wallpapers
  Batch D: 05_avatars
  Batch E: 06_chat_media
  Batch F: 07_status_stories
  Batch G: 08_stickers
After each batch: post-process with Python (resize/crop/convert/compress/strip metadata), show a
contact-sheet preview, and give me a download link for that batch's zip
(pulse_assets_batch_A.zip, etc.). At the end, merge everything into one pulse_assets.zip with the
exact folder structure above plus manifest.json and README.txt, and report the total size.

Before finishing, verify with Python and print a checklist:
  ✔ every listed file exists, ✔ names valid for Android, ✔ exact dimensions, ✔ transparency where
  required, ✔ sticker files ≤ 100 KB, ✔ total zip ≤ 15 MB.
Start with Batch A now.
```

## Notes for the developer

- Small UI icons are intentionally excluded: Material Symbols vectors (and custom vector drawables
  for ticks, status rings, etc.) are crisper and much smaller than generated PNGs.
- Mock data (names, messages, call logs) is generated in code, so it is not part of the image pack.

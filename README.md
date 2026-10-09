# Radha Music

Created, designed and developed by **Diwash Bhatta**.

## Version 1.4 — discovery, lyrics, private downloads and portable backups

### Online Music and Watch

- **Learns from listening:** Quick Picks blends your most-played artist, recent artists, and chosen preferences. Results alternate between sources and duplicate videos are removed. A listen qualifies after 30 seconds. This is an on-device taste profile with search-based discovery, not imported YouTube history or its private recommendation algorithm.
- **One load per session:** Music and Watch preload at launch. Returning to a tab reuses shared results, including in-flight requests. Recommendations refresh on a fresh session or preference change. Page caches live in memory; artwork requests are deduplicated. Playback buffers up to 90 seconds and waits for 5 seconds of buffered media after a stall. Server throttling or poor connections can still cause buffering.

- **Speed Dials:** online songs qualify after three listens of at least 30 seconds. Higher play counts come first; ties use the most recent listen. Three columns and two rows per swipe page, up to five pages (30 songs). Counts persist on the device and are included in backups. Per-song counting starts with this version; older artist totals cannot reconstruct past per-song plays.
- **Quick Picks:** five song rows per horizontal page, up to five pages. The app fetches another result page when needed; fewer cards appear if the source has fewer results. **Show more** opens the full list and further pagination.
- **Forgotten Favorites:** songs played at least twice and not heard for 14 days. No invented history or placeholder songs are inserted. Song/video cards and user playlists scroll horizontally.
- **Made for you:** mood cards open real searches using your listening preferences. These are discovery searches, not imported YouTube playlists or YouTube's recommendation model.
- **Add to library:** available in online item menus. Open **Saved → My library** for your saved songs and videos. Saving metadata does not itself download the media.
- Loading lists and buffering video use the coral Radha logo spinner. Video opens in portrait; the rotation control explicitly switches portrait/landscape. Decoder dimensions and fullscreen no longer override that choice. Original aspect ratio remains fitted to the screen.

### Full-screen lyrics

Open **Lyrics** in the music player. Synced lyrics highlight the current line and scroll with playback; tapping a timed line seeks to it. Manual scrolling pauses automatic scrolling briefly. Plain lyrics appear when timing is unavailable, and missing lyrics have an explicit empty state. Previous/next and pause remain available.

Lyrics are supplied by [LRCLIB](https://lrclib.net). Opening lyrics sends the song title, artist and duration to that service. Matching requires the same title/artist and, when known, a similar duration. Availability and metadata quality vary. Returned lyrics are cached privately, with automatic age and size limits; cached lyrics work offline.

### App-only downloads

Use **Download for offline** in an online song/video menu or the player download action. Downloads are stored under Radha's private `files/downloads` directory, not the public Downloads folder or MediaStore. Access them through **Downloads** in Online/Watch or Saved. Other normal apps cannot browse this private folder; uninstalling Radha removes it unless you first export a backup.

- Music downloads its available audio stream. Video downloads its video and audio streams and plays them together offline, or uses a combined stream when available. Files are not transcoded.
- Downloads run one at a time with a foreground notification showing progress and cancellation. Incomplete transfers are not marked downloaded; failure/cancellation offers retry. Automatic resume after process termination is not implemented.
- Playback prefers completed private downloads, including items launched from saved collections. Long press a completed download to remove it. Source availability, storage space, Android background limits and codecs still apply.
- Download activity does not enable background playback: listening with the screen off still requires **Background · On**.

### Settings-only export/import

Recent listening entries expire after 15 days, checked on app opening and listening/history access. Compact play totals (up to 300 tracks) and artist scores (up to 100 artists) remain for taste and Speed Dials. Saved items, playlists, custom artwork and intentional downloads are preserved. Forgotten Favorites can surface during the 14–15 day window before dated history expires. No cleanup notification is shown. Cached lyrics expire after 15 days and are limited to 100 files/8 MiB during cleanup.

**Settings → Export backup** saves a ZIP through Android's file picker. It includes listening preferences, per-song play counts, history, saved online library, favorites, playlists, custom artwork, cached lyrics and completed private downloads. There is no automatic cloud sync or account upload; the destination is chosen by you.

Transfer the ZIP to another phone and use **Settings → Import backup**. Import replaces Radha's saved preferences/collections after validating and staging the backup. Existing files are preserved for rollback if restoration fails. Finish or cancel active downloads before exporting/importing and keep the app open until complete.

Ordinary phone music/videos are not duplicated into the backup. Copy those separately, re-grant media permissions and re-select folders; local content URIs may need playlist entries re-added on the new phone. Online IDs and private downloads are portable. The current playback queue/session and temporary background-playback toggle are not backed up. Export/import controls appear only in Settings, not on the home page.

## Version 1.3.3 — Android 11 URL encoding compatibility

The reported `NoSuchMethodError: URLEncoder.encode(String, Charset)` happened before a YouTube search request could be sent. Android added that overload in API 33; the phone runs API 30. The previous default desugaring configuration did not rewrite that call.

- Use `desugar_jdk_libs_nio:2.1.5`, which includes backported URL encoding/decoding, so the extractor can run on supported older Android versions. No API key or change to phone data permissions is involved.
- A packaged-APK regression check rejects references to the unsupported platform Charset overloads and requires compiled, bundled implementations of both `j$.net.URLEncoder` and `j$.net.URLDecoder`. Desktop JVM tests alone cannot catch this packaging defect.
- Run after building: `java tools/VerifyUrlCodecCompat.java app/build/outputs/apk/debug/app-debug.apk`. The check intentionally fails against v1.3.2 with the exact reported method.
- Reference: [Android's NIO desugaring API support](https://developer.android.com/studio/write/java11-nio-support-table).

## Version 1.3.2 — visible online results and Android UI coverage

- Online now places Quick picks, loading status, results and connection errors above the decorative mix cards. Watch uses the same result panel.
- An explicit Search button submits online queries alongside the keyboard Search action.
- Search completion and timeout callbacks use the main Android looper, independent of whether the view was attached when the worker completed. Closing a screen cancels its callbacks.
- Android UI regression tests exercise request startup, visible failure actions, successful song rows, replacement search, retry recovery and empty results. These run with Robolectric, without installing an emulator.
- This addresses result/error visibility. A desktop API check or Robolectric test does not establish that the reported zero-data condition on the user's physical phone is resolved.

## Version 1.3.1 — online search recovery and diagnostics

- If music-specific search fails or returns no playable results, the app tries regular YouTube video search and plays those results as audio in Online. Watch remains video playback.
- Search results display their actual result count and whether the fallback was used. Pagination retains the selected search source.
- Errors now distinguish DNS lookup, timeout, TLS, blocked access, and Android component compatibility failures. **Error details → Copy error** provides a bounded diagnostic report with URLs and common credential fields redacted.
- A 90-second search deadline prevents an indefinite loading state. Android component linkage failures are handled instead of disappearing inside a background task.
- Android grants the declared INTERNET permission at installation; no runtime permission popup is expected. Device-specific Wi-Fi/mobile-data restrictions can still block an app.
- The original phone failure has not been reproduced on hardware. This update improves recovery and exposes the evidence needed if it persists; a successful desktop network check does not establish Android runtime behavior.

## Version 1.3 — online listening and the approved design

The approved Radha design is now implemented in the Android app: charcoal/ivory surfaces, coral accents, the coral **r** logo, landscape mix cards, a grooved black disc with album artwork in its center, and five evenly spaced destinations: **Online · Watch · Music · Videos · Saved**. The app follows the device's light/dark theme.

### Online music and Watch

- Search music by artist or title. Press the keyboard's Search button to submit. Search is separate from the local library.
- Online discovery starts with your chosen artists, languages and moods. **Listening preferences** changes these seeds or clears listening history.
- After 30 seconds of online playback, the artist contributes to the local taste profile. The most-listened artist helps seed future recommendations. These are simple Radha recommendations, not YouTube's personalized recommendation model.
- **Your daily mix**, **Late night**, and **Acoustic days** open searches tailored to those seeds. Mix artwork is decorative; it does not imply a preexisting playlist or fabricated track count.
- **Watch** searches online videos and plays the selected video with its original aspect ratio. Available progressive video streams up to 1080p are selected without transcoding; quality depends on what YouTube makes available.
- Search supports additional pages. Every result offers play-next, add-to-queue, favorites and playlists. Saved collections can mix local and online items.
- Online audio uses the existing player, notification/media controls, queue, repeat, shuffle and opt-in background mode. Tap **Background · Off** to enable screen-off/background listening.
- Stable YouTube IDs are saved in collections. Temporary playback links are resolved when Media3 loads an item, including queued tracks, and cached in memory for at most ten minutes. Retry refreshes the link after a playback error.
- Artwork has a bounded memory cache. Audio/video is streamed, not downloaded to the device for offline playback.
- Connection failures show retry options; playback failures also offer **Open YouTube**. Live streams, paid/restricted content, YouTube sign-in, account-library imports, synced lyrics, and automatic YouTube-history import are not implemented.

Online extraction uses **NewPipe Extractor v0.26.5**. It is unofficial and can stop working when the upstream service changes. Online browsing/playback sends requests to YouTube/Google media servers; listening history and preferences stay in app-private storage on your device. A Google account is not required or collected. The app is not affiliated with YouTube.

The app and full corresponding source are distributed under **GPL-3.0-or-later**. See [LICENSE](LICENSE) and [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md). License texts are also included in the APK assets.

## Previous update: version 1.2

- Songs are included regardless of extension, including M4A, OGG, AAC and OPUS. Only Android-identified recordings (Android 12+) and recognized recorder/call-recording/voice-note folders are filtered out. On older Android versions and manually added folders, filtering uses the folder name. No files are deleted.
- The local Videos, Music and Saved sections retain consistent navigation alignment within the new five-tab layout.
- Video transport icons sit at the bottom, leaving the center of the picture clear.
- Video orientation follows its displayed dimensions: portrait stays portrait and landscape stays landscape. Fullscreen hides controls and system bars while retaining the complete original aspect ratio. It never forces portrait video into landscape or zoom-crops the picture.
- Existing favorites, playlists, custom album images and unplayed-video history are retained when installed as an update with the same signing key.

## Library and player design

- Redesigned light/dark library, compact cards and bottom navigation.
- Video folders sort A–Z by displayed folder name, with an uppercase first letter and no storage path displayed.
- Red NEW badges identify unplayed videos. A red number on each folder counts them. Playback history persists after restarting the app; tracking begins with this version, since version 1.0 did not record it.
- Immersive fullscreen hides Android's system bars (swipe from an edge to reveal them temporarily). Video controls are transparent icons with fading overlays, and hide automatically while playing.
- The four-corner fullscreen button keeps the complete original frame and the video's natural orientation. Black bars can appear when the screen and video aspect ratios differ; the file is never stretched or cropped.
- Speed choices: 0.2×, 0.3×, 0.5×, 0.8×, 1×, 1.25×, 1.5×, 2× and 3×.
- All audio formats remain eligible for the Music list; recording detection is based on Android metadata and recognized recording folders, not file extension.
- Embedded album art appears in song rows and on a circular, grooved disc that rotates only during playback.
- A song's three-dot menu, or the music player's More menu, offers **Add / change album image**. Images are stored privately per song without modifying your audio file. The list menu can restore the original image.
- Swipe **left** on the music disc/background for the next track, **right** for the previous track.
- Creator credit appears in About and the music player.

An Android local and online music and video player. Supports Android 8.0 (API 26) and newer. Built with Java and AndroidX Media3. No account is required. Local media remains playable offline; online sections require Internet access.

## Install the APK

1. Transfer **Radha-Music-v1.4.apk** to your Android phone and open it from Files.
2. If asked, allow that file manager to install unknown apps, then install.
3. Open **Radha Music** and allow music/audio and video access. Choose all videos if you want your complete library.
4. Open Videos for source folders, or Music for songs with the newest additions first.

This is a development APK for testing on your phone. It has not been device-tested. Compilation and any completed automated checks are recorded separately in BUILD-NOTES.md.

## Controls and features

- **Videos:** grouped by their real source folder, such as Download, Camera, or WhatsApp Video. Files remain in place and play directly without re-encoding.
- **Music:** one list sorted by Android MediaStore date added, descending. Android does not expose the original download date for every file; imported folders use file modification dates.
- **Search:** title, artist, or folder. The library menu can refresh media, open permission settings, or add a folder using Android’s folder picker.
- **Playback:** previous/next, seek bar, backward/forward six seconds, pause, mute, speed, original video resolution display, audio language selection, embedded subtitles, and external SRT/VTT/SSA/ASS/TTML subtitle files.
- **Gestures:** vertically swipe the left side of the playback area for brightness; right side for device volume. Double tap left/right to go back/forward six seconds.
- **Screen lock:** Lock blocks on-screen playback gestures and controls until Unlock is tapped. It does not disable Android’s power or system navigation buttons.
- **Volume boost:** More → Volume & boost, from 100% to 200% amplitude (up to approximately +6 dB). Hardware/output support varies and boost can distort audio.
- **Background:** disabled when the playback service starts. Tap **Background · Off** to enable playback outside the app and with the screen off. This applies to music and video, with Android media notification controls. A new playback service starts with background playback off again.
- **Playback order:** More → Playback order / repeat → stop after current, auto-next, repeat item, or repeat queue. Playing from a video folder creates a queue from that folder. Auto-next is on by default.
- **Saved:** favorites and named playlists/albums for music, videos, or a mix. These are virtual collections; original files are not moved. Long press a saved collection to rename or delete it.
- **Play next:** open a media item’s three-dot menu while browsing. This inserts that item immediately after the current item. “Add to queue” appends it to the end. Browse the library while playback continues inside the app.
- **Theme:** follows Android’s light/dark setting. Playback uses a dark background for viewing comfort.

## Build the source

Open this folder in Android Studio, let Gradle sync, and choose **Build APK(s)**. Alternatively, install JDK 17 and Android SDK 35 / Build Tools 35.0.0, set `ANDROID_HOME`, then run:

```powershell
.\gradlew.bat assembleDebug
```

The output is `app/build/outputs/apk/debug/app-debug.apk`. Gradle 8.11.1, Android Gradle Plugin 8.9.1, Media3 1.8.0, NewPipe Extractor v0.26.5, and desugar_jdk_libs_nio 2.1.5. Extractor dependencies are pinned and resolved through JitPack. The Gradle wrapper downloads its distribution and dependencies on the first build. The working project uses `.local/debug.keystore` when present; otherwise Android's default debug key is used. Preserve the existing signing key for updates. The packaged source excludes local SDK paths, caches, and keys. A build from a different key cannot update an installed app signed with the previous key.

## Practical limits

- Codec support depends on Media3 and Android decoders. This build does not bundle FFmpeg/VLC software decoders; unusual audio formats such as DTS may not play on some phones. Available languages and subtitle tracks must exist in the media file.
- Android’s scoped storage and WhatsApp’s media visibility settings determine what MediaStore exposes. Use “Choose an additional folder” for accessible folders missing from the list. Android itself blocks selection of some locations.
- Favorites, collections, and selected folders persist. The current queue and playback position are kept only while the playback service lives.
- Files deleted or moved outside the app may leave stale playlist entries. Remove and re-add those entries.
- Redmi/MIUI battery restrictions can stop background services. If this happens after you enable background mode, allow background activity for Radha Music in the phone’s app/battery settings.
- Private offline downloads are supported for available YouTube streams. Instagram and TikTok integration is not included.

AndroidX Media3 is licensed under Apache 2.0: https://github.com/androidx/media

## Validation

Run `./gradlew testDebugUnitTest lintDebug assembleDebug` (Windows: `gradlew.bat`). Unit tests cover local music filtering, video orientation, seek and queue behavior, online ID validation, and taste ranking. Live online smoke checks require a network connection and are described in BUILD-NOTES.md. No emulator is required for building; playback, gestures and theme/layout behavior should be checked on a phone.

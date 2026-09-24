# Radha Music

Created, designed and developed by **Diwash Bhatta**.

## Version 1.1 update

- Redesigned light/dark library, compact cards and bottom navigation.
- Video folders sort A–Z by displayed folder name, with an uppercase first letter and no storage path displayed.
- Red NEW badges identify unplayed videos. A red number on each folder counts them. Playback history persists after restarting the app; tracking begins with this version, since version 1.0 did not record it.
- Immersive fullscreen hides Android's system bars (swipe from an edge to reveal them temporarily). Video controls are transparent icons with fading overlays, and hide automatically while playing.
- The four-corner fullscreen button enters landscape and fills the display; different video/screen aspect ratios can crop the edges. Tap it again for Fit to see the complete original frame. This does not convert or change the original file.
- Speed choices: 0.2×, 0.3×, 0.5×, 0.8×, 1×, 1.25×, 1.5×, 2× and 3×.
- Music hides M4A, OGG, AAC/ACC, AMR, OPUS and 3GA files and recording/voice-note folders. This is a library filter; no files are deleted. Songs in those formats are also hidden, as requested.
- Embedded album art appears in song rows and on a circular, grooved disc that rotates only during playback.
- A song's three-dot menu, or the music player's More menu, offers **Add / change album image**. Images are stored privately per song without modifying your audio file. The list menu can restore the original image.
- Swipe **left** on the music disc/background for the next track, **right** for the previous track.
- Creator credit appears in About and the music player.

An offline Android music and video player. Supports Android 8.0 (API 26) and newer. Built with Java and AndroidX Media3. No ads, account, server, or Internet permission.

## Install the APK

1. Transfer **Radha-Music.apk** to your Android phone and open it from Files.
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
- **Background:** disabled when the playback service starts. Tap **Background: off** to enable playback outside the app and with the screen off. This applies to music and video, with Android media notification controls. A new playback service starts with background playback off again.
- **Playback order:** More → Playback order / repeat → stop after current, auto-next, repeat item, or repeat queue. Playing from a video folder creates a queue from that folder. Auto-next is on by default.
- **Saved:** favorites and named playlists/albums for music, videos, or a mix. These are virtual collections; original files are not moved. Long press a saved collection to rename or delete it.
- **Play next:** open a media item’s three-dot menu while browsing. This inserts that item immediately after the current item. “Add to queue” appends it to the end. Browse the library while playback continues inside the app.
- **Theme:** follows Android’s light/dark setting. Playback uses a dark background for viewing comfort.

## Build the source

Open this folder in Android Studio, let Gradle sync, and choose **Build APK(s)**. Alternatively, install JDK 17 and Android SDK 35 / Build Tools 35.0.0, set `ANDROID_HOME`, then run:

```powershell
.\gradlew.bat assembleDebug
```

The output is `app/build/outputs/apk/debug/app-debug.apk`. Gradle 8.11.1, Android Gradle Plugin 8.9.1, Media3 1.8.0. The Gradle wrapper downloads its distribution and dependencies on the first build. The working project uses `.local/debug.keystore` when present; otherwise Android's default debug key is used. Preserve the existing signing key for updates. The packaged source excludes local SDK paths, caches, and keys. A build from a different key cannot update an installed app signed with the previous key.

## Practical limits

- Codec support depends on Media3 and Android decoders. This build does not bundle FFmpeg/VLC software decoders; unusual audio formats such as DTS may not play on some phones. Available languages and subtitle tracks must exist in the media file.
- Android’s scoped storage and WhatsApp’s media visibility settings determine what MediaStore exposes. Use “Choose an additional folder” for accessible folders missing from the list. Android itself blocks selection of some locations.
- Favorites, collections, and selected folders persist. The current queue and playback position are kept only while the playback service lives.
- Files deleted or moved outside the app may leave stale playlist entries. Remove and re-add those entries.
- Redmi/MIUI battery restrictions can stop background services. If this happens after you enable background mode, allow background activity for Radha Music in the phone’s app/battery settings.
- No YouTube, Instagram, or TikTok downloading is included.

AndroidX Media3 is licensed under Apache 2.0: https://github.com/androidx/media

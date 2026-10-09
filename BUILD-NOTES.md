# Radha Music 1.3.2

Created, designed and developed by **Diwash Bhatta**.

- Version code: 6. Application ID: `com.radha.music`.
- Minimum Android: 8.0 (API 26). Target/compile SDK: 35.
- Built October 9, 2026 with JDK 17, Gradle 8.11.1, Android Gradle Plugin 8.9.1 and Build Tools 35.0.0.
- `assembleDebug`, `testDebugUnitTest`, and `lintDebug` passed for the final source.
- Seventeen tests passed (including four Android UI tests), including nested DNS errors, compatibility errors, diagnostic redaction and timeouts, plus: music filtering, video orientation, seeking, queue insertion, boost limits, stable online IDs, URL validation and preference ranking.
- Lint: zero errors, 25 warnings (drawing allocations, translation, RTL, tooling constructors and legacy backup/gesture accessibility warnings). No lint baseline hides errors.
- Live desktop JVM checks using the same OnlineClient and dependencies: music search returned 27 results, next page 20, Watch search 20; selected audio and video URLs returned HTTP 206 with readable media bytes. These checks do not verify Android rendering, decoding, background lifecycle or audio/video synchronization on a phone.
- APK signature verification passed (v2). Certificate SHA-256 matches v1.2: `5d51b9604140d46dc9bddc2e6e5395408d88148a8e16205e014908deb8f30d60`.
- SHA-256 of `Radha-Music-v1.3.2.apk`: `DDC636623008712A3EC9EF112D51502A8F25EFE616ACF7F12C66BBD24507EB8F`.
- The original signing key is retained privately and excluded from the repository and source archive.
- No emulator was installed or used. Phone testing remains necessary for the final UI, gestures, streaming and screen-off playback.
- SDK, dependency caches and temporary builds were placed on D: because C: was almost full. No platform-tools or emulator are required for this build.

## Android UI regression verification

- Robolectric 4.14.1 runs Android API 28 framework tests at a 393 × 851 viewport, without an emulator. Four tests cover request startup through MainActivity, visible result/error rendering, new searches replacing results, retry recovery and explicit empty-result messaging.
- Restoring the old placement below decorative cards reproduced a failing assertion: `Retry search is outside the viewport`. The final layout passes the same test.
- The UI tests use controlled responses; separate live desktop network checks returned 27 music results, 20 next-page results and 20 Watch results, and read 1024 bytes each from real audio/video streams (HTTP 206).
- These tests do not reproduce the physical phone's zero-data counter or establish that every phone-specific connection problem is resolved. No claim of hardware testing is made.

## Recovery verification

- A desktop JVM test deliberately failed the music endpoint: regular YouTube fallback returned 20 results and its next page returned 20 results.
- The reported phone connection failure has not been reproduced on hardware. This build adds recovery and diagnostics, not a confirmed diagnosis of that device.
- If Online still fails, choose Error details, then Copy error, and share that report for diagnosis.

## Phone test checklist

1. Install v1.3.2 over your existing version; verify existing favorites/playlists remain.
2. Check the coral logo, all five tabs and both device light/dark modes.
3. Search Online, play a result, enable Background manually and lock the screen. With Background off, leaving the app should pause playback.
4. Queue another result with Play next; check automatic advancement, repeat and shuffle.
5. Watch an online video; check picture/sound synchronization, aspect ratio and fullscreen controls.
6. Save an online song to Favorites and a playlist, restart the app and play it again.
7. Change Listening preferences and listen for 30 seconds; return to Online to see the updated recommendation seed. YouTube history is not imported.
8. Confirm local folders, songs, audio/subtitle tracks, gestures and album artwork still work offline.
9. Disable Internet; local playback should work and online failures should offer Retry.

Online extraction is experimental and depends on YouTube availability. See README and THIRD-PARTY-NOTICES for supported behavior and licenses.

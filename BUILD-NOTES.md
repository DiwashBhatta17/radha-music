# Radha Music 1.4

Created, designed and developed by **Diwash Bhatta**.

- Version code 8; application ID `com.radha.music`; Android 8/API 26 minimum, target/compile SDK 35.
- Built October 9, 2026 with JDK 17, Gradle 8.11.1, AGP 8.9.1 and Android Build Tools 35.0.0.
- Final `assembleDebug`, `testDebugUnitTest`, `lintDebug`: successful. **33 tests passed**, zero failures. Lint: zero errors, 37 warnings (primarily existing allocation, accessibility, RTL, formatting and dependency-update suggestions).
- Packaged-DEX URL compatibility check passes: unsupported platform Charset URL codec references are absent; the replacement implementations are bundled. The Android 11 fix remains intact.
- Signature verification passed; certificate SHA-256 unchanged: `5d51b9604140d46dc9bddc2e6e5395408d88148a8e16205e014908deb8f30d60`.
- APK SHA-256: 5C6B003BA5F0EF4980311151FB8D21F134832A41BE370DDD2C070718A02343B0.
- Signing keys, generated builds and local SDK paths are excluded from GitHub and the source ZIP. Build caches and test dependencies remain on D:.

## Verification coverage

- Speed Dials: three-listen threshold, descending play counts, recency tie-breaks, 30-item/five-page cap, actual three-column/two-row Android layout, history persistence.
- Quick Picks: five items per page and five-page limit; result loading, empty/error states and retry behavior.
- Forgotten Favorites: repeat-listen and 14-day absence conditions.
- Player: portrait default, explicit rotate toggle, decoder dimensions and fullscreen preserving manual orientation.
- Lyrics: timestamp parsing, multiple timestamps, fractional seconds, offsets, current-line selection after seeks, title/artist/duration matching. A live LRCLIB endpoint check returned HTTP 200 with plain and synced lyrics for a supported track; lyrics text was not bundled.
- Downloads: HTTP transfer to private storage with byte-for-byte verification, incomplete-body rejection and partial-file cleanup; completed audio/video pairing and offline media-source selection.
- Backups: round-trip preferences, play counts, saved library, favorites, playlists and private media; path-traversal rejection and invalid metadata leaving original media untouched.
- Settings/library: import/export absent from the home screen and present in Settings; Add to library persists correctly.
- Existing local filtering, seeking, queue, volume, URL validation and online error checks remain passing.

Android UI tests run through Robolectric API 28 on the desktop. No emulator or physical device was used. These checks do not establish physical-phone decoding, actual lyric coverage for every song, background-download survival under every OEM policy, or exact visual rendering on the user's device.

## Phone verification

Install v1.4 over v1.3.3. Test a song three times for at least 30 seconds each; it should appear in Speed Dials. More-played tracks should rank first. Swipe the three-column grid and five-row Quick Picks.

Open Lyrics from a supported song. Check highlighting, seeking and previous/next. Play portrait and landscape videos, rotate explicitly and verify fullscreen keeps the chosen orientation.

Download a short song/video, then disable Internet and play it from Downloads. Check cancellation, retry and removal. Background listening remains explicitly opt-in.

Use Settings → Export backup, transfer the ZIP, then Settings → Import backup on another install. Keep Radha open during the operation and finish/cancel active downloads first. Ordinary phone media must be transferred separately; local content URI playlist entries may need re-adding after re-granting access. Online IDs, preferences and private downloads are portable.

Source: https://github.com/DiwashBhatta17/radha-music

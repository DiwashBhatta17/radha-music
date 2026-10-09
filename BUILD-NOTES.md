# Radha Music 1.5.1

Created, designed and developed by **Diwash Bhatta**.

- Version code 10; application ID `com.radha.music`; Android 8/API 26 minimum, target/compile SDK 35.
- Built October 9, 2026 with JDK 17, Gradle 8.11.1, AGP 8.9.1 and Android Build Tools 35.0.0.
- Final `assembleDebug`, `testDebugUnitTest`, `lintDebug`: successful. **54 tests passed**, zero failures. Lint: zero errors, 44 warnings (primarily existing allocation, accessibility, RTL, formatting and dependency-update suggestions).
- Packaged-DEX URL compatibility check passes: unsupported platform Charset URL codec references are absent; the replacement implementations are bundled. The Android 11 fix remains intact.
- Signature verification passed; certificate SHA-256 unchanged: `5d51b9604140d46dc9bddc2e6e5395408d88148a8e16205e014908deb8f30d60`.
- APK SHA-256: 12531F1D3549160EA6E9E81268987C6F6A523C174D4494E804A445E1C410243E.
- Signing keys, generated builds and local SDK paths are excluded from GitHub and the source ZIP. Build caches and test dependencies remain on D:.

## Verification coverage

- Startup test holds the initial requests beyond the old 15-second timeout and verifies the dialog waits for Home rendering. Lyrics availability tests cover missing/instrumental text, deduplicated checks and retryable failures.
- Queue ordering/removal, 3 equal-width Speed Dial columns within the viewport, centered video-buffering placement, fixed visible lyrics tracker, 21sp lyric text and preserved credits are covered. Native view renders for music, lyrics, queue and Home were inspected with test fixtures.

- Dark redesign: starter Speed Dials, Discover more, no Home playlist-management section, compact header under light device configuration, Saved filters, music-player controls and cancellation of pending playback intent. Native Android view rendering through Robolectric was inspected for Home, Saved and the player using test fixtures.
- Startup waits for shared launch requests and Home rendering, with an explicit Open local music escape instead of timed dismissal. Phone network latency and real provider buffering have not been benchmarked. Buffer thresholds are now 750 ms start / 1500 ms after stalls; speculative next-stream resolving is removed.

- Session cache: shared in-flight requests, page exit does not cancel shared fetching, revisit reuse and immediate cached access, failure retry and fresh-session reset.
- Recommendations: combines frequent/recent artists and chosen preferences, alternates sources, removes duplicates and tolerates a failed source.
- Retention: 15-day recent-history expiry preserves compact taste totals, saved songs and Speed Dials; expired lyrics cleanup preserves private downloads.

- Speed Dials: three-listen threshold, descending play counts, recency tie-breaks, 30-item/five-page cap, actual three-column/two-row Android layout, history persistence.
- Quick Picks: five items per page and five-page limit; result loading, empty/error states and retry behavior.
- Forgotten Favorites: repeat-listen and 14-day absence conditions.
- Player: portrait default, explicit rotate toggle, decoder dimensions and fullscreen preserving manual orientation.
- Lyrics: timestamp parsing, multiple timestamps, fractional seconds, offsets, current-line selection after seeks, title/artist/duration matching. A live LRCLIB endpoint check returned HTTP 200 with plain and synced lyrics for a supported track; lyrics text was not bundled.
- Downloads: HTTP transfer to private storage with byte-for-byte verification, incomplete-body rejection and partial-file cleanup; completed audio/video pairing and offline media-source selection.
- Backups: round-trip preferences, play counts, saved library, favorites, playlists and private media; path-traversal rejection and invalid metadata leaving original media untouched.
- Settings/library: import/export absent from the home screen and present in Settings; Add to library persists correctly.
- Existing local filtering, seeking, queue, volume, URL validation and online error checks remain passing.

Android UI tests run through Robolectric API 28 on the desktop. No emulator or physical device was used. These checks do not establish physical-phone decoding, actual lyric coverage for every song, background-download survival under every OEM policy, or exact physical-device visual rendering on the user's device.

## Phone verification

Install v1.5.1 over the existing Radha Music installation. Test a song three times for at least 30 seconds each; it should appear in Speed Dials. More-played tracks should rank first. Swipe the three-column grid and five-row Quick Picks.

Open Lyrics from a supported song. Check highlighting, seeking and previous/next. Play portrait and landscape videos, rotate explicitly and verify fullscreen keeps the chosen orientation.

Download a short song/video, then disable Internet and play it from Downloads. Check cancellation, retry and removal. Background listening remains explicitly opt-in.

Use Settings → Export backup, transfer the ZIP, then Settings → Import backup on another install. Keep Radha open during the operation and finish/cancel active downloads first. Ordinary phone media must be transferred separately; local content URI playlist entries may need re-adding after re-granting access. Online IDs, preferences and private downloads are portable.

Source: https://github.com/DiwashBhatta17/radha-music

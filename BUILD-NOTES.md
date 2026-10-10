# Radha Music 1.5.2

Created, designed and developed by **Diwash Bhatta**.

- Version code 11; application ID `com.radha.music`; Android 8/API 26 minimum, target/compile SDK 35.
- Built October 10, 2026 with JDK 17, Gradle 8.11.1, AGP 8.9.1 and Android Build Tools 35.0.0.
- Final `assembleDebug`, `testDebugUnitTest`, `lintDebug`: successful. **63 tests passed**, zero failures/errors. Lint: zero errors, 43 warnings.
- Packaged-DEX URL compatibility verification passed: unsupported Android Charset overloads are absent and replacement codecs are bundled.
- Signature verified; unchanged certificate SHA-256: `5d51b9604140d46dc9bddc2e6e5395408d88148a8e16205e014908deb8f30d60`.
- APK SHA-256: `57B1692974EB217EBA31E5FC3338B3DFAA8D7257717B075C4E14880A6B2A6852`.
- Signing keys, generated builds and local SDK paths are excluded from GitHub and the source ZIP. Build caches and test dependencies remain on D:.

## Changed behavior

- Search results and automatic playback queues are separate. Selecting a result or Home card starts the selected item; radio is fetched after playback reaches READY. Explicit playlists retain their order. Late responses for a different selection are ignored, and related entries append without overwriting manually inserted items.
- Music uses the provider RDAMVM song-radio playlist, not its generic related-video list. Broad mood searches retain their search context. Radio filters alternate versions of the selected song, duplicate titles, covers, karaoke and reactions, and limits repeated artists. Artist search is a fallback when the provider mix is unavailable. Search results themselves remain unfiltered.
- For you blends up to three recent learned radios with artist, mood-query and preference recommendations. Meaningful 30-second listens teach this compact on-device taste profile. Returning Home can blend it into the existing cached list; navigation does not trigger a new fetch. This is provider-assisted similarity, not guaranteed mood classification or YouTube account synchronization.
- Speed Dials records only explicitly searched selections that reach their natural end with at least 95% unique playback coverage. One completion qualifies; higher completion counts rank first. Home, discovery chips, Up next, skips and seeking to the end do not qualify. Old play totals are not migrated because they cannot establish search origin. Starter suggestions are shown only while there are no qualifying songs.
- Favorites, Forgotten Favorites, Listen again, artist recommendations, party/acoustic/live matches and exploration rails are conditional on available songs. No empty headings. Three-column Speed Dials and approved dark player/lyrics styling are retained.
- Startup no longer has an Open local music link. Branding, the progress animation and creator credit remain. It waits for launch requests and Home rendering; a 90-second failure bound prevents a stalled provider from trapping users indefinitely.
- Background playback remains opt-in. Local media, downloads, lyrics controls, credit and Android 11 URL compatibility are preserved.

## Verification coverage

- Pure tests cover radio diversity, cover/version filtering without changing search results, party intent, natural completion, seek-to-end rejection, unique playback coverage and pause/resume.
- Android tests cover search/Home/browse origin, first-completion persistence, non-qualifying legacy plays, taste persistence/reset, conditional sections, three equal-width columns, the removed startup link, queue order, lyrics availability and seek controls, dark UI, portable backups and existing local/download behavior.
- A live desktop check searched Die with a Smile (20 results) and Indian party songs (30 results), producing 20 different-song queue entries for each. The music-radio check returned Night Changes, Perfect, Attention, Senorita and Let Me Love You after Die with a Smile. Party results included The Disco Song, Tip Tip Barsa Paani, Dilbar Dilbar and Laila Main Laila. Provider ordering and relevance can vary; these samples are not hard-coded.
- The first live test exposed unrelated generic video recommendations. Music radio replaced that source before delivery. Native Home rendering was inspected using fixture artwork.

Android UI tests run through Robolectric API 28 on the desktop. No emulator or physical device was used. Live checks establish desktop provider responses, not phone-specific decoding, network latency or absence of buffering.

## Phone verification

Install v1.5.2 over the existing app. Search a song and let it finish normally: it should appear in Speed Dials. Playing from Home or Up next, or skipping to the end, should not add a song there. Search Die with a Smile and Indian party songs; inspect the different-song Up next queues and the next For you mix.

Add/remove a favorite on Home and verify its section appears/disappears. Check the startup screen, three-column grid, swipe rows, lyrics tracker, portrait/landscape video and manual background playback.

Use Settings -> Export backup and Import backup to transfer preferences, saved library and private downloads. Ordinary phone media must be transferred separately; local content URI entries may need re-adding after permission changes.

Source: https://github.com/DiwashBhatta17/radha-music
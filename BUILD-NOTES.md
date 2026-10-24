# Radha Music 1.6.0

Created, designed and developed by **Diwash Bhatta**.

- Version code 12; same application ID `com.radha.music` and signing certificate as previous releases. Android 8/API 26 minimum, compile/target SDK 35.
- Built October 24, 2026 with JDK 17, Gradle 8.11.1 and AGP 8.9.1. Added Media3 DASH 1.8.0 and AndroidX Core 1.15.0 for adaptive playback and the installation FileProvider.
- Local assembleDebug, testDebugUnitTest and lintDebug passed: 70 tests, zero failures/errors; lint zero errors and 45 warnings.
- APK signing and bundled Android 11 URL codec checks passed. Certificate SHA-256: `5d51b9604140d46dc9bddc2e6e5395408d88148a8e16205e014908deb8f30d60`.
- Locally verified APK SHA-256: `3C5A85201EEAEC91EECD3999755801BECC625198B7510E3603A1EC71E8BB7BEE`. GitHub Actions rebuilds the APK; its release asset carries its own verified digest.

## Playback and search changes

Online video now resolves an in-memory DASH manifest over the provider's original indexed AVC streams. Auto uses Media3 bandwidth adaptation with a conservative initial 1 Mbps estimate. The quality menu lists only supported resolutions actually available for the video; manual choices set a video-only track override and retain playback position and audio/subtitle selection. New videos default to Auto. Unsupported adaptive sources fall back to their original progressive source; downloaded/local files remain original files.

The player buffers upcoming media in RAM with a 48 MiB target and up to 90 seconds of media. Startup buffering is 1.5 seconds, rebuffering 3 seconds. Actual RAM use and buffered duration depend on bitrate, segment size and renderer allocations. Nothing here adds permanent media caching. Networks slower than the lowest stream or interrupted connections can still cause stalls.

Search metadata now includes duration and available view counts; unknown counts are omitted. Counts survive saved metadata and preference-radio storage. Existing dark UI, creator credit, portrait/fullscreen behavior and opt-in background playback remain.

A live desktop test retrieved a video with a 635-second duration and a reported 23,464,515 views. Its indexed streams offered 144p, 240p, 360p, 480p, 720p and 1080p. A live byte-range index request returned HTTP 206 and 1,508 bytes. Results are provider samples, not hard-coded data.

## Update delivery

The app checks the latest stable GitHub Release after startup and from Settings -> Check for updates. On approval, Android DownloadManager fetches `Radha-Music.apk`, including while the app is backgrounded. Returning to the app offers installation after checking SHA-256, file size, package ID, signing certificate and a higher installed version code. Android requires installation confirmation and may ask the user to allow installs from this source. The downloaded installer is removed once the new version is detected.

A GitHub Actions workflow builds, tests, signs and publishes app/build updates on main. Both versionCode and versionName must increase for a new release; existing release tags are skipped. The existing signing key is stored in an encrypted GitHub repository secret, never public Git. Workflow builds use that key so installs remain compatible.

Users must install v1.6.0 once to gain the updater. Older versions cannot discover updates themselves. There is no silent or remotely forced APK installation.

## Validation limits

Automated tests run with Robolectric API 28 and unit-level checks; no emulator or physical phone was used. They cover DASH parsing, resolutions and byte ranges, video-only manual/Auto parameters, update version ordering, trusted assets and checksums, draft/prerelease rejection, search metadata, and existing app behavior. Live desktop extraction/index checks do not establish phone-specific decoding, measured buffering improvement or the Android install-permission experience on every device.

## Phone checks

Install over the previous app. Play an online video, open Video quality and confirm Auto plus available resolutions. Switch between 360p/720p/1080p while watching the position and sound. Use a slower connection to exercise Auto; check portrait video too. Search and verify durations and view counts where supplied.

Open Settings -> Check for updates. The first installed v1.6.0 should report current once its release is published. Later higher-version releases should offer download, then verified installation. Confirm existing library and favorites remain after updating.
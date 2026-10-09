# Radha Music 1.3.3

Created, designed and developed by **Diwash Bhatta**.

- Version code: 7. Application ID: `com.radha.music`.
- Minimum Android: 8.0 (API 26). Target/compile SDK: 35.
- Built October 9, 2026 with JDK 17, Gradle 8.11.1, Android Gradle Plugin 8.9.1 and Build Tools 35.0.0.
- `assembleDebug`, `testDebugUnitTest` and `lintDebug` passed. Seventeen tests passed; lint has zero errors and 28 warnings.
- APK signature verification passed. Certificate SHA-256 matches existing releases: `5d51b9604140d46dc9bddc2e6e5395408d88148a8e16205e014908deb8f30d60`.
- APK SHA-256: 34DDBAC521A5213592049F025684F3A2A5D9347783806D2E598813DD54C7B94E.
- The signing key remains private and is excluded from source archives and GitHub.
- All new build dependencies and temporary builds are on D:. No emulator was installed or run.

## Confirmed regression and fix

The phone reported Android 11/API 30 and `NoSuchMethodError` for `java.net.URLEncoder.encode(String, Charset)`. This method was added to Android in API 33. Failure during search construction explains why no search data was transferred.

The default `desugar_jdk_libs` configuration did not backport URL codecs. Version 1.3.3 uses the full `desugar_jdk_libs_nio:2.1.5` configuration, covering both encoding and decoding.

`java tools/VerifyUrlCodecCompat.java <APK>` inspects every packaged DEX method reference and implementation. Against v1.3.2 it fails with the exact unsupported method from the phone report. Against v1.3.3 it confirms:

- Neither unsupported platform Charset overload is referenced.
- Both replacement `j$.net.URLEncoder.encode` and `j$.net.URLDecoder.decode` methods have bundled code implementations.

This APK-level check covers the packaging defect missed by desktop JVM/Robolectric tests. The 17 existing unit/UI tests also pass. No physical-phone runtime test is claimed; other upstream streaming issues remain possible.

## Install and verify

Install v1.3.3 over the existing app, preserving playlists and favorites. Open Online and search for an artist or song. Check Watch and local playback. If another error appears, copy its Error details; it should no longer be the platform URL encoding/decoding Charset error.

Source: https://github.com/DiwashBhatta17/radha-music
Android compatibility reference: https://developer.android.com/studio/write/java11-nio-support-table

# Radha Music 1.2.0

Created, designed and developed by **Diwash Bhatta**.

- Version code: 3
- Application ID: `com.radha.music`
- Minimum Android version: 8.0 (API 26)
- Target/compile SDK: 35
- Build: `assembleDebug` completed successfully on September 24, 2026 using JDK 17.
- Six JUnit regression checks passed for audio filtering, portrait/landscape classification, seeking, queue insertion and gain limits. These pure Java tests were run directly with JUnit because Gradle's offline cache lacked the JUnit dependency metadata.
- APK signing verification: passed (APK Signature Scheme v2).
- SHA-256 of `Radha-Music-v1.2.apk`: `210DEAF7CAEFB365F9E4008CA843BF7C9471D8ED24D4F3224C4FA637C69C30DC`
- The existing workspace signing key was retained for this build.
- No emulator was set up or run for this update, per the user's instruction. Runtime/UI testing is left to the user's phone.

Install the APK as an update to your existing app when its signature matches. If Android reports a signature mismatch, preserve your installed app and report it so the original signing key can be used; uninstalling would remove its app data.

See README.md for the new controls and features. Unplayed-video tracking starts with this version because the earlier version did not save playback history.

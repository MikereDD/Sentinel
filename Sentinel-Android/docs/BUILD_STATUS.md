# Build status — 1.5.0-dev.2

Source packaging and static resource checks completed in the ChatGPT build environment.

## Completed

- Version: `1.5.0-dev.2` (`versionCode 7`)
- Final six-node Sentinel identity wired into adaptive launcher foreground.
- Android 13+ monochrome themed icon updated to the same geometry.
- Full-detail Sentinel Android artwork wired into About.
- Windows/Linux family artwork preserved under `branding/` for their upcoming repos.
- Android resource XML parsed successfully.
- Revival UI/behavior/database edits from dev.1 retained.

## APK not produced here

This environment does not contain an Android SDK. The recovered project also omitted
`gradle/wrapper/gradle-wrapper.jar`, so Gradle cannot bootstrap here. Open the project in
Android Studio on the development machine, restore/regenerate the Gradle wrapper if needed,
then build the Debug variant.

The original recovered POSIX wrapper script also contained malformed JVM-option quoting;
that text issue has been corrected in this package. The wrapper JAR itself still needs to be
regenerated from a trusted Gradle installation rather than copied from an arbitrary source.

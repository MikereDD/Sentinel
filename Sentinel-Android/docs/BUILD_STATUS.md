# Build status — 1.5.0-dev.4

Sentinel Android is prepared for the `1.5.0-dev.4` local build and phone-test cycle.

## Current baseline

- Version: `1.5.0-dev.4` (`versionCode 9`)
- Canonical six-node Sentinel branding remains in place.
- Gateway presentation now treats the known router/gateway role as authoritative.
- Activity wording uses calmer state-change terminology.
- Monitors expose their direct check type (`Reachability` or `TCP :port`) and last-check time.
- No Room migration or other database schema change is required for dev.4.
- No Sentinel Protocol change is required for dev.4.

## Build environment

The Gradle wrapper JAR is restored and tracked in the monorepo. The previous
`1.5.0-dev.3` source was successfully built on the development machine with the
Android Studio JBR / Java 21 environment.

`1.5.0-dev.4` still requires a fresh local build and phone test before it should
be treated as verified.

Suggested verification:

```powershell
Set-Location .\Sentinel-Android
.\gradlew.bat clean assembleDebug
```

Then install/test the debug APK and verify:

- the gateway does not display `WEAK ID`;
- ordinary weakly identified devices still can;
- Activity wording is clear and restrained;
- reachability monitors show `Reachability`;
- TCP monitors show `TCP :<port>`;
- checked monitors show a `Last checked` timestamp.

# Sentinel 1.5.0-dev.2 test plan

## Build/install

1. Open `Sentinel-Android` in Android Studio.
2. Allow Gradle sync to complete. The recovered project does not include `gradle-wrapper.jar`; Android Studio can regenerate/use a local Gradle installation if required.
3. Build the Debug variant.
4. Install on a stock Android device.

If an older Sentinel build was signed with a different debug key, Android will reject an in-place update. Uninstall the old build only if needed. If the signing key matches, prefer an upgrade install so the Room 4→5 migration is exercised.

## Visual QA

- New six-node Sentinel launcher mark is visible and centered.
- Android 13+ themed icon uses the monochrome Sentinel geometry.
- About shows the full-detail Sentinel radar/network artwork.
- Bottom navigation reads `Status`, `Activity`, `Monitors`, `About`.
- No old robot branding remains.
- Status uses cyan for Sentinel/scanning, green for healthy, amber for attention, red for failures.
- Device summary reads `N active • M known`, not `N/M online`.

## Scan behavior

### First scan
- On a fresh database, the first scan establishes a baseline instead of creating a flood of `New device` events.
- Internet and gateway health render correctly.

### Discovery hysteresis
- A known ordinary device missed for one scan must NOT immediately become offline.
- If still absent on the second consecutive scan, it may transition offline and create one event.
- When rediscovered, it should return online and reset its missed-scan count.

### Monitors
- Add a host-only monitor and confirm it performs a direct reachability check.
- Add a host + TCP port monitor and confirm service state is reported separately from fuzzy discovery.
- Confirm monitor creation happens in the focused add dialog.

## Activity

- Verify All / Alerts / Devices / Infrastructure filters.
- Confirm routine scan noise is substantially reduced compared with 1.4.0.
- Clear Activity and confirm the list updates.

## Persistence/migration

When upgrading from 1.4.0 with a compatible signing key:
- Existing known devices remain.
- Rooms/custom names/types remain.
- Existing monitor targets remain.
- Existing Activity history remains.
- Database migration 4→5 completes without destructive reset.

## Things to report back

For every issue, capture:
- screenshot
- what screen/action triggered it
- whether it reproduces on a second attempt
- device/Android version
- relevant IP/hostname only if useful (redact anything you do not want in chat)

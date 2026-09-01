# Sentinel revival notes

`1.5.0-dev.2` is the first modernization pass over the recovered 1.4.0 source.

## Implemented in this pass

- New visual system: Sentinel cyan is brand identity; green/amber/red are semantic state colors.
- New lens/network mark used in-app and as the adaptive launcher icon.
- Android 13+ monochrome launcher artwork for themed icons.
- Status screen reorganized as a quieter network console.
- Device summary now reports `active • known` rather than implying every known device should be online.
- Activity screen replaces Timeline and adds All / Alerts / Devices / Infrastructure filters.
- Monitors replaces Watched in the UI; monitor creation moves into a focused dialog.
- First scan establishes the network baseline without flooding Activity with “new device” events.
- Two consecutive discovery misses are required before an ordinary device is marked offline.
- Direct monitors remain authoritative and do not use the discovery miss grace period.
- Room database schema bumped 4 → 5 with `missedScans`.
- Room schema export enabled for future migration testing.
- Android backup disabled to keep the local network database out of cloud-backup eligibility.

## Next development pass

1. Build and visual-QA on a real device after importing into Android Studio.
2. Commit the generated Room v5 schema from `app/schemas/`.
3. Add migration tests and classifier/identity unit tests.
4. Add a device-details screen instead of making edit controls the primary tap destination.
5. Add search/filtering for larger learned-device lists.
6. Expand Monitors into typed checks (reachability, TCP, HTTP/HTTPS) before background scheduling.
7. Add opt-in WorkManager background checks only after notification semantics are settled.

## Design principle

Sentinel should be quiet when the network is healthy. It should surface a change because the change matters, not because the scanner happened to miss a sleepy device once.

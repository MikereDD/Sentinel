# Changelog

## 1.5.0-dev.3 — phone-test polish

- Enlarged the Android adaptive launcher mark for better mask fill.
- Replaced the baked About-screen icon image with the native Sentinel mark.
- Removed remaining lavender Material selection styling from Activity filters.
- Fixed Activity empty-state text clipping and centered its layout.
- Added friendlier primary names for the gateway and generated device hostnames.
- Moved weak device identity into a dedicated amber `WEAK ID` badge.
- Bumped Android test version to `1.5.0-dev.3` (`versionCode` 8).

## 1.5.0-dev.2 — Sentinel revival

- Reworked the visual system around a quiet network-operations console.
- Replaced the robot launcher icon with the final Sentinel radar/network-node identity.
- Added full-detail Sentinel branding artwork plus a simplified six-node adaptive/themed launcher mark.
- Added Android themed-icon monochrome artwork.
- Renamed Timeline to Activity and Watched to Monitors in navigation.
- Added Activity filters for alerts, devices, and infrastructure.
- Moved monitor creation into a focused add dialog.
- Separated Sentinel brand cyan from healthy green, warning amber, and failure red.
- Changed device summary copy from `online/known` to `active • known`.
- Added two-scan discovery hysteresis before ordinary devices are marked offline.
- Added Room schema export and migration 4→5 for discovery miss tracking.
- Disabled Android application backup so local network history is not eligible for cloud backup.

All notable changes to Sentinel are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.4.0] - 2026-06-09

### Added
- **Rooms** — assign each device a room/location (presets like Living room, Bedroom,
  Patio, plus free-text) from the edit sheet. The Status device list groups by room, with
  an Unassigned section at the end.

### Changed
- The device edit sheet now has a Room field with one-tap suggestion chips (presets plus
  rooms you've already used).
- `versionName` bumped to 1.4.0 (`versionCode` 5).

### Database
- Schema migrated v3 → v4 (additive): `known_devices` gains `room`.

## [1.3.0] - 2026-06-09

### Added
- **About tab** — app identity, live version, how-it-works, privacy summary, and a tip,
  reachable from the bottom navigation.
- New **robot-sentinel launcher icon** — a watchful robot head with a single lens eye,
  replacing the shield mark.

### Changed
- Visual polish: "What changed" cards are now colour-coded by event type with a status dot,
  the status card has a softer rounded shape and a subtle border, and corner radii are
  unified across cards.
- Enabled `buildConfig` so the About screen reads the real version name.
- `versionName` bumped to 1.3.0 (`versionCode` 4).

## [1.2.0] - 2026-06-09

### Added
- **Manual device overrides** — tap any device to set a custom name and pin its type.
  Overrides persist and are never touched by scans (a *(set)* marker shows pinned types).
- `ReverseDnsResolver` — best-effort reverse-DNS (PTR) lookups to recover DHCP-registered
  names (e.g. `Blink-Mini-xxxx`, `DESKTOP-…`) for hosts with no mDNS/SSDP.
- Camera hostname keywords (blink, doorbell, camera, reolink, arlo, wyze, ring) so named
  cameras classify as Camera automatically.

### Changed
- `DeviceClassifier` now picks the **most confident** signal across SSDP / ports / mDNS /
  hostname instead of a fixed priority order. Fixes Macs that advertise AirPlay being
  mislabelled as TVs — a confident "Mac mini" hostname now wins over the AirPlay→TV hint.
- Device names that were just an IP are auto-upgraded when a real name later appears.
- Long device names are truncated with an ellipsis in the list.
- `versionName` bumped to 1.2.0 (`versionCode` 3).

### Database
- Schema migrated v2 → v3 (additive): `known_devices` gains `customName` and `userType`.

## [1.1.0] - 2026-06-09

### Added
- Device typing: infers a device **category** (and often brand/model) from MAC-free
  signals, fused strongest-first by `DeviceClassifier`.
- `SsdpDiscovery` — SSDP/UPnP M-SEARCH to 239.255.255.250:1900 plus device-description
  XML parsing for `manufacturer` / `modelName` / `friendlyName`.
- `PortScanner` — signature-port probing limited to already-discovered live hosts.
- `DeviceClassifier` — combines SSDP, open ports, mDNS service types, and hostname
  keywords into a `DeviceProfile` (type, vendor, model, 0–100 confidence).
- Per-type icons and a brand/model line in the Status device list.
- Expanded mDNS service-type coverage (Android TV remote, Spotify Connect, Sonos,
  Fire TV, HomeKit, IPP, Apple mobile, and more).

### Changed
- "New device found" events now include the inferred type/brand,
  e.g. *"Samsung TV — 192.168.4.30"*.
- SSDP friendly-names now feed both the display name and the identity key, firming up
  some devices that previously showed as *weak id*.
- `versionName` bumped to 1.1.0 (`versionCode` 2).

### Database
- Schema migrated v1 → v2 (additive): `known_devices` gains `deviceType`, `vendor`,
  `model`, and `typeConfidence`. Existing watched targets and timeline are preserved.

## [1.0.0] - 2026-06-09

### Added
- Initial release.
- Two-subsystem design: best-effort device discovery (bounded subnet ping sweep +
  mDNS/NSD, best-effort ARP) and user-pinned watched infrastructure checked by direct
  reachability.
- Diff engine that reports only what changed — new / offline / returned devices,
  internet outage & restored, router/gateway up & down, watched host up & down — with a
  chronological timeline.
- Room persistence for known devices, watched targets, events, and per-network state.
- Jetpack Compose (Material 3) UI with Status / Timeline / Watched tabs.
- No runtime permissions — only normal install-time permissions.

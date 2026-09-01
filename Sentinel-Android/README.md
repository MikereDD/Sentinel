# Sentinel

A local-only Android app that scans your home Wi-Fi network and reports **only what
changed** since the last scan. When nothing differs it simply says *Status: Normal —
Nothing changed*. When something does, it surfaces the event (a new device appeared,
a known device went offline or came back, the internet dropped, the router became
unreachable, a watched host went down) and logs it to a timeline.

Package: `com.typezero.sentinel` · Current source: **v1.5.0-dev.2** · Latest stable: **v1.4.0**

> Full version history lives in [CHANGELOG.md](CHANGELOG.md).

## What's new in 1.5.0-dev.2

This is the first revival pass over the recovered source. It keeps Sentinel small and local,
but gives the app a more deliberate network-console identity and makes discovery less noisy.

- New Sentinel lens/network visual identity and adaptive/themed launcher icon.
- Calmer status dashboard with separate brand, healthy, warning, and failure colors.
- `Activity` replaces Timeline and adds useful event filters.
- `Monitors` replaces Watched and uses a focused add-monitor flow.
- Ordinary discovered devices must be missed twice before an offline event is emitted.
- The first scan establishes a baseline instead of reporting every device as newly discovered.
- Android backup is disabled so Sentinel's local network history is not cloud-backup eligible.

See the [changelog](CHANGELOG.md) for the complete list.

## Design: two subsystems

Sentinel deliberately splits its work into two independent subsystems so that the events
you care about most stay reliable even where device identity is fuzzy.

**1. Discovered devices** — generic discovery of everything on the subnet via a bounded
ping sweep plus mDNS/NSD. Identity is resolved MAC-first, falling back to mDNS hostname,
then a service fingerprint, then IP. Only MAC-based identities are marked *strong*; weak
identities can occasionally produce a false "new device" when an IP changes. This is the
inherently fuzzy half.

**2. Watched infrastructure** — hosts you pin yourself (the Pi/Arakiel, the Eero gateway,
Home Assistant, etc.). These are checked by direct reachability (ping, or a TCP connect to
a port like `8123` for Home Assistant), so their up/down events do **not** depend on
discovery and stay rock-solid and MAC-independent.

Internet reachability and the default-gateway state are tracked per network and diffed
across scans to produce outage/restored events.

## Device typing

Since most hosts expose no MAC and no mDNS, Sentinel infers a device **category** (and
often brand/model) from MAC-free signals, fused strongest-first by `DeviceClassifier`:

- **SSDP/UPnP** (`SsdpDiscovery`) — M-SEARCH to 239.255.255.250:1900, then fetch the
  device-description XML for `manufacturer` / `modelName` / `friendlyName`. Best signal
  for TVs, consoles, NAS, media renderers, and routers.
- **Open-port signatures** (`PortScanner` over live hosts only) — e.g. 62078→iOS,
  8009→Chromecast/Google TV, 3389→Windows, 631/9100→printer, 32400→Plex, 22→Linux box.
- **mDNS service types** — `_googlecast`, `_androidtvremote2`, `_airplay`, `_printer`,
  `_hap`, `_workstation`, etc.
- **Hostname keywords** — iPhone / DESKTOP- / raspberrypi / BRAVIA / ESP_ as a tie-break.

This yields a device **category**, not a guaranteed per-host OS. Exact OS fingerprinting
(nmap-style TCP/IP stack probing) needs raw sockets/root, which an unprivileged Android
app cannot use — but category + brand is usually enough to know what something is.

Some devices expose nothing at all — cloud-only battery gadgets like Blink cameras and
doorbells advertise no local services, and Android won't surface their MAC for an OUI
lookup, so they stay generic. For those, **tap the device to set a name and pin its type**;
the override persists and is never overwritten by later scans.

## Why no MAC scanning of neighbours?

Android 10+ blocks non-system apps from reading `/proc/net/arp`, so neighbour MAC reads
are unreliable. `ArpReader` tries anyway and degrades gracefully to an empty map, at which
point identity falls back to the fingerprint path. Quarantining this fuzziness to
subsystem 1 keeps subsystem 2 dependable.

## Permissions

Sentinel requests **only normal, install-time permissions** — there are no runtime
permission dialogs:

- `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`
- `CHANGE_WIFI_MULTICAST_STATE` (for the mDNS and SSDP multicast locks)

The current network is resolved from `ConnectivityManager` link properties, which needs no
location permission. Known devices are scoped by gateway/subnet (`networkKey`) so a foreign
Wi-Fi never pollutes your home device list.

## Tech stack

- Kotlin 2.0.21, Jetpack Compose (Material 3), Compose BOM 2024.10.01
- Room 2.6.1 (via KSP) for known devices, watched targets, events, and per-network state
- Coroutines for the concurrent sweep / mDNS / SSDP / port / reachability passes
- Manual DI (no Hilt); `SentinelApp` builds the database, scanner, and repository
- minSdk 26, targetSdk/compileSdk 35, Java 17, AGP 8.7.2, Gradle 8.9

## Project layout

```
app/src/main/java/com/typezero/sentinel/
├── MainActivity.kt          bottom-nav scaffold: Status / Timeline / Watched
├── SentinelApp.kt           Application + manual DI container
├── data/
│   ├── db/                  Room entities, DAOs, database (+ migrations)
│   ├── model/               scan models, enums, DeviceType/DeviceProfile
│   └── repository/          SentinelRepository — the diff engine + persistence
├── scan/                    NetworkUtils, SubnetSweeper, ArpReader, MdnsDiscovery,
│                            SsdpDiscovery, PortScanner, ReverseDnsResolver,
│                            DeviceClassifier, ReachabilityChecker, DeviceIdentity,
│                            NetworkScanner
└── ui/                      theme, MainViewModel, screens/ (Status, Timeline,
                             Watched, About)
```

## Building

Open the project root in Android Studio and let it sync. Because the Gradle wrapper JAR is a
binary it is **not** included here — Android Studio will provision the wrapper on first sync.
If you prefer the command line, run `gradle wrapper` once (with a local Gradle 8.9) to
generate `gradle/wrapper/gradle-wrapper.jar`, then `./gradlew assembleDebug`.

## Status & limitations

- Sweeps are capped to a /24 around the device to stay fast and bounded.
- mDNS and SSDP discovery are best-effort; non-advertising devices won't be enriched.
- Scans are manual (tap *Scan now*) — no background service or notifications yet.
- Device typing returns a category, not a guaranteed per-host OS (see above).

Not yet implemented: background/periodic scanning, push notifications, deeper Home
Assistant integration, and service-level health checks.


## Revival testing

See `docs/TEST_PLAN.md` for the 1.5.0-dev.2 device test checklist.

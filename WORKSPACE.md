# Sentinel Workspace

Sentinel is a local-first network awareness and monitoring family.

## Repository family

- `Sentinel-Android`
  - Primary dashboard/controller.
  - Local Android network discovery.
  - Device inventory, activity history, monitors, alerts, and future agent/sensor integration.
  - Must remain usable on stock Android without root.

- `Sentinel-Agent-Linux`
  - Linux-native telemetry agent.
  - Host identity, capabilities, listeners, active connections, process/service attribution, and service health.
  - First target host: a controlled Linux system used for real-world development/testing.

- `Sentinel-Agent-Windows`
  - Windows-native telemetry agent.
  - Same logical contract as the Linux agent, implemented with Windows-native facilities.

- `Sentinel-Protocol`
  - Platform-neutral contract shared by Sentinel components.
  - Defines envelopes, capabilities, hosts, services, connections, events, authentication expectations, versioning, and compatibility.

## Core principles

1. Discover what exists.
2. Observe what changes.
3. Instrument what we control.
4. Distinguish **discovered**, **observed**, and **directly known/instrumented** facts.
5. Monitor deeply; report sparingly.
6. Observe first, understand second, act deliberately.
7. Do not require Android root.
8. Connection metadata is useful; packet-content inspection is not a project goal.
9. Local-first and privacy-preserving by design.
10. New or unknown does not automatically mean malicious.
11. Prefer explicit confidence and evidence over unsupported certainty.
12. Agents advertise capabilities; clients do not assume every platform provides every feature.

## Initial development sequence

1. Stabilize and modernize `Sentinel-Android`.
2. Establish `Sentinel-Protocol` v0 draft.
3. Implement the first self-reporting `Sentinel-Agent-Linux`.
4. Display Linux host/listener/connection data in `Sentinel-Android`.
5. Implement `Sentinel-Agent-Windows` against the same protocol.
6. Add richer baselines, correlation, and optional future network-sensor support.

## Repository versions

New placeholder repositories begin at `0.1-dev.1`.

`Sentinel-Android` retains its own existing application version history.

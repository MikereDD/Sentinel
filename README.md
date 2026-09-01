# Sentinel

Sentinel is a local-first network awareness and monitoring system for discovering devices, tracking changes, monitoring services, and correlating host and connection telemetry across Android, Linux, and Windows.

This repository is the canonical Sentinel monorepo.

## Components

- `Sentinel-Android`
  - Android dashboard/controller
  - Local network discovery
  - Device inventory, activity, monitors, and alerts

- `Sentinel-Agent-Linux`
  - Linux-native host telemetry
  - Listeners, connections, service/process attribution, and health

- `Sentinel-Agent-Windows`
  - Windows-native host telemetry
  - Equivalent Windows-side visibility through native APIs

- `Sentinel-Protocol`
  - Shared platform-neutral contract
  - Hosts, services, connections, events, capabilities, authentication, versioning, and compatibility

## Project direction

Sentinel distinguishes between what it:

- discovered
- observed
- knows directly through instrumentation

The core philosophy is:

**Observe first. Understand second. Act deliberately.**

See `WORKSPACE.md` for architecture and development sequencing.

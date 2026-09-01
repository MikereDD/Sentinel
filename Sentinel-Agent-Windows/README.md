# Sentinel Agent — Windows

Windows-native telemetry agent for the Sentinel ecosystem.

## Purpose

The Windows agent provides direct host telemetry using Windows-native facilities while speaking the same logical Sentinel protocol as the Linux agent.

## Initial responsibilities

- Stable host identity.
- Agent/protocol version information.
- Capability advertisement.
- Listening sockets.
- Active inbound/outbound connection metadata.
- Process/service attribution where available.
- Windows service health/state where available.
- Event/change reporting.
- Local authenticated communication with Sentinel clients.

## Compatibility rule

Windows and Linux are not required to collect telemetry the same way.

They are required to describe equivalent concepts consistently through `Sentinel-Protocol` and to advertise platform-specific capabilities explicitly.

## Implementation language

Not selected yet.

The repository structure intentionally avoids locking the implementation during bootstrap.

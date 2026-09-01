# Sentinel Agent — Linux

Linux-native telemetry agent for the Sentinel ecosystem.

## Purpose

The Linux agent gives Sentinel direct, host-side knowledge that an Android network scanner cannot obtain reliably from the outside.

The agent is expected to report only information supported by the host and by its advertised capabilities.

## Initial responsibilities

- Stable host identity.
- Agent/protocol version information.
- Capability advertisement.
- Listening sockets.
- Active inbound/outbound connection metadata.
- Process/service attribution where available.
- Service health/state where available.
- Event/change reporting.
- Local authenticated communication with Sentinel clients.

## First milestone

```text
agent starts
→ identifies host
→ advertises capabilities
→ reports host information
→ reports listeners
→ reports active connections
→ Sentinel-Android displays the data
```

## Non-goals for the first milestone

- Packet payload/content inspection.
- Automatic firewall changes.
- eBPF as a hard dependency.
- Enterprise-scale telemetry storage.
- Cloud dependence.

## Implementation language

Not selected yet.

The first implementation should be chosen after the protocol contract and deployment requirements are sufficiently clear rather than locking the project to a language during repository bootstrap.

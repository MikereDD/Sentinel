# Protocol Overview

## Design goals

- Local-first.
- Explicit versioning.
- Capability-driven.
- Evidence-aware.
- Extensible without requiring synchronized platform releases.
- Human-auditable message structures during early development.
- Authentication required before accepting instrumented telemetry.

## Initial transport decision

Not selected yet.

For `0.1-dev.2`, the reference Linux agent emits UTF-8 JSON messages to stdout for
development and testing. That does not define the eventual network transport.

## Message envelope

Every message includes protocol version, message type, message identifier, timestamp,
sender identity, payload, and optional correlation identifier.

Concrete bootstrap message types are documented in `message-types.md`.

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

The first protocol milestone defines message semantics before committing the ecosystem to HTTP, WebSocket, gRPC, raw TCP, or another transport.

## Message envelope

Every message should eventually include at least:

- protocol version
- message type
- message identifier
- timestamp
- sender identity
- payload
- optional correlation identifier

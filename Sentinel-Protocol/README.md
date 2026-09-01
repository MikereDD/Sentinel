# Sentinel Protocol

Platform-neutral communication contract for the Sentinel ecosystem.

`Sentinel-Protocol` exists so Android, Linux, Windows, and future sensors can evolve independently while exchanging the same logical concepts.

## Core concepts

- Envelope
- Agent identity
- Capabilities
- Host
- Service/listener
- Connection
- Event
- Health
- Evidence/source classification

## Evidence classification

Sentinel must preserve how a fact became known:

- `discovered` — inferred or found through network discovery/scanning.
- `observed` — seen through network/sensor telemetry.
- `instrumented` — reported directly by a controlled host/agent.

A receiver must not silently upgrade one evidence class into another.

## Important

The schemas in `schemas/` are **draft bootstrap schemas**, not a frozen protocol.

They exist to give early implementations something concrete to discuss and test. Breaking changes are expected before protocol v1.

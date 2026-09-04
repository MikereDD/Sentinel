# Changelog

## 0.1-dev.2

- Define concrete bootstrap message types for agent hello, capabilities, host snapshots,
  listener snapshots, connection snapshots, and agent errors.
- Add message-level JSON Schemas and example telemetry fixtures.
- Keep the message envelope transport-neutral and JSON-based.
- Extend host, listener, and connection payloads for the first Linux instrumented host.
- Preserve evidence classification explicitly; Linux agent telemetry is `instrumented`.

## 0.1-dev.1

- Establish initial protocol repository structure.
- Define protocol responsibilities and evidence model.
- Add non-normative draft JSON Schema skeletons for core concepts.
- Document versioning, capabilities, authentication, and compatibility goals.

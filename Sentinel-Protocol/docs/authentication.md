# Authentication

Authentication design is intentionally unresolved during bootstrap.

Requirements:

- Instrumented telemetry must be attributable to a paired/trusted agent.
- Pairing must not depend on a cloud service.
- Credentials/secrets must not be committed to repositories.
- Transport security and identity verification must be considered together.
- Revocation/re-pairing must be possible.

Do not implement "trust any agent on the LAN" as the permanent design.

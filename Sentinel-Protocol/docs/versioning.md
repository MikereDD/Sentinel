# Versioning

Repository development begins at `0.1-dev.1`.

Protocol-version rules will be finalized before a stable v1.

Early goals:

- Every message identifies its protocol version.
- Receivers reject incompatible major versions cleanly.
- Optional capabilities permit additive evolution.
- Message fields should have explicit required/optional status.
- Unknown optional fields should be safely ignored where possible.

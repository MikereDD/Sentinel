from __future__ import annotations

import hashlib
from pathlib import Path

_MACHINE_ID_PATHS = (
    Path("/etc/machine-id"),
    Path("/var/lib/dbus/machine-id"),
)

def read_machine_id() -> str:
    for path in _MACHINE_ID_PATHS:
        try:
            value = path.read_text(encoding="utf-8").strip()
        except OSError:
            continue
        if value:
            return value
    raise RuntimeError("No stable Linux machine-id is available")

def identity_token(machine_id: str) -> str:
    material = f"sentinel:linux:{machine_id.strip()}".encode("utf-8")
    return hashlib.sha256(material).hexdigest()[:32]

def host_id(machine_id: str) -> str:
    return f"linux:{identity_token(machine_id)}"

def agent_id(machine_id: str) -> str:
    return f"sentinel-agent-linux:{identity_token(machine_id)}"

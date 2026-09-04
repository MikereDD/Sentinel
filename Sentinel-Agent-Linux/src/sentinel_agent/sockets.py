from __future__ import annotations

from dataclasses import dataclass
import subprocess
from typing import Iterable

@dataclass(frozen=True)
class Endpoint:
    address: str
    port: int
    interface: str | None = None

@dataclass(frozen=True)
class SocketRow:
    protocol: str
    state: str | None
    local: Endpoint
    remote: Endpoint | None

def _parse_port(value: str) -> int:
    if value == "*":
        return 0
    try:
        return int(value)
    except ValueError as exc:
        raise ValueError(f"Non-numeric port from ss: {value}") from exc

def _split_scope(address: str) -> tuple[str, str | None]:
    if "%" not in address:
        return address, None
    base, interface = address.rsplit("%", 1)
    return base, interface or None

def parse_endpoint(value: str) -> Endpoint:
    value = value.strip()

    if value.startswith("[") and "]:" in value:
        address, port_text = value[1:].rsplit("]:", 1)
    else:
        if ":" not in value:
            raise ValueError(f"Endpoint has no port: {value}")
        address, port_text = value.rsplit(":", 1)

    address, interface = _split_scope(address)
    return Endpoint(
        address=address,
        port=_parse_port(port_text),
        interface=interface,
    )

def _run_ss(args: list[str]) -> list[str]:
    proc = subprocess.run(["ss", *args], check=True, capture_output=True, text=True)
    return [line for line in proc.stdout.splitlines() if line.strip()]

def _rows(lines: Iterable[str], listening: bool) -> list[SocketRow]:
    rows: list[SocketRow] = []
    for raw in lines:
        parts = raw.split()
        if len(parts) < 5:
            continue

        protocol = parts[0].lower()
        state = parts[1] if len(parts) > 1 else None

        try:
            local = parse_endpoint(parts[-2])
            remote = parse_endpoint(parts[-1])
        except ValueError:
            continue

        rows.append(
            SocketRow(
                protocol=protocol,
                state=state,
                local=local,
                remote=None if listening else remote,
            )
        )
    return rows

def listeners() -> list[SocketRow]:
    return _rows(_run_ss(["-H", "-lntu"]), listening=True)

def connections() -> list[SocketRow]:
    return [
        row
        for row in _rows(_run_ss(["-H", "-ntu"]), listening=False)
        if row.remote is not None and row.remote.port != 0
    ]

from __future__ import annotations

import json
import platform
import socket
import subprocess
from pathlib import Path
from typing import Any

def _read_os_release() -> dict[str, str]:
    values: dict[str, str] = {}
    try:
        lines = Path("/etc/os-release").read_text(encoding="utf-8").splitlines()
    except OSError:
        return values
    for line in lines:
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key] = value.strip().strip('"')
    return values

def _fallback_addresses() -> list[str]:
    found: set[str] = set()
    try:
        for info in socket.getaddrinfo(socket.gethostname(), None):
            address = info[4][0]
            if address not in {"127.0.0.1", "::1"} and not address.startswith("fe80:"):
                found.add(address)
    except OSError:
        pass
    return sorted(found)

def _ip_addresses() -> list[str]:
    try:
        proc = subprocess.run(["ip", "-j", "addr", "show"], check=True, capture_output=True, text=True)
        data = json.loads(proc.stdout)
    except (OSError, subprocess.CalledProcessError, json.JSONDecodeError):
        return _fallback_addresses()

    found: set[str] = set()
    for iface in data:
        if iface.get("ifname") == "lo":
            continue
        for info in iface.get("addr_info", []):
            local = info.get("local")
            family = info.get("family")
            if local and family in {"inet", "inet6"} and not local.startswith("fe80:"):
                found.add(local)
    return sorted(found)

def snapshot(host_id: str) -> dict[str, Any]:
    os_release = _read_os_release()
    hostname = socket.gethostname()
    return {
        "hostId": host_id,
        "hostname": hostname,
        "displayName": hostname,
        "platform": "linux",
        "osName": os_release.get("PRETTY_NAME") or os_release.get("NAME"),
        "osVersion": os_release.get("VERSION_ID"),
        "kernel": platform.release(),
        "architecture": platform.machine(),
        "addresses": _ip_addresses(),
        "evidence": "instrumented",
    }

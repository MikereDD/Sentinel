from __future__ import annotations
from typing import Any
from .sockets import connections as collect_rows

def snapshot(host_id: str) -> dict[str, Any]:
    items = []
    for row in collect_rows():
        if row.remote is None:
            continue

        item = {
            "hostId": host_id,
            "direction": "unknown",
            "protocol": row.protocol,
            "state": row.state,
            "localAddress": row.local.address,
            "localPort": row.local.port,
            "remoteAddress": row.remote.address,
            "remotePort": row.remote.port,
            "evidence": "instrumented",
        }

        if row.local.interface:
            item["localInterface"] = row.local.interface
        if row.remote.interface:
            item["remoteInterface"] = row.remote.interface

        items.append(item)

    return {"hostId": host_id, "connections": items}

from __future__ import annotations
from typing import Any
from .sockets import listeners as collect_rows

def snapshot(host_id: str) -> dict[str, Any]:
    items = []
    for row in collect_rows():
        item = {
            "hostId": host_id,
            "protocol": row.protocol,
            "localAddress": row.local.address,
            "port": row.local.port,
            "state": row.state,
            "evidence": "instrumented",
        }
        if row.local.interface:
            item["localInterface"] = row.local.interface
        items.append(item)

    return {"hostId": host_id, "listeners": items}

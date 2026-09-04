from __future__ import annotations
from typing import Any
from .sockets import listeners as collect_rows

def snapshot(host_id: str) -> dict[str, Any]:
    return {"hostId": host_id, "listeners": [{
        "hostId": host_id,
        "protocol": row.protocol,
        "localAddress": row.local.address,
        "port": row.local.port,
        "state": row.state,
        "evidence": "instrumented",
    } for row in collect_rows()]}

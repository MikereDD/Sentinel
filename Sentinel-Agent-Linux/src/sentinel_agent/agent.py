from __future__ import annotations
from typing import Any

from . import PROTOCOL_VERSION, __version__
from .capabilities import CAPABILITIES
from .connections import snapshot as connection_snapshot
from .host import snapshot as host_snapshot
from .identity import agent_id, host_id, read_machine_id
from .listeners import snapshot as listener_snapshot
from .protocol import envelope

def collect_messages() -> list[dict[str, Any]]:
    machine_id = read_machine_id()
    aid = agent_id(machine_id)
    hid = host_id(machine_id)

    messages = [
        envelope("agent.hello", aid, {
            "agentId": aid, "agentVersion": __version__,
            "platform": "linux", "protocolVersion": PROTOCOL_VERSION,
        }),
        envelope("agent.capabilities", aid, {
            "agentId": aid, "platform": "linux", "agentVersion": __version__,
            "capabilities": list(CAPABILITIES),
        }),
        envelope("host.snapshot", aid, host_snapshot(hid)),
    ]

    for message_type, component, collector in (
        ("network.listeners", "listeners", listener_snapshot),
        ("network.connections", "connections", connection_snapshot),
    ):
        try:
            messages.append(envelope(message_type, aid, collector(hid)))
        except Exception as exc:
            messages.append(envelope("agent.error", aid, {
                "component": component,
                "message": str(exc),
                "recoverable": True,
            }))
    return messages

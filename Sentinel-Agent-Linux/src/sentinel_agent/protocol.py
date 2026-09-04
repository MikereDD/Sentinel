from __future__ import annotations

from datetime import datetime, timezone
from typing import Any
from uuid import uuid4

from . import PROTOCOL_VERSION

def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z")

def envelope(message_type: str, sender_id: str, payload: Any, correlation_id: str | None = None) -> dict[str, Any]:
    return {
        "protocolVersion": PROTOCOL_VERSION,
        "messageType": message_type,
        "messageId": str(uuid4()),
        "timestamp": utc_now(),
        "senderId": sender_id,
        "correlationId": correlation_id,
        "payload": payload,
    }

from __future__ import annotations
import argparse
import json
import sys
from .agent import collect_messages

def main() -> int:
    parser = argparse.ArgumentParser(description="Sentinel Linux telemetry agent")
    parser.add_argument("--pretty", action="store_true")
    args = parser.parse_args()
    try:
        messages = collect_messages()
    except Exception as exc:
        print(f"sentinel-agent-linux: {exc}", file=sys.stderr)
        return 1

    if args.pretty:
        print(json.dumps(messages, indent=2))
    else:
        for message in messages:
            print(json.dumps(message, separators=(",", ":")))
    return 0

if __name__ == "__main__":
    raise SystemExit(main())

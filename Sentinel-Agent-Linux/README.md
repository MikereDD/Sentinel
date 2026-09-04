# Sentinel Agent — Linux

Linux-native telemetry agent for the Sentinel ecosystem.

## Current milestone — 0.1-dev.2

The first executable agent is implemented in Python 3 using the standard library plus
common Linux userspace commands (`ip` and `ss`).

It currently provides:

- stable host identity derived from `/etc/machine-id`;
- agent/protocol version information;
- explicit capability advertisement;
- host metadata and addresses;
- listening TCP/UDP sockets;
- active TCP/UDP connection metadata;
- Sentinel Protocol JSON output.

Advertised capabilities:

- `host.identity`
- `network.listeners`
- `network.connections`

## Run

```bash
cd Sentinel-Agent-Linux
PYTHONPATH=src python -m sentinel_agent
```

Human-readable output:

```bash
PYTHONPATH=src python -m sentinel_agent --pretty
```

## Test

```bash
PYTHONPATH=src python -m unittest discover -s tests -v
```

## First milestone flow

```text
agent starts
→ identifies host
→ advertises capabilities
→ reports host information
→ reports listeners
→ reports active connections
→ emits Sentinel Protocol JSON
```

## Deferred

- process/service attribution;
- systemd service state;
- authentication/network transport;
- eBPF;
- packet payload inspection;
- automatic firewall changes;
- cloud dependence.

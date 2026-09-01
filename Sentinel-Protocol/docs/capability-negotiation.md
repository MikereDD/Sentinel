# Capability Negotiation

Agents advertise what they can provide.

Example capability families:

- `host.identity`
- `host.health`
- `network.listeners`
- `network.connections`
- `network.process_attribution`
- `services.state`
- `services.systemd`
- `services.windows`
- `events.changes`

Clients must treat capabilities as data, not assumptions based only on operating-system name.

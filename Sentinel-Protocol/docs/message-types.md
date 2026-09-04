# Bootstrap message types

Protocol `0.1-dev.2` defines:

- `agent.hello`
- `agent.capabilities`
- `host.snapshot`
- `network.listeners`
- `network.connections`
- `agent.error`

Snapshot messages replace the receiver's current view for that category. Transport
remains intentionally undecided.

Linux agent facts in this milestone use evidence class `instrumented`.

The first Linux implementation advertises only:

- `host.identity`
- `network.listeners`
- `network.connections`

Process/service attribution is not advertised until it is implemented reliably.

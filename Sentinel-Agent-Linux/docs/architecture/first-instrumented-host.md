# First instrumented host

`0.1-dev.2` establishes the first executable Sentinel instrumented-host path.

The agent derives identity from `/etc/machine-id`, hashes it with a Sentinel/Linux
namespace, and never emits the raw machine-id. Hostname and addresses are metadata,
not identity.

Collectors:

- addresses: `ip -j addr show`, with a socket fallback;
- listeners: `ss -H -lntu`;
- active connections: `ss -H -ntu`;
- OS metadata: `/etc/os-release` and kernel/platform data.

Facts reported directly by this controlled agent use evidence `instrumented`.

Process ownership is not claimed in this milestone.

The runner emits shared Sentinel Protocol messages as JSON Lines or a pretty JSON
array, proving the contract before network transport and authentication are selected.

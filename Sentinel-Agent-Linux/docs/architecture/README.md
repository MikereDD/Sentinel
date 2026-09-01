# Linux Agent Architecture

This directory will hold Linux-specific architecture decisions.

Initial collection candidates include standard kernel/userspace interfaces such as:

- `/proc`
- socket tables
- `ss`
- systemd/service state
- conntrack/nftables data where explicitly enabled and useful

The agent should begin with the least-privileged reliable mechanism and add deeper capabilities only when they provide clear value.

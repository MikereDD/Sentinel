# Windows Agent Architecture

This directory will hold Windows-specific architecture decisions.

Likely areas of investigation include:

- IP Helper / networking APIs
- process/socket ownership
- Windows Service Control Manager
- Event Log integration where useful
- firewall state only when explicitly required

Prefer documented, supported Windows APIs over scraping command output when practical.

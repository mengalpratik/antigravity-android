# Environment, Diagnostics & Gateway Subsystem (`modules/env`)

> Core subsystem managing dynamic Android filesystem path resolution, hardware and network preflight diagnostics, structured flight-recorder logging, and system IPC with the Android Gateway.

---

## 1. Architectural Overview

The `modules/env` subsystem decouples runtime components from hardcoded filesystem paths, providing a resilient execution context across different Android OS versions and device manufacturers:

```
 +-----------------------------------------------------------------------------------+
 |                             SPECIFICATION LAYER                                   |
 |          [spec/paths.json]                     [spec/env_vars.json]               |
 |    (Base Android Path Templates)          (Static vars, PATH, LD_LIBRARY_PATH)    |
 +--------------------------+---------------------------------+----------------------+
                            |                                 |
                            v                                 v
 +-----------------------------------------------------------------------------------+
 |                              CONTEXT & RESOLVER                                   |
 |                   [env_context.py]         [path_resolver.py]                     |
 |        (Resolves $PREFIX, $HOME, app-private and external workspace paths)        |
 +--------------------------+---------------------------------+----------------------+
                            |                                 |
                            +----------------+----------------+
                                             |
                                             v
 +-----------------------------------------------------------------------------------+
 |                          EXECUTION & DIAGNOSTICS LAYER                            |
 |  [preflight.py]        [port_checker.py]       [command_audit.py]  [logger.py]    |
 |  (CPU/ABI/Storage)     (Port collision check)  (Audit logging)     (Telemetry)    |
 +-----------------------------------------------------------------------------------+
```

---

## 2. Directory Layout & Key Modules

```
modules/env/
├── spec/
│   ├── paths.json              # Canonical path templates ($HOME, $PREFIX, tmp, workspace)
│   └── env_vars.json           # Base environment variable specifications
├── env_context.py              # Dynamic runtime context inspector (app-id, user-id, API level)
├── path_resolver.py            # Path resolution engine with placeholder expansion
├── preflight.py                # Hardware, storage, architecture, and permission checks
├── port_checker.py             # TCP socket collision and loopback availability scanner
├── diag_runner.py              # Unified diagnostic runner with JSON reporting
├── structured_logger.py        # Structured JSON logger with sensitive data scrubbing
├── command_audit.py            # Execution flight recorder tracking shell commands
├── gateway_client.py           # REST client communicating with the local SystemGateway
└── tests/                      # Automated module tests
```

---

## 3. Key Capabilities

1. **Dynamic Path Resolution:** Resolves paths at runtime according to the Android package ID and user profile (`/data/user/<id>/<pkg>/files`), preventing hardcoded path failures.
2. **Preflight Hardware Validation:** Assesses ARM64 architecture, baseline CPU features (LSE atomics emulation), available internal storage, and write permissions before starting runtime services.
3. **Sensitive Data Redaction:** Automatically scrubs API keys, authorization bearer tokens, and session identifiers from stdout, stderr, and exported telemetry logs.
4. **Local System Gateway Client:** Dispatches Android platform requests (displaying notifications, setting wake locks, querying hardware sensors) over local loopback.

---

## 4. Testing & Verification

Run the test suite:

```bash
python3 -m unittest discover -s modules/env/tests
```

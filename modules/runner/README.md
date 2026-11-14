# Runtime Runner & Lifecycle Subsystem (`modules/runner`)

> Orchestrates runtime preparation, core server daemonization, health probing, graceful shutdown, and end-to-end integration verification on Android ARM64.

---

## 1. Scope & Responsibilities

The `modules/runner` subsystem manages the complete operational lifecycle of the standalone Antigravity core engine:

```
modules/runner/
├── prepare_runtime.py   # Assembles runtime directory, verifies checksums & permissions
├── start_core.sh        # Starts language server in background with I/O bus routing
├── probe_server.py      # Health probe monitoring liveness and readiness endpoints
├── stop_core.sh         # Graceful process termination and resource cleanup
├── test_e2e.sh          # Full end-to-end operational lifecycle verification test
└── tests/               # Automated unit test suite
```

---

## 2. Lifecycle Stages

1. **Preparation (`prepare_runtime.py`):**
   - Verifies pristine binary checksums and unpacks required glibc shared objects.
   - Sets executable permissions (`0755`) on `language_server`, the dynamic linker, and shims.
   - Deploys default seed configurations and environment manifests.
2. **Execution (`start_core.sh`):**
   - Configures `LD_LIBRARY_PATH` and runtime environment variables.
   - Spawns the language server process bound strictly to loopback (`127.0.0.1:45157`).
   - Hooks stdout/stderr streams into the `modules/io` event bus.
3. **Health Probing (`probe_server.py`):**
   - Polls HTTP/gRPC health check endpoints until the server signals operational readiness.
   - Implements exponential backoff with configurable timeout thresholds.
4. **Graceful Teardown (`stop_core.sh`):**
   - Issues `SIGTERM` to allow active workspace tasks to save state cleanly.
   - Falls back to `SIGKILL` after a safety grace period, cleaning up FIFO pipes and lock files.

---

## 3. End-to-End Verification

Run the full lifecycle test suite:

```bash
bash modules/runner/test_e2e.sh
```

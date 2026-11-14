# Changelog: Antigravity for Android

All notable changes to **Antigravity for Android** are documented in this file.  
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.5.1] - 2026-11-14

### Added
- **Centralized Workspace Root Manager (`WorkspaceRootManager`):**
  - Standardized dedicated application workspace root at `/storage/emulated/0/Antigravity/`.
  - IDE-grade directory hierarchy:
    - `Projects/`: Primary destination for newly created projects and Git repositories.
    - `Workspaces/Default/`: Initial workspace location for project planning and intelligence documents.
    - `System/`: Centralized configurations (`config/`, `profiles/`, `models/`).
    - `Agents/`: Persona definitions, prompts (`prompts/`), execution rules (`instructions/`), and tools (`tools/`).
    - `Knowledge/`: Agent long-term memory (`memory/`), offline references (`references/`), and documentation (`documentation/`).
    - `Logs/` & `Cache/`: Runtime diagnostics and staging caches decoupled from source repositories (`.nomedia` protected).
    - `Backups/` & `Exports/`: System snapshots, backups, and user report bundles.
- **Dynamic SAF Custom Directory Selection & Restart Persistence:**
  - Full support for selecting custom storage volumes / SD cards via Storage Access Framework.
  - Custom paths persisted in Android `SharedPreferences` across restarts with graceful fallback.
- **Safe Legacy Projects Migration Engine:**
  - Automated detection of legacy `/storage/emulated/0/Projects/` folder.
  - Non-destructive migration preserving `.git` metadata, hidden files, timestamps, and file contents.
  - Post-migration integrity verification and automatic atomic rollback on failure.
- **Extended System Gateway REST Endpoints (`127.0.0.1:45160`):**
  - Added `workspace_root_info`, `workspace_set_custom_root`, `workspace_reset_root`, `workspace_detect_legacy`, and `workspace_migrate_legacy`.

---

## [2.5.0] - 2026-11-10

### Added
- **Centralized Workspace Architecture (Architecture A):**
  - Introduced unified root hierarchy at `Internal Storage/Antigravity/` with dedicated directories:
    - `Workspace/`: Project-specific planning, architecture, research, reports, collaboration, and session logs.
    - `Projects/`: Actual project source-code trees for default-created projects.
    - `Templates/`: Standard templates for projects, documentation, and system architectures.
    - `Exports/`: Centralized store for reports, documentation packages, and workspace backups.
    - `Shared/`: Global references, developer guidelines, and reusable shared resources.
- **Unified SQLite Project Registry:**
  - Integrated `antigravity_projects.db` with thread-safe `ProjectRegistryRepository` tracking UUID project identities, display names, source directory bindings, workspace directory bindings, creation/update timestamps, status (`ACTIVE`/`ARCHIVED`), source location types (`DEFAULT`/`CUSTOM`), and workspace schema versions (`1.0.0`).
  - Added zero-dependency `ProjectYamlParser` generating human-readable `project.yaml` manifests in every workspace root.
- **Automated Workspace Document Generator:**
  - Automatically initializes standard root documents (`project.yaml`, `PROJECT_CONTEXT.md`, `TASKS.md`) and pre-configures structured folders (`planning/`, `architecture/`, `research/`, `reports/`, `collaboration/`, `sessions/`).
  - Syncs registered project entries to `$HOME/.gemini/config/projects/<UUID>.json` for autonomous agent awareness.
- **System Gateway Workspace REST APIs:**
  - Added dedicated endpoints on `127.0.0.1:45160`:
    - `GET /workspace/list`: List all registered projects with status and directory references.
    - `POST /workspace/quick-create`: 1-tap creation of a default project and linked centralized documentation workspace.
    - `POST /workspace/import`: Register an existing project repository from any custom storage location without modifying or duplicating source files.
    - `POST /workspace/reconnect`: Detect and reconnect previously registered projects whose storage mount or source paths changed.
    - `POST /workspace/archive` & `POST /workspace/restore`: Manage project lifecycle states.
    - `POST /workspace/remove`: Safely deregister projects from the system while protecting underlying source code.
    - `POST /workspace/verify`: Validate directory existence and structural integrity.
- **Legacy Migration & Auto-Seeding:**
  - Integrated automatic legacy migration in `OnboardingSeeder.java` transitioning legacy projects from `/storage/emulated/0/Projects/` to the new centralized workspace architecture.
  - Pre-seeds `/storage/emulated/0/Antigravity/` path grants and folder picker fallbacks.

---

## [2.4.2] - 2026-11-10

### Added
- **Native Superuser Root (`su`) Integration:** Automatic runtime detection of KernelSU, Magisk, and APatch providers via `RootBridge.java`.
- **Privileged Command Execution:** Added `root: true` and `auto_elevate: true` options to `SystemGatewayServer`'s `ExecHandler` for `uid=0` execution.
- **Default Turbo Mode Configuration:** Pre-seeded configuration in `OnboardingSeeder.java` configuring `autoExecutionPolicy: CASCADE_COMMANDS_AUTO_EXECUTION_EAGER`, `fileAccessPolicy: AGENT_SETTING_POLICY_ALLOW`, and unrestricted workspace grants.
- **Root Status Indicator & Quick Settings:** UI header bar includes a centered `# ROOT` status badge and a direct settings modal shortcut (`⚙`).
- **Default Project Workspace:** Automatically pre-seeds `/storage/emulated/0/Projects/` as the default workspace directory.

### Fixed
- **Test Suite Port Isolation:** Isolated gateway port in `env/tests/selftest` to ensure `agy-notify` fallback checks succeed cleanly while `CoreServerService` is running.

---

## [2.4.1] - 2026-11-08

### Added
- **Built-in Web Terminal & PTY Spawner Support (PR #19):** Enabled native terminal execution inside WebView via length-preserving PTY shell binary patching (`/bin/bash` &rarr; `/bin///sh`, exactly 9 bytes) in `modules/patches/groups/pty_shell`.
- **Resilient Shell Hierarchy:** Configured prioritized `$SHELL` fallback hierarchy in `CoreEnvironment.java`: `$PREFIX/bin/bash` &rarr; `runtime/bin/bash` &rarr; `/system/bin/sh`.
- **Terminal Color & Capabilities:** Added `TERM=xterm-256color` and automatic `TERMINFO` configuration pointing to `$PREFIX/share/terminfo` for ANSI rendering in `nano`, `htop`, and interactive CLI programs.

### Fixed
- **AAPT Build Reliability on ARM64:** Added `export ANDROID_DATA="${ANDROID_DATA:-/data}"` to `modules/builder/02_r_java.sh` and `06_package.sh`, preventing `aapt` crash with `SIGABRT (134)` during on-device compilation.

---

## [2.4.0] - 2026-11-07

### Added
- **Modular Subsystem Architecture:** Refactored monolithic codebase into 6 standalone decoupled subsystems under `modules/` (`core`, `patches`, `io`, `env`, `builder`, `runner`) with independent unit test suites.
- **Universal ARMv8.0-A CPU Emulation:** Added transparent atomic and SIMD emulation library (`liblse_compat.so`) trapping `SIGILL` on older ARM64 chipsets (Snapdragon 685, Cortex-A53) to emulate FEAT_LSE instructions.
- **Autonomous Background Micro-Daemon (`agy-daemon`):** Lightweight background scheduler (<10 MB RAM) running under Android Foreground Service Partial WakeLock for cron tasks, file watchers, and AI triggers.
- **Native Hardware Torch Bridge:** Integrated CameraManager flashlight bridge (`com.agy.ACTION_TORCH`) and CLI command (`torch on/off`).
- **Native Canvas Splash Screen:** Dot animation splash implemented in `NativeSplashDotsView.java`, saving ~40 MB RAM compared to heavy WebView-based splash screens.
- **Desktop Mode Toggle:** Responsive toggle in Appearance settings for Samsung DeX, tablets, and external monitors.

### Security & Hardening
- **Debuggable Flag Isolation (Issue #6):** Completely removed `android:debuggable="true"` from production release APKs (`com.agy`), eliminating vulnerability where `run-as` could access OAuth tokens and private files. Isolated debuggable flag to `com.agy.dev`.
- **Media & File Picker Overhaul (Issue #8):** Replaced SAF-only document picker with universal native Android Intent chooser supporting persistent URI permissions.

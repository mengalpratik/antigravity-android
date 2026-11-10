# Changelog: Antigravity for Android

All notable changes to **Antigravity for Android** are documented in this file.  
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

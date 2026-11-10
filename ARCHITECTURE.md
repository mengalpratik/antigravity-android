# System Architecture: Antigravity for Android

> **Technical Architecture & Component Interaction Specification**  
> *Project Lead:* Pratik Mengal  
> *Independent Community Project &mdash; Not an official Google product and not affiliated with Google LLC.*

---

## 1. High-Level Architectural Diagram

```mermaid
flowchart TD
    subgraph UI_LAYER["Presentation & WebView Layer (web/)"]
        WV["Android Chromium WebView"]
        SPA["React SPA Frontend"]
        DM["Desktop Mode Toggle"]
        TS["Theme Sync & CSS Engine"]
        WV --> SPA
        SPA --> DM
        SPA --> TS
    end

    subgraph HOST_LAYER["Android Host Application Shell (app/)"]
        MA["MainActivity\n(Splash & Window Lifecycle)"]
        CSS["CoreServerService\n(Foreground dataSync & WakeLock)"]
        AB["AndroidBridge\n(@JavascriptInterface)"]
        OM["OAuthManager\n(System Browser & Token Store)"]
        SGW["SystemGatewayServer\n(127.0.0.1:45160 HTTP)"]
        RB["RootBridge (su - uid 0)\nShizukuBridge (rish - uid 2000)"]
        
        MA --> AB
        MA --> OM
        MA --> CSS
        CSS --> SGW
        CSS --> RB
    end

    subgraph CORE_LAYER["Language Server Core Engine (modules/core & modules/patches)"]
        LS["language_server (Go AArch64 ELF)"]
        GLIBC["glibc loader (ld-linux-aarch64.so.1)"]
        LSE["liblse_compat.so (ARMv8.0 Atomics)"]
        PATCHES["Binary Patches\n(PTY, Syscalls, Resolv, Gates)"]
        
        GLIBC --> LSE
        LSE --> LS
        PATCHES -.-> LS
    end

    subgraph IO_LAYER["High-Performance I/O Bus (modules/io)"]
        FIFO["Named FIFO Pipe (core_stream.fifo)"]
        SS["Stream Splitter (stream_splitter.py)"]
        EB["Event Broker (event_broker.py)"]
        LOGS["Flight Recorder Ring Buffer"]
        
        LS --> FIFO
        FIFO --> SS
        SS --> EB
        SS --> LOGS
    end

    subgraph USERSPACE_LAYER["Native Bionic Userspace (env/)"]
        SHELL["Bionic /bin/sh & Bash 5.3"]
        PKG["pkg Package Manager"]
        DAEMON["agy-daemon\n(Autonomous Micro-Scheduler)"]
        TOOLS["Toolchains: Python 3, Git, Node, Clang"]
        
        SHELL --> PKG
        SHELL --> TOOLS
        CSS --> DAEMON
    end

    subgraph STORAGE_LAYER["Filesystem & Android Platform"]
        INTERNAL["App Data: /data/user/0/com.agy/files"]
        EXTERNAL["Project Workspace: /storage/emulated/0/Projects"]
        TOKEN[".gemini/jetski-standalone-oauth-token"]
    end

    SPA <===>|HTTPS / gRPC (127.0.0.1:45157)| LS
    SPA <===>|Native IPC JavaScript Interface| AB
    LS <===>|System Calls via Gateway| SGW
    LS -->|Terminal PTY Spawner| SHELL
    SHELL <-->|Read / Write| EXTERNAL
    LS <-->|State & Cache| INTERNAL
    OM -.->|Polls Auth Token| TOKEN
```

---

## 2. Component Subsystems Breakdown

### 2.1. Android Host Shell (`app/`)
The native Android host acts as the supervisor, resource manager, and hardware bridge for the environment:

- **`MainActivity.java`:**
  - Serves as the primary activity hosting the WebView container.
  - Implements dynamic splash screen synchronization (`SplashController.java`). The splash remains visible while the language server boots and dismisses only when `AndroidBridge.onUiReady()` is triggered by the web frontend.
  - Controls dynamic launcher icon switching between dark and light themes using programmatic component toggling (`LauncherDarkActivity` vs `LauncherLightActivity`).
- **`CoreServerService.java`:**
  - An Android Foreground Service registered with `foregroundServiceType="dataSync"`.
  - Holds a `WakeLockHolder` (`PARTIAL_WAKE_LOCK`) preventing CPU sleep during autonomous coding, dependency installation, or long-running builds.
  - Spawns and supervises the `language_server` process, capturing stdout/stderr into rotating log buffers (`LogRingBuffer.java`).
  - Automatically launches the background scheduler daemon (`agy-daemon start`).
- **`SystemGatewayServer.java` & `GatewayDispatcher.java`:**
  - A zero-dependency, ultra-lightweight embedded HTTP server listening strictly on `127.0.0.1:45160`.
  - Exposes Android OS capabilities to CLI tools, Python scripts, and the agent:
    - `notify`: Posts or cancels rich Android status bar notifications (`NotificationHandler.java`).
    - `open_url`: Dispatches URLs to the system browser (`BrowserHandler.java`).
    - `clipboard`: Reads or updates the Android system clipboard (`ClipboardHandler.java`).
    - `system_info`: Queries battery level, memory stats, and OS version (`DeviceHandler.java`).
    - `exec`: Dispatches commands with optional privilege elevation (`ExecHandler.java`).
- **`AndroidBridge.java`:**
  - JavaScript interface registered in the Chromium WebView under `window.AndroidBridge`.
  - Provides two-way communication between the web frontend and Android: handles file downloads, UI readiness signals, desktop mode toggles, and system toast messages.
- **`OAuthManager.java` & `OAuthTokenStore.java`:**
  - Observes OAuth authentication requests.
  - Dispatches browser intents without breaking custom tab flows.
  - Watches `$HOME/.gemini/jetski-standalone-oauth-token` to signal successful user login to the UI.
- **`RootBridge.java` & `ShizukuBridge.java`:**
  - **`RootBridge`:** Direct superuser execution (`su -c "<command>"`) with automatic detection of KernelSU, Magisk, and APatch.
  - **`ShizukuBridge`:** Dispatches commands through Shizuku's `rish` IPC socket (`uid=2000`) for non-rooted environments.

---

### 2.2. Language Server Core & Patch Engine (`modules/core` & `modules/patches`)

The agent intelligence is driven by Google's native AArch64 ELF binary (`language_server`), compiled originally for standard Linux environments and adapted for Android through modular binary patching:

- **Glibc Loader (`ld-linux-aarch64.so.1`):**
  Because Android utilizes Bionic libc, the core Go binary requires an isolated glibc execution environment. The glibc dynamic linker and runtime libraries (`libc.so.6`, `libm.so.6`, `libpthread.so.0`, `libdl.so.2`) are packaged in `modules/core/glibc/` and loaded via explicit invocation:
  ```bash
  ld-linux-aarch64.so.1 --library-path <runtime>/lib --preload <runtime>/lib/liblse_compat.so <runtime>/bin/liblanguage_server.so [args...]
  ```
- **Bytecode Patch Pipeline (`modules/patches/engine/`):**
  Direct in-place binary modifications applied to the pristine ELF binary:
  1. **`pty_shell`:** Replaces the hardcoded `/bin/bash` terminal spawner path with `/bin///sh` (preserving the exact 9-byte string length) to allow native terminal execution on Android where `/bin/bash` does not exist.
  2. **`resolv_redirect`:** Redirects `/etc/resolv.conf` accesses to the app's internal glibc resolver configuration.
  3. **`sigill_handler`:** Emulates or catches unsupported vector/atomic instructions.
  4. **`faccessat` & `fchmodat`:** Prevents fatal permission errors when manipulating files on Android's FUSE shared storage.
  5. **`cli_gate` & `enterprise_auth`:** Bypasses region/account eligibility gates in the region-bypass build edition.
- **Cryptographic Manifest (`modules/core/manifest/`):**
  Generates and verifies `manifest.json` containing SHA-256 hashes of all bundled binaries, libraries, and SSL certificates to guarantee byte-for-byte runtime integrity before execution.

---

### 2.3. Native Bionic Userspace & Package Manager (`env/`)

Rather than relying on emulation or heavy chroot containers, Antigravity executes shell tools directly inside Android's native userspace:

- **Userspace Rootfs (`$HOME/usr`):**
  Houses the native binary directory (`bin/`), library directory (`lib/`), and configuration files (`etc/`).
- **`pkg` Package Manager (`env/pkg/pkg`):**
  An ultra-fast, bash/awk-based native package manager that resolves dependencies, parses Debian `.deb` control metadata, and installs packages from curated Termux package mirrors.
- **Hardware Emulation (`liblse_compat.so`):**
  Transparent LD_PRELOAD library written in C that traps `SIGILL` signals on ARMv8.0-A CPUs (e.g. Snapdragon 685, Cortex-A53) and emulates ARMv8.1-A Large System Extensions (FEAT_LSE) atomic instructions (`cas`, `ldadd`, `swp`).
- **Autonomous Micro-Daemon (`agy-daemon`):**
  Background scheduler written in bash that supports cron expressions, delay timers, file-watch triggers, and GitHub PR polling under Partial WakeLock.

---

### 2.4. Web UI & Frontend (`web/`)

- **Chromium WebView SPA:** Single-page React application providing chat history, diff viewers, artifact inspectors, and configuration panels.
- **Desktop Mode:** Modifies viewport meta tags and injects custom layout stylesheets to transition between touch-friendly mobile layouts and multi-column desktop workstation views.
- **Theme Synchronization:** Binds Android OS system day/night states and user preference to the web frontend CSS theme variables.

---

### 2.5. Gradle-Free APK Builder (`modules/builder/`)

A self-contained Android build pipeline designed to build valid APKs without Gradle, Maven, or Android Studio:

```
[01_gen_config.py] ──> Generates BuildConfig.java, gen.xml, AndroidManifest.xml
         │
[02_r_java.sh]     ──> Runs aapt package to generate R.java
         │
[03_compile_java.sh] ─> Runs javac to compile Java classes
         │
[04_dex.sh]        ──> Runs Android D8 (r8.jar) to convert bytecode to classes.dex
         │
[05_stage_assets.py] ─> Prepares runtime assets, web bundles, and core binaries
         │
[06_package.sh]    ──> Runs aapt to package unaligned APK
         │
[align.py]         ──> Python 4-byte ZipAligner aligns uncompressed assets
         │
[08_sign.sh]       ──> Signs APK with apksigner (v2 & v3 schemes) using debug.keystore
         │
[09_verify.sh]     ──> Verifies APK signature and structural integrity
```

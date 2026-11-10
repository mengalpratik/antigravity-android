# Project Overview: Antigravity for Android

> **Autonomous AI Development Environment & Native Bionic Userspace for Android ARM64**  
> *Project Maintainer & Lead:* Pratik Mengal  
> *Independent Community Project &mdash; Not an official Google product and not affiliated with Google LLC.*

---

## 1. Executive Summary & Purpose

**Antigravity for Android** is an independently developed and maintained native Android application that ports and hosts Google's Antigravity / JetSki autonomous agentic AI development environment directly onto mobile and portable ARM64 hardware.

### Primary Purpose
To empower software engineers, developers, and AI researchers with a **fully autonomous, bare-metal development environment** capable of writing code, inspecting repositories, running shell commands, executing compilers, and performing automated debugging directly on an Android device (smartphones, tablets, foldables, and Samsung DeX desktop setups).

### Intended Users
- **Mobile Software Engineers:** Developers requiring on-device code generation, repository maintenance, bug fixing, and script execution without hauling laptops.
- **Researchers & Agentic AI Practitioners:** Engineers exploring mobile-native LLM agent workflows, local tool execution, and Android system automation.
- **Hardware Tinkerers & Power Users:** Developers utilizing rooted Android devices or Shizuku ADB environments to automate system configuration, package management, and device testing.

---

## 2. Core Workflows & Features

```
+-----------------------------------------------------------------------------------+
|                                 USER INTERFACE                                    |
|   Chromium WebView Single Page Application (React) & Native Splash Overlay       |
+-----------------------------------------------------------------------------------+
                                         │
                        Local Loopback HTTPS (127.0.0.1:45157)
                                         ▼
+-----------------------------------------------------------------------------------+
|                             LANGUAGE SERVER CORE                                  |
|     Patched AArch64 ELF Binary & Glibc Loader (liblanguage_server.so)             |
|     Autonomous Agent Planning, Multi-file Tool Calls, Gemini API Integration       |
+-----------------------------------------------------------------------------------+
                  │                                               │
      System Gateway (127.0.0.1:45160)                    PTY Terminal Spawn
                  ▼                                               ▼
+-----------------------------------+             +---------------------------------+
|      ANDROID SYSTEM GATEWAY       |             |     NATIVE BIONIC USERSPACE     |
|  - Toast, Vibrator, Device Info   |             |  - Bash, Python 3, Git, Node.js |
|  - Android Status Notifications   |             |  - Termux-compatible 'pkg'      |
|  - Shizuku (ADB uid=2000) Bridge  |             |  - ARMv8.0 LSE Atomics Compat   |
|  - Native Root (su uid=0) Bridge  |             |  - Background Daemon micro-cron |
+-----------------------------------+             +---------------------------------+
```

1. **Autonomous Code Generation & Editing:** The agent plans multi-step coding objectives, reads and writes files across external storage (`/storage/emulated/0/Projects/`) and internal userspace, and iterates on build outputs.
2. **Interactive Terminal & Tool Execution:** Runs native shell tools (`python3`, `git`, `bash`, `curl`, `clang`) inside native Android Bionic userspace without virtualization or container overhead.
3. **Elevated System Access:**
   - **Shizuku Bridge (`rish`):** Dispatches ADB-level system shell operations (`uid=2000`) for non-rooted devices to query packages, run logcat, and inspect system services.
   - **Native Root Bridge (`su`):** Out-of-the-box superuser execution (`uid=0`) with auto-detection for KernelSU, Magisk, and APatch.
4. **Adaptive Viewport & Desktop Mode:** Touch-first responsive mobile interface with an instant toggle for desktop layout (optimized for tablets, external displays, and Samsung DeX).
5. **Background Persistence (`agy-daemon`):** Micro-daemon running under Android Foreground Service WakeLock to execute delayed tasks, cron schedules, file-change watchers, and notifications even when the screen is off.
6. **Built-in Package Manager (`pkg`):** Ultra-fast native package manager installing development toolchains directly from curated Android repositories.

---

## 3. Application Metadata & SDK Targets

| Attribute | Specification | Technical Rationale |
| :--- | :--- | :--- |
| **Package Name (Release)** | `com.agy` | Standard production application identifier. |
| **Package Name (Dev)** | `com.agy.dev` | Isolated debuggable package for concurrent installation. |
| **Version Name** | `2.4.2` | Production release version. |
| **Version Code** | `18` | Incremental Android version code. |
| **Minimum SDK** | `24` (Android 7.0 Nougat) | Baseline requirement for modern Android Java APIs and ARM64 support. |
| **Target SDK** | `28` (Android 9.0 Pie) | **Strict Technical Requirement:** Android 10+ (API 29+) enforces W^X (Write XOR Execute) memory restrictions on app-private storage directories (`/data/user/0/<pkg>/files`), blocking `execve` execution of dynamically unpacked ELF binaries. Targeting API 28 preserves the platform capability to execute the core runtime directly. |
| **Architecture** | ARM64 / `aarch64` | Native 64-bit ARM execution. |
| **CPU Baseline** | ARMv8.0-A+ | Transparent atomic instruction emulation via `liblse_compat.so` for Snapdragon 685 / Cortex-A53. |

---

## 4. Entry Points & Android Manifest Components

### Activities
- **`com.agy.ui.MainActivity`**  
  *Exported: false, launchMode: singleTask.*  
  Primary container activity. Initializes WebView host, themes, native bridges (`AndroidBridge`, `RootBridge`, `ShizukuBridge`), and manages the startup splash screen until the web UI signals completion.
- **`com.agy.ui.launcher.LauncherDarkActivity`**  
  *Exported: true, category: LAUNCHER.*  
  Default entry point for dark theme. Enables dynamic launcher icon switching without restarting the process.
- **`com.agy.ui.launcher.LauncherLightActivity`**  
  *Exported: true, category: LAUNCHER.*  
  Alternative entry point for light theme (toggled programmatically via `PackageManager`).

### Services
- **`com.agy.core.CoreServerService`**  
  *Exported: false, foregroundServiceType: dataSync.*  
  Hosts and supervises the background `language_server` process, acquires Android partial wake lock, runs the `SystemGatewayServer`, and spawns the `agy-daemon`.

### Broadcast Receivers
- **`com.agy.runtime.TorchReceiver`**  
  *Exported: false, action: `com.agy.ACTION_TORCH`.*  
  Hardware flashlight bridge allowing terminal CLI scripts (`torch on/off`) to control device camera LED.

### Content Providers
- **`com.agy.util.AppFileProvider`**  
  *Exported: false, grantUriPermissions: true.*  
  Extends `androidx.core.content.FileProvider` to share captured logs, diagnostics, and workspace artifacts securely with external viewer apps.

---

## 5. Android Permissions Rationale

| Permission | Purpose & Justification |
| :--- | :--- |
| `INTERNET` | Communication with local loopback servers (127.0.0.1) and outbound HTTPS to Google Gemini / OAuth endpoints. |
| `ACCESS_NETWORK_STATE` | Monitoring active network connectivity (Wi-Fi, Mobile) to handle offline states and DNS switches. |
| `READ_EXTERNAL_STORAGE` | Accessing source code repositories on shared storage on Android 7.0 - 9.0. |
| `WRITE_EXTERNAL_STORAGE` | Writing generated code and assets to shared storage on Android 7.0 - 9.0. |
| `MANAGE_EXTERNAL_STORAGE` | Full read/write access to project repositories located in `/storage/emulated/0/Projects` on Android 11+. |
| `FOREGROUND_SERVICE` | Running `CoreServerService` continuously in the background without Android OS freezing. |
| `WAKE_LOCK` | Keeping the CPU active during long-running builds, compilation, and autonomous agent tasks. |
| `POST_NOTIFICATIONS` | Sending status notifications (build results, daemon alerts, completion events) on Android 13+. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Requesting battery whitelist to prevent aggressive OEM process killing during autonomous sessions. |
| `RECEIVE_BOOT_COMPLETED` | Optional background service initialization upon device restart. |
| `CAMERA` | Controlling the hardware flashlight (torch) LED via CameraManager API. |
| `com.agy.permission.INTERNAL_BROADCAST` | Signature-level permission ensuring internal broadcast intents are never spoofed by third-party apps. |

---

## 6. Network Communication & External APIs

1. **Local Loopback Communication (Strictly 127.0.0.1):**
   - **Port `45157` (Release) / `45158` (Dev):** Fixed HTTPS port for gRPC-Web and language server communication. Protected by a runtime-generated CSRF token.
   - **Port `39475`:** Internal HTTP listener spawned by the core engine.
   - **Port `45160` (Release) / `45161` (Dev):** Android System Gateway HTTP server accepting JSON commands from shell/Python scripts.
2. **External Google Cloud APIs:**
   - `https://daily-cloudcode-pa.googleapis.com`: Upstream language model invocation, code completions, and agent tool execution.
   - `https://www.googleapis.com/oauth2`: Google OAuth2 token validation and profile info.
   - `https://feedback-pa.googleapis.com`: Optional user feedback submission.
3. **Package Distribution:**
   - Termux official package mirrors for `pkg update` and `pkg install` (HTTPS).

---

## 7. Data Storage & Filesystem Topology

```text
/data/user/0/com.agy/files/                # $HOME: Application private storage
├── usr/                                   # $PREFIX: Linux userspace rootfs (bin/, lib/, etc/)
├── runtime/                               # Language server binaries, glibc, and SSL certs
│   ├── bin/liblanguage_server.so          # Patched core executable
│   ├── bin/libloader.so                   # Dynamic linker (glibc ld-linux-aarch64.so.1)
│   ├── lib/                               # Glibc shared libraries (.so)
│   ├── certs/ca-certificates.crt          # SSL CA bundle
│   └── seed/                              # Onboarding presets and AGENTS.md
├── logs/                                  # Flight recorder logs, core.log, gateway.port
├── .gemini/                               # AI State, conversation transcripts, OAuth tokens
│   ├── jetski-standalone-oauth-token      # Persisted OAuth session token
│   ├── config/config.json                 # Execution policy & turbo defaults
│   └── antigravity-app/brain/             # Agent task memory & conversation transcripts
└── /storage/emulated/0/Projects/          # User project workspaces (external shared storage)
```

---

## 8. Technology Stack Summary

- **Android Host:** Java 17, Android Support / Bionic libc, Android API 28 Target.
- **Agent Core:** Google Antigravity / JetSki Go ELF binary (`language_server`), Glibc 2.38 AArch64 loader.
- **Frontend UI:** Chromium Android WebView, React SPA, Tailwind CSS, PrismJS, PDF.js.
- **Userspace & CLI:** Bionic Shell, GNU Coreutils, Bash 5.3, Python 3.11, AWK.
- **Build Toolchain:** Standalone custom builder (Python 3, `aapt`, `javac`, Android D8 Dexer, custom `align.py`, `apksigner`).

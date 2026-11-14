<div align="center">

  <img src="docs/assets/logo.png" alt="Antigravity for Android" width="88" height="88" style="border-radius: 20px;" />

  # Antigravity for Android
  ### *Build Anywhere. Anytime.*

  **Autonomous AI Software Engineering Environment & Native Bionic Userspace for ARM64**

  <sub>No PRoot &bull; No Emulation &bull; Bare-Metal Hardware Performance &bull; Independent Userspace</sub>

  [![Android](https://img.shields.io/badge/Android-7.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://android.com)
  [![Arch](https://img.shields.io/badge/ARM64-v8.0--A%2B-blue?style=flat-square)](https://arm.com)
  [![Release](https://img.shields.io/badge/Version-2.5.0-blueviolet?style=flat-square)](https://github.com/mengalpratik/antigravity-android/releases)
  [![License](https://img.shields.io/badge/License-Apache_2.0-orange?style=flat-square)](LICENSE)
  [![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20A%20Coffee-Support-FFDD00?style=flat-square&logo=buy-me-a-coffee&logoColor=black)](https://buymeacoffee.com/pratikmengal)
  [![UPI](https://img.shields.io/badge/UPI-iampratikmengal--2%40okaxis-008080?style=flat-square&logo=google-pay&logoColor=white)](upi://pay?pa=iampratikmengal-2@okaxis&pn=Pratik%20Mengal&cu=INR)

</div>

> [!NOTE]
> **Enterprise-Grade Mobile AI Engineering:**
> - **Native Bionic Execution:** Executes bare-metal with zero virtualization or PRoot overhead, delivering unthrottled CPU and storage throughput.
> - **Autonomous Agentic Coding:** Driven by Gemini AI models — multi-file code refactoring, terminal tool execution, and automated test-driven debugging.
> - **Complete Development Toolchain:** Bundled Python 3, Node.js, Git, Bash, C/C++ toolchains, SQLite, and the native `pkg` package manager.
> - **Adaptive Desktop & Touch Workspaces:** Tailored for mobile smartphones, tablet multi-window, and full Samsung DeX desktop workstations.

---

> [!IMPORTANT]
> **Independent Community Project Disclaimer:**  
> Antigravity for Android is an independently maintained project led by **Pratik Mengal**. It is **not** an official Google product and is not affiliated with, sponsored by, or endorsed by Google LLC. "Google", "Antigravity", "Android", and related trademarks belong to their respective owners.

---

## 1. Project Introduction

**Antigravity for Android** brings autonomous agentic software development directly to mobile and portable ARM64 devices. 

Unlike conventional approaches that rely on heavy virtualization, chroot containers, or slow PRoot translation, Antigravity executes **bare-metal inside native Android userspace (Bionic)**. This unleashes the full read/write throughput of physical UFS storage and hardware CPU cores. Driven by Gemini AI models, the agent plans software tasks, edits code across multiple project files, executes shell tools, and fixes build issues directly on-device.

---

## 2. Feature Overview

- **Centralized Workspace Management (Architecture A):**
  - **Logical Separation:** Project planning, architecture designs, research, audits, and AI collaboration records are organized centrally in `Internal Storage/Antigravity/Workspace/<ProjectName>/`, keeping the source tree clean.
  - **Dedicated Source Directories:** Default-created projects reside in `Internal Storage/Antigravity/Projects/<ProjectName>/`, while custom external projects remain untouched at their original authorized locations.
  - **Extensible Resource Categories:** Built-in directories for project, documentation, and architecture templates (`Templates/`), exports and backups (`Exports/`), and shared guidelines and references (`Shared/`).
  - **Structured Project Documentation:** Automated initialization of standard metadata (`project.yaml`), project context (`PROJECT_CONTEXT.md`), active task tracking (`TASKS.md`), and organized subdirectories (`planning/`, `architecture/`, `research/`, `reports/`, `collaboration/`, `sessions/`).
- **Unified SQLite Project Registry:**
  - Backed by an internal SQLite database (`antigravity_projects.db`) managed via `ProjectRegistryRepository`.
  - Tracks unique, immutable UUID project identities, source-to-workspace bindings, schema versions (`1.0.0`), active/archived lifecycle status, and human-readable `project.yaml` manifests.
- **Three Flexible Project Workflows:**
  - **Quick Project:** 1-tap creation of a default project in `Antigravity/Projects/` with synchronized documentation in `Antigravity/Workspace/`.
  - **Import Existing Project:** Register any external directory without modifying, moving, or duplicating source files; initializes centralized workspace documentation automatically.
  - **Reconnect Project:** Detect and reconnect previously registered projects whose storage mount or source paths changed, preserving project UUIDs and full documentation history.
- **System Gateway Workspace REST APIs:**
  - Built-in HTTP endpoints on `127.0.0.1:45160` (`/workspace/list`, `/workspace/quick-create`, `/workspace/import`, `/workspace/reconnect`, `/workspace/archive`, `/workspace/restore`, `/workspace/remove`, `/workspace/verify`).
- **Autonomous Agentic Coding:** Plans development tasks, edits multiple files across project trees, runs terminal commands, and resolves compiler errors automatically.
- **Native Bionic Userspace:** Operates directly on Android ARM64 without container or emulation overhead.
- **Built-in Package Manager (`pkg`):** Install full developer runtimes (`python3`, `git`, `nodejs`, `clang`, `bash`) directly from curated Android repositories.
- **Elevated Control (Root & Shizuku):**
  - **Native Root (`su`):** Out-of-the-box support for KernelSU, Magisk, and APatch for privileged device operations.
  - **Shizuku Bridge (`rish`):** Dispatches ADB-level system shell operations (`uid=2000`) on non-rooted devices.
- **Adaptive Viewports & Desktop Mode:** Touch-first interface for phones, with an instant multi-column layout toggle for tablets, foldables, and Samsung DeX.
- **Background Micro-Daemon (`agy-daemon`):** Runs autonomous background tasks, file-change watchers, cron schedules, and notification alerts under Android Foreground Service Partial WakeLock.
- **Hardware Flashlight Bridge (`torch`):** Integrated hardware LED control utility for diagnostic indicators.
- **In-Place Seamless Upgrades:** Built with a consistent signing keystore, allowing upgrades across releases without losing workspace data or configuration.

---

## 3. Architecture Overview

```text
[ Chromium WebView (React Frontend) ]
                ▲
                │  Local Loopback HTTPS (127.0.0.1:45157)
                ▼
[ Language Server Core (Go ELF + glibc loader + liblse_compat.so) ]
       ▲                                          ▲
       │ System Gateway (127.0.0.1:45160)         │ Native PTY Terminal
       ▼                                          ▼
[ Android System Gateway (app/) ]         [ Native Bionic Userspace (env/) ]
  - Centralized Workspace & Registry       - Bash 5.3, Python 3, Git, Node.js
  - SQLite Database (antigravity_projects.db) - pkg package manager
  - Status Notifications & Shizuku/Root    - agy-daemon background tasks
  - Safe File & Media Pickers              - Storage Root: /storage/emulated/0/Antigravity/
```

### 4.1. Centralized Workspace Hierarchy

```text
Antigravity/
│
├── Workspace/
│   ├── ProjectAlpha/
│   │   ├── project.yaml                 # Human-readable project manifest & schema
│   │   ├── PROJECT_CONTEXT.md           # Scope, architecture, tech stack, constraints
│   │   ├── TASKS.md                     # Active task tracker, completed milestones
│   │   ├── planning/                    # Requirements, RFCs, milestone roadmaps
│   │   ├── architecture/                # System diagrams, schemas, API contracts
│   │   ├── research/                    # Benchmarks, tech evaluations, spike notes
│   │   ├── reports/                     # Test summaries, security audits, reviews
│   │   ├── collaboration/               # Agent prompt instructions, handoff notes
│   │   └── sessions/                    # Chronological agent session summaries
│   │
│   └── AnotherProject/
│
├── Projects/                            # Default location for new source projects
│   ├── ProjectAlpha/                    # Clean source code repository
│   └── AnotherProject/
│
├── Templates/                           # Reusable project & doc templates
│   ├── project-templates/
│   ├── documentation-templates/
│   └── architecture-templates/
│
├── Exports/                             # Exported reports, archives & backups
│   ├── reports/
│   ├── documentation-packages/
│   └── backups/
│
└── Shared/                              # Shared developer knowledge & guidelines
    ├── references/
    ├── resources/
    └── common-guidelines/
```

For a comprehensive technical breakdown of component interactions and database schema, see [ARCHITECTURE.md](ARCHITECTURE.md) and [docs/CENTRALIZED_WORKSPACE_PLAN.md](docs/CENTRALIZED_WORKSPACE_PLAN.md).

---

## 4. Technology Stack

- **Android Host Shell:** Java 17, Android Support / Bionic libc, Target SDK 28, Min SDK 24.
- **Agent Core Engine:** Google Antigravity / JetSki Go ELF binary (`language_server`), Glibc 2.38 AArch64 dynamic loader.
- **Frontend Layer:** Chromium Android WebView, React SPA, Tailwind CSS, PrismJS, PDF.js.
- **Userspace & CLI:** Bionic Userspace, GNU Coreutils, Bash 5.3, Python 3.11, AWK.
- **Hardware Emulation:** `liblse_compat.so` (transparent C library emulating ARMv8.1-A Large System Extensions atomics on ARMv8.0-A CPUs).
- **Build System:** Self-contained Gradle-free builder (`modules/builder/` using Python 3, `aapt`, `javac`, D8, and `apksigner`).

---

## 5. Installation Instructions

### System Requirements
- **Device:** Android ARM64 (`aarch64`) smartphone, tablet, or handheld.
- **Android Version:** Android 7.0 (API 24) or higher.
- **Storage:** Minimum 500 MB free space for the base environment; 2 GB+ recommended for user development toolchains (`python`, `node`, `git`).
- **RAM:** 3 GB minimum; 4 GB+ recommended.

---

## 6. APK Installation Instructions

1. Download the latest release package from [GitHub Releases](https://github.com/mengalpratik/antigravity-android/releases):
   - **`antigravity.apk`** &mdash; Standard production release (recommended).
   - **`antigravity-region-bypass.apk`** &mdash; Production release with account eligibility gate bypass.
2. Install the APK on your device.
3. Open **Antigravity**. Upon first launch, grant **All Files Access** (`MANAGE_EXTERNAL_STORAGE`) so the agent can access your project folders (automatically structured under `Internal Storage/Antigravity/`).
4. *(Optional)* If using Shizuku for elevated operations, grant Shizuku permission when prompted.
5. Open the terminal and install standard development packages:
   ```bash
   pkg update
   pkg install git python nodejs
   ```

---

## 7. Build from Source Instructions

Antigravity uses a lightweight, Gradle-free build pipeline that can be executed on any standard Linux environment or directly on an Android device:

```bash
# Clone the repository
git clone https://github.com/mengalpratik/antigravity-android.git
cd antigravity-android

# Build the standard production APK (outputs dist/antigravity.apk):
bash build.sh

# Build the region bypass production APK:
bash build.sh --region-bypass

# Run all 6 modular test suites:
bash test_all_modules.sh
```

For complete step-by-step instructions, dependency setup, and CI configurations, see [BUILD_GUIDE.md](BUILD_GUIDE.md).

---

## 8. Permissions Explanation

| Permission | Technical Requirement & Justification |
| :--- | :--- |
| `INTERNET` | Loopback communication with internal core servers (127.0.0.1) and outbound HTTPS to Gemini / Google Cloud APIs. |
| `ACCESS_NETWORK_STATE` | Monitors active Wi-Fi and mobile connectivity for resilient API polling. |
| `MANAGE_EXTERNAL_STORAGE` | Full read/write access to project workspaces and code in `/storage/emulated/0/Antigravity/` (`Workspace/`, `Projects/`, `Templates/`, `Exports/`, `Shared/`) on Android 11+. |
| `READ_EXTERNAL_STORAGE` | File access on Android 7.0 &ndash; 9.0 devices. |
| `WRITE_EXTERNAL_STORAGE` | Writing generated code and assets on Android 7.0 &ndash; 9.0 devices. |
| `FOREGROUND_SERVICE` | Runs the core server process continuously in the background without OS freezing. |
| `WAKE_LOCK` | Holds partial CPU wake lock during long compilation or autonomous coding tasks. |
| `POST_NOTIFICATIONS` | Displays status notifications and task completion alerts on Android 13+. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Requests battery whitelist to prevent aggressive vendor background termination. |
| `CAMERA` | Controls the hardware flashlight (torch) LED via CameraManager. No camera frames are accessed. |
| `INTERNAL_BROADCAST` | Signature-protected permission securing internal application events from external apps. |

---

## 9. Security and Privacy Information

- **Target SDK 28 & W^X Enforcement:** Android 10+ (API 29+) forbids executing dynamically unpacked binaries from app-private storage (`/data/user/0/<pkg>/files`). Targeting API 28 allows the app to execute its runtime natively while maintaining strict internal file permission isolation (`0700`).
- **Loopback Binding (127.0.0.1):** All internal services (`language_server` on port `45157`, `SystemGatewayServer` on port `45160`) bind strictly to localhost. External network interfaces reject all incoming connections.
- **CSRF Protection:** All local HTTP/gRPC requests are validated against a dynamic runtime CSRF token.
- **Production Hardening:** Production release builds strictly omit `android:debuggable="true"`, preventing unauthorized `run-as` access to session tokens.
- **Sensitive Data Redaction:** All logs exported or rendered through the UI pass through automated redaction filters (`LogRedactor.java`, `SensitiveDataRedactor.java`) that scrub OAuth tokens, API keys, and authorization headers.

For detailed security disclosures, see [SECURITY.md](SECURITY.md).

---

## 10. Known Limitations

- **Google Play Store Policy:** Because the application targets API 28 to support direct native binary execution from app storage, it is distributed independently via GitHub Releases and cannot be published to the Google Play Store under current Play target API requirements.
- **Shared Storage `noexec`:** Android mounts `/storage/emulated/0` with the `noexec` mount flag. Shell scripts stored on external storage cannot be executed directly via `./script.sh` and must be invoked with their explicit interpreter (`bash script.sh`, `python script.py`).
- **ARM64 Only:** Only 64-bit ARM (`aarch64`) architecture is supported. 32-bit ARM (`armeabi-v7a`) and x86 devices are not supported.

---

## 11. Contribution Instructions

Contributions, bug reports, and pull requests are welcome!
- Check existing issues before opening a new bug report.
- Ensure all test suites pass by running `bash test_all_modules.sh` prior to submitting pull requests.
- For guidelines on commit formatting, code standards, and branch policies, see [CONTRIBUTING.md](CONTRIBUTING.md).

---

## 12. Support & Sponsorship ☕

If you enjoy using **Antigravity for Android** and want to support ongoing development, maintenance, and community upkeep, consider buying me a coffee or donating via UPI:

<div align="center">
  <br/>
  <a href="https://buymeacoffee.com/pratikmengal" target="_blank">
    <img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" height="48" style="border-radius: 8px;" />
  </a>
  &nbsp;&nbsp;
  <a href="upi://pay?pa=iampratikmengal-2@okaxis&pn=Pratik%20Mengal&cu=INR">
    <img src="https://img.shields.io/badge/UPI-iampratikmengal--2%40okaxis-008080?style=for-the-badge&logo=google-pay&logoColor=white" height="42" alt="Pay via UPI" />
  </a>
  <br/><br/>
  <img src="https://raw.githubusercontent.com/mengalpratik/antigravity-android/main/docs/assets/upi-qr.png" alt="Scan to Pay via UPI" width="220" style="border-radius: 12px; border: 1px solid rgba(128,128,128,0.2);" />
  <br/><br/>
  <p><b>Scan with Google Pay, PhonePe, Paytm, BHIM or any UPI app</b></p>
  <p><code>iampratikmengal-2@okaxis</code></p>
  <br/>
  <p><i>Every contribution helps keep the project independent, open-source, and actively maintained! 🚀</i></p>
</div>

---

## 13. License Information

This project is licensed under the [Apache License 2.0](LICENSE).  
Copyright &copy; 2026 Pratik Mengal and project contributors.

*(Notice: Portions of the bundled runtime assets, including third-party libraries and upstream tools, are subject to their respective open-source licenses as documented in the repository.)*

# Security Policy: Antigravity for Android

> **Security Considerations, Threat Model & Vulnerability Disclosure Procedure**  
> *Project Lead:* Pratik Mengal  
> *Independent Community Project &mdash; Not an official Google product and not affiliated with Google LLC.*

---

## 1. Reporting Security Vulnerabilities

We take the security of Antigravity for Android seriously. If you discover a security vulnerability, we appreciate your efforts to coordinate responsible disclosure.

### Reporting Procedure
- **Private Disclosure:** Please do **NOT** disclose vulnerabilities publicly in GitHub Issues or discussions.
- **Reporting Contact:** Submit security reports directly to the project maintainer, **Pratik Mengal**, via email or via a [GitHub Private Security Advisory](https://github.com/mengalpratik/antigravity-android/security/advisories/new).
- **Report Details:** Please include:
  1. A clear description of the vulnerability.
  2. Proof of Concept (PoC) steps or exploit scenario.
  3. Affected component (`app/`, `modules/core`, `modules/patches`, `modules/io`, `env/`, `web/`).
  4. Device environment (Android OS version, Root/Shizuku state, device architecture).

We acknowledge receipt of reports within 48 hours and strive to issue a patched release promptly.

---

## 2. Supported Versions

| Version | Supported | Status |
| :--- | :--- | :--- |
| **v2.4.x** (v2.4.2+) | **Yes** | Active security support and bug fixes |
| **v2.3.x** and earlier | No | Superseded by v2.4 modular architecture |

---

## 3. Threat Model & Security Boundaries

### 3.1. Target SDK 28 Rationale & W^X Memory Protection
- **Platform Constraint:** Beginning with Android 10 (API 29), Android enforces W^X (Write XOR Execute) memory restrictions on app-private storage directories (`/data/user/0/<pkg>/files`). Native ELF executables cannot be invoked via `execve` from writable app data if the app targets API 29+.
- **Architectural Solution:** Antigravity targets **API 28 (Android 9.0 Pie)**. This allows the application to dynamically unpack and execute the glibc loader (`libloader.so`), the language server (`liblanguage_server.so`), and user-installed development runtimes (`python3`, `git`, `bash`) without requiring root access or container virtualization.
- **Security Boundary:** Because API 28 is targeted, the application implements its own rigorous internal access controls, file permissions (`0700` on private directories), and cryptographic manifest checks (`manifest.json`) to prevent untrusted code execution.

### 3.2. Local Loopback Isolation (Strictly 127.0.0.1)
- **Attack Vector:** Unauthorized external devices on the same local Wi-Fi network attempting to send commands to the language server or system gateway.
- **Mitigation:**
  - `language_server` (port `45157`), `SystemGatewayServer` (port `45160`), and the internal HTTP listener (port `39475`) **bind strictly to localhost (`127.0.0.1`)**.
  - All external network interfaces (`0.0.0.0`, Wi-Fi IP, cellular IP) reject incoming connections.
  - The language server validates a secret runtime-generated CSRF token (`--csrf_token`) on all incoming requests.

### 3.3. Production vs. Debuggable Builds (`android:debuggable`)
- **Vulnerability Addressed (Issue #6):** An APK with `android:debuggable="true"` permits local shell users or malicious apps with ADB access to run `run-as com.agy`, exposing stored OAuth session tokens and private project data.
- **Mitigation:**
  - The production release builds (`dist/antigravity.apk` and `dist/antigravity-region-bypass.apk`) **strictly omit `android:debuggable="true"`**.
  - Debuggable flags are strictly isolated to the distinct package `com.agy.dev` (`dist/antigravity-dev.apk`).

### 3.4. Elevated Privileges (Root & Shizuku)
- **Shizuku (`rish`):** Requires the user to explicitly approve ADB-level shell permissions (`uid=2000`) through the official Shizuku Android app.
- **Native Root (`su`):** Out-of-the-box root operations (`uid=0`) require authorization from the user's root manager (KernelSU, Magisk, or APatch). All elevated command executions are recorded in the internal ring buffer for auditability.

### 3.5. Sensitive Data Redaction & Telemetry
- **Token Protection:** The core OAuth token is stored at `$HOME/.gemini/jetski-standalone-oauth-token` in internal private storage (`chmod 600`).
- **Redaction Engine:** All logs streamed through `LogRedactor.java` and `SensitiveDataRedactor.java` automatically scrub OAuth bearer tokens, API keys, CSRF tokens, and credentials matching known security patterns before log export or crash reporting.

---

## 4. Permissions Security Analysis

| Permission | Scope | Security Mitigation |
| :--- | :--- | :--- |
| `MANAGE_EXTERNAL_STORAGE` | Shared storage | Restricts default project creation to `/storage/emulated/0/Antigravity/Projects/`. User files outside the Antigravity centralized workspace are accessed only upon explicit user workspace selection. |
| `INTERNET` | Full network | Internal servers strictly bind to `127.0.0.1`. Outbound network requests are limited to verified HTTPS endpoints (Google Gemini APIs and Termux package mirrors). |
| `FOREGROUND_SERVICE` | Background execution | Running as a foreground service with persistent user notification ensures users are always aware when the core engine is active. |
| `CAMERA` | Hardware LED | Used exclusively for flashlight (torch) toggle via CameraManager API. Never opens camera preview frames or captures imagery. |
| `INTERNAL_BROADCAST` | IPC security | Defined with `protectionLevel="signature"` so only components signed with the same keystore can trigger internal broadcasts. |

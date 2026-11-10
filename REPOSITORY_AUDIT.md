# Complete Repository Audit & Open-Source Preparation Report

> **Comprehensive Technical Audit, Risk Analysis, Cleanup & Verification Evidence**  
> *Project Lead:* Pratik Mengal  
> *Date of Audit:* November 10, 2026  
> *Independent Community Project &mdash; Not an official Google product and not affiliated with Google LLC.*

---

## 1. Executive Summary

This repository audit was conducted to transition **Antigravity for Android** (`antigravity.apk`, package `com.agy`) into an independently maintained, professional open-source project ready for public review and distribution by **Pratik Mengal**.

The audit verified application architecture, tested all subsystem modules, performed static security and secret scans, validated Git configuration and .gitignore behaviors, resolved a test isolation issue in the environment test suite, and prepared all required open-source governance files.

---

## 2. Repository Structure & Directory Audit

| Path | Purpose | Audit Verdict |
| :--- | :--- | :--- |
| `app/` | Android native host shell (Java 17, targetSdk 28). | Clean, well-structured. No hardcoded debug flags in production. |
| `config/` | Environment definitions (`app.env`, `paths.env`, `tools.env`). | Clean single-source-of-truth constants. No hardcoded secrets. |
| `docs/` | Screenshots, banners, and diagrams for documentation. | High-quality assets present in both dark and light modes. |
| `env/` | Native Bionic userspace rootfs, `pkg` manager, `liblse_compat.so`. | Tested and functional. Test isolation issue resolved. |
| `modules/builder/` | Gradle-free APK builder pipeline. | Operational and deterministic. |
| `modules/core/` | Pristine language server archive, glibc loader, SSL certs. | Cryptographic SHA-256 manifest verified byte-for-byte. |
| `modules/patches/` | Binary byte-level patch engine. | 8 patch groups verified and tested. |
| `modules/io/` | High-performance I/O Bus (FIFO streams, event broker). | Clean IPC architecture. Unit tests passing. |
| `modules/env/` | Diagnostics, CLI bridge, telemetry logging. | Unit tests passing. |
| `modules/runner/` | Standalone runtime preparation, probe server, E2E tests. | E2E integration test passing in 0.19s. |
| `tools/` | Pre-compiled compile jars (`android.jar`, `r8.jar`, `debug.keystore`). | Tracked in Git for reproducible builds. Verified. |
| `web/` | Web frontend UI (Chromium WebView React SPA). | Verified. Desktop Mode and theme sync operational. |
| `build/` | Temporary build directory (classes, obj, intermediate APKs). | Properly excluded by `.gitignore`. Not tracked in Git. |
| `dist/` | Distribution output directory (`antigravity.apk`). | Properly excluded by `.gitignore`. Not tracked in Git. |

---

## 3. Git Configuration & Safety Audit

### Current Git State
- **Branch:** `main`
- **Remote Origin:** `https://github.com/werdio325-png/antigravity-android`
- **History Status:** Ahead of `origin/main` by 2 commits (`e2f5400`, `41e13d9`).
- **Safety Policy:**
  - No commits were rewritten or altered.
  - No branches were deleted.
  - No force pushes were performed.
  - Existing uncommitted user changes in `OnboardingSeeder.java`, `web/patch_main.py`, and `web/generated/main.js` were preserved in full.

### .gitignore Verification
Verified that build artifacts (`build/`, `dist/`, `cache/`, `*.apk`, `*.dex`, `*.class`), Python caches (`__pycache__/`, `*.pyc`), and IDE files (`.idea/`, `.vscode/`, `.DS_Store`) are properly ignored by Git while preserving required inputs like `modules/core/pristine/language_server.tar.gz`.

---

## 4. Security & Privacy Audit

### 4.1. Secret Scan Results
A recursive pattern scan was conducted across all source files, scripts, and configuration files for credentials, private keys, API tokens, and authorization headers:
- **Zero user credentials, OAuth tokens, or private signing keys were found** in tracked files.
- The only matched string was the upstream Google Client API key (`AIzaSyCNwssj18yx5z0YgvDoBCiewnY_xSXyaWk`) inside the bundled web frontend (`web/main.js`) used for optional client-side feedback submission (`feedback-pa.googleapis.com`). This is a public client identifier inherent to the web bundle.
- Release signing keys are strictly read from environment variables (`AGY_RELEASE_KEYSTORE`, `AGY_RELEASE_KEY_PASS`) and are not committed.

### 4.2. Sensitive Data Redaction
Verified that `LogRedactor.java` and `SensitiveDataRedactor.java` intercept all application logs to redact OAuth tokens, CSRF tokens, and API keys before outputting to log files or crash logs.

### 4.3. Privilege Boundaries
- **Shizuku (`rish`):** Dispatched only through user-authorized Shizuku IPC (`uid=2000`).
- **Native Root (`su`):** Dispatched only with user superuser approval (`uid=0`). Command execution is audited in the internal ring buffer.

---

## 5. Verification & Test Evidence

Every test runner in the repository was executed on native ARM64:

```text
1. modules/core/tests/run_all.sh       ==> 6 PASS, 0 FAIL
2. modules/patches/tests/run_all.sh    ==> 4 PASS, 0 FAIL
3. modules/io/tests/run_all.sh         ==> 4 PASS, 0 FAIL
4. modules/env/tests/run_all.sh        ==> 5 PASS, 0 FAIL
5. modules/builder/tests/run_all.sh    ==> 2 PASS, 2 SKIPPED, 0 FAIL
6. modules/runner/test_e2e.sh (Live)   ==> HTTP 400 Probe Response in 0.19s, PASS
7. env/tests/selftest                  ==> 39 PASS, 0 FAIL
8. env/tests/invariant                 ==> 7 PASS, 0 FAIL
9. env/tests/e3                        ==> 4 PASS, 0 FAIL
10. web/tests/selftest.sh              ==> 8 Bundles Generated, PASS
```

### Bug Fix Applied during Audit:
- **`env/tests/selftest`:** When running on a live Android device with `CoreServerService` active, `http://127.0.0.1:45160/gateway` responded immediately to notification requests, preventing `agy-notify` from exercising its `rish` fallback code path and causing two test assertions to fail. The test was modified to provide a non-listening gateway port (`45199`) in the test fixture's isolated `$NOTIFY_TMP/logs/gateway.port`. Following this fix, `env/tests/selftest` passed with `== failures: 0 ==`.

---

## 6. Risk Assessment & Legal Considerations

| Risk Category | Details | Recommended Mitigation |
| :--- | :--- | :--- |
| **Trademark & Naming** | The name "Antigravity" is associated with Google's agentic AI project. | Prominently display the independent community disclaimer in README, documentation, and app metadata: *"Independent community project &mdash; Not an official Google product."* |
| **Google Play Target SDK** | Google Play requires targetSdk 34+. Antigravity requires targetSdk 28 to execute binaries from app storage. | Distribute APKs via GitHub Releases and F-Droid / Obtainium rather than Google Play. |
| **Binary Licensing** | The core `language_server` binary is proprietary Google software distributed within the app bundle. | Retain Apache 2.0 for the host wrapper, tools, and scripts, while clearly identifying the bundled third-party core in documentation and release notes. |

---

## 7. Strategic Recommendations for Pratik Mengal

1. **Licensing Confirmation:**
   - The repository currently contains the **Apache License 2.0**.
   - **Recommendation:** Maintain Apache 2.0 for all wrapper code, build scripts, native Java classes, and userspace tools. Clearly state that upstream binary components retain their original copyright.
2. **Branding & Project Identity:**
   - Keep the package name `com.agy` and title "Antigravity for Android".
   - Retain the clear open-source disclaimer established in the new `README.md`.
3. **Public Distribution Strategy:**
   - Utilize **GitHub Releases** for publishing signed APKs (`antigravity.apk` and `antigravity-region-bypass.apk`) accompanied by `SHA256SUMS.txt`.
   - Maintain the dual-edition model: Standard Edition for standard Google account workflows; Region Bypass Edition for users requiring account eligibility bypass.
   - Do not commit APK binaries to the Git tree; automate APK publishing via `.github/workflows/build-and-release.yml` on release tags (`v*`).
4. **Localization Follow-up:**
   - Translate the Russian language module README files in `modules/*` into English in a future milestone.

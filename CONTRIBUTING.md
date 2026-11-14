# Contributing to Antigravity for Android

Thank you for your interest in contributing to **Antigravity for Android**!  
This project is an independently maintained, open-source autonomous development environment for Android ARM64, led by **Pratik Mengal**.

---

## 1. Development Principles & Code of Conduct

- **Respect & Professionalism:** All participants are expected to adhere to our [Code of Conduct](CODE_OF_CONDUCT.md).
- **Documentation First:** Any change to architecture, flags, environment variables, or CLI tools must be accompanied by corresponding updates to documentation.
- **No Redundant Dependencies:** Keep the application lightweight. Avoid introducing heavy external frameworks or unnecessary libraries.
- **Preserve Stability:** Existing uncommitted work, user projects, and test invariants must never be broken.

---

## 2. Getting Started

### 2.1. Fork & Clone
```bash
git clone https://github.com/mengalpratik/antigravity-android.git
cd antigravity-android
```

If developing on Android shared storage (`/storage/emulated/0/...`), configure Git's safe directory:
```bash
export GIT_CONFIG_NOSYSTEM=1
git config --global --add safe.directory "*"
```

### 2.2. Branching Strategy
- **`main`:** Stable production branch. Release tags (`v2.4.x`) are cut directly from `main`.
- **`dev`:** Active development integration branch.
- **Feature Branches:** Create descriptive branch names originating from `dev`:
  - `feat/feature-name`
  - `fix/bug-description`
  - `docs/documentation-update`

---

## 3. Coding Standards

### 3.1. Java (Android Host Layer in `app/`)
- Target **Java 17** syntax compatible with Android API 24&ndash;28.
- Never add `android:debuggable="true"` to production manifest files.
- Ensure all native resources (processes, streams, wake locks) are properly managed in lifecycle callbacks (`onDestroy`, `stopSelf`).

### 3.2. Python (`modules/`, `env/`, `web/`)
- Target **Python 3.10+**.
- Use explicit type annotations and docstrings for public utility functions.
- Avoid external third-party pip dependencies in core build scripts; rely on Python standard library modules (`sys`, `os`, `shutil`, `tempfile`, `hashlib`, `json`).

### 3.3. Shell Scripts (`bash`, `sh`)
- Use explicit `set -euo pipefail` in orchestrator scripts.
- Write shell scripts compatible with native Android Bionic userspace (`/system/bin/sh` and `/data/user/0/com.agy/files/usr/bin/bash`).
- Quote all paths and variables to handle spaces in Android directories.

---

## 4. Verification & Testing Requirements

Before opening a pull request, you **must** run the test suites and confirm all checks pass:

```bash
# 1. Master test suite across all 6 core subsystems:
bash test_all_modules.sh

# 2. Environment rootfs & package manager selftest:
bash env/tests/selftest
bash env/tests/invariant
bash env/tests/e3

# 3. Web bundle & patch verification:
bash web/tests/selftest.sh

# 4. Verify clean build:
bash build.sh
```

---

## 5. Commit Guidelines & Pull Requests

### Conventional Commits
Please format commit messages using [Conventional Commits](https://www.conventionalcommits.org/):
- `feat(component): add new feature`
- `fix(component): resolve bug or crash`
- `docs(module): update documentation`
- `chore(release): bump version or clean dependencies`
- `refactor(subsystem): restructure code without changing behavior`

### Submitting a Pull Request
1. Push your branch to your GitHub fork.
2. Open a Pull Request targeting the **`dev`** branch.
3. Provide a clear summary of changes, problem solved, and verification evidence (test outputs).
4. Maintainers will review your PR and provide feedback.

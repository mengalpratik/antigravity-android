# Reproducible Build Guide: Antigravity for Android

> **Complete Instructions for Compiling, Packaging, and Signing Antigravity APKs**  
> *Project Lead:* Pratik Mengal  
> *Independent Community Project &mdash; Not an official Google product and not affiliated with Google LLC.*

---

## 1. Overview & Build Philosophy

Antigravity for Android uses a **custom, lightweight, Gradle-free build system** located in `modules/builder/`. This architecture eliminates heavy Gradle daemon memory overhead and allows the entire application to be compiled either on standard Linux CI servers (Ubuntu) or directly on an Android device running in ARM64 userspace.

---

## 2. Host Prerequisites

### 2.1. Linux / Ubuntu (x86_64 or AArch64)

Install the required build tools and utilities:
```bash
# Update package lists
sudo apt-get update

# Install Android packaging tools and system utilities
sudo apt-get install -y aapt zipalign apksigner file zip unzip gzip curl

# Install Java 17 Development Kit
sudo apt-get install -y openjdk-17-jdk

# Install Python 3.10+
sudo apt-get install -y python3 python3-pip
```

### 2.2. Pre-Packaged Host Toolchain (`tools/`)
The repository contains or automatically fetches the required Java bytecode compilers and platform stubs:
- **`tools/android.jar`:** Android API 28 compile stubs.
- **`tools/r8.jar`:** Android D8/R8 bytecode compiler (v8.2.42).
- **`tools/debug.keystore`:** Embedded signing keystore for consistent development builds.

If absent, they are automatically fetched or can be downloaded manually:
```bash
# Download Android API 28 stubs
curl -fsSL -o tools/android.jar "https://github.com/Sable/android-platforms/raw/master/android-28/android.jar"

# Download D8/R8 compiler
curl -fsSL -o tools/r8.jar "https://dl.google.com/dl/android/maven2/com/android/tools/r8/8.2.42/r8-8.2.42.jar"
```

---

## 3. Building Release APKs

### 3.1. Standard Release Edition (`dist/antigravity.apk`)
Builds the production APK configured with original Google authentication and account gates:
```bash
bash build.sh
```
- **Output:** `dist/antigravity.apk`
- **Application ID:** `com.agy`
- **Target Port:** `45157`
- **Debuggable:** `false`

### 3.2. Region Bypass Release Edition (`dist/antigravity-region-bypass.apk`)
Builds the production APK with the account eligibility gate bypass enabled in the core binary patcher:
```bash
bash build.sh --region-bypass
```
- **Output:** `dist/antigravity-region-bypass.apk`
- **Application ID:** `com.agy`
- **Target Port:** `45157`
- **Debuggable:** `false`

### 3.3. Fast Iteration Build (`--fast`)
Skips heavy asset re-compression during development cycles:
```bash
bash build.sh --fast
```

### 3.4. Developer Debug Edition (`dist/antigravity-dev.apk`)
Builds an isolated debug package that can be installed side-by-side with the standard release APK:
```bash
bash build.sh --dev
```
- **Output:** `dist/antigravity-dev.apk`
- **Application ID:** `com.agy.dev`
- **Target Port:** `45158`
- **Debuggable:** `true`

---

## 4. Verification & Testing

### 4.1. Run Full Modular Test Suite
Run the master test runner to execute unit and integration tests across all 6 subsystems:
```bash
bash test_all_modules.sh
```
This script executes:
1. `modules/core/tests/run_all.sh`: ELF validation, unpacker, and manifest verification.
2. `modules/patches/tests/run_all.sh`: Binary byte replacer, PTY patcher, and patch pipeline.
3. `modules/io/tests/run_all.sh`: FIFO pipes, stream splitter, and event broker.
4. `modules/env/tests/run_all.sh`: Path resolver, diagnostics, and CLI tests.
5. `modules/builder/tests/run_all.sh`: Builder syntax and configuration generation tests.
6. `modules/runner/test_e2e.sh`: Live end-to-end server launch, probe, and graceful shutdown.

### 4.2. Verify APK Integrity & Signatures
Inspect the generated APK signatures using Android's `apksigner`:
```bash
apksigner verify --verbose dist/antigravity.apk
```
Expected output:
```text
Verifies
Verified using v1 scheme (JAR signing): false
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): true
```

### 4.3. Generate Cryptographic Checksums
```bash
cd dist
sha256sum antigravity.apk antigravity-region-bypass.apk > SHA256SUMS.txt
cat SHA256SUMS.txt
```

---

## 5. Troubleshooting & Known Build Caveats

### 1. `aapt` Crashes with SIGABRT (Exit Code 134) on ARM64
When building inside native Android ARM64 userspace, `aapt` requires the `ANDROID_DATA` environment variable to be explicitly set. `modules/builder/02_r_java.sh` and `06_package.sh` include this export automatically:
```bash
export ANDROID_DATA="${ANDROID_DATA:-/data}"
```

### 2. `Permission denied` on Android Shared Storage
Android mounts `/storage/emulated/0` with the `noexec` mount flag. Executing `./build.sh` directly will fail with exit code 126. Always execute scripts via their explicit shell interpreter:
```bash
bash build.sh
python3 script.py
```

### 3. Git `dubious ownership` on Shared Storage
If running git operations inside `/storage/emulated/0/Antigravity/...` (or legacy `/storage/emulated/0/Projects/...`), configure git's safe directory exception:
```bash
export GIT_CONFIG_NOSYSTEM=1
git config --global --add safe.directory "*"
```

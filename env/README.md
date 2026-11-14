# Antigravity Userspace Environment (`env/`)

> [!NOTE]
> This document specifies the rootfs architecture, package management subsystem (`pkg`), dynamic prefix rewriting engine, and hardware compatibility layers in Antigravity for Android.

---

## 1. Rootfs Composition & Architecture

The `env/` directory houses a lightweight, self-contained **ARM64 Bionic userspace** designed to execute directly within the Android application sandbox without requiring superuser permissions (`root`) or slow syscall translation emulators (`proot` / `ptrace`).

### Directory Layout

```
env/
├── README.md               # Technical specification and architecture reference
├── README                  # Concise summary reference
├── build.sh                # Packaging script assembling assets into dist/env
├── bootstrap/              # Curated base rootfs archive and prefix shims
├── overlay/                # System overlays copied into $PREFIX (etc, bin/rish, etc.)
├── pkg/                    # Native 'pkg' package manager and shell libraries
└── tests/                  # Offline selftest, invariant, and integrity test suites
```

---

## 2. Dynamic Prefix Rewriting

Because Android assigns isolated app paths based on application package ID and user profile (`/data/user/<user-id>/<package-id>/files/usr`), paths cannot be hardcoded. Antigravity handles this dynamically:

1. **Install-Time Rewriting:**
   - `TermuxShebangRewriter`, `TermuxPrefixRewriter`, and `TermuxSymlinkFixer` dynamically repoint binary and script paths to the active `$PREFIX`.
   - Text rewrites cover `$PREFIX/{bin,libexec,etc,share,var}`; symlinks are recalculated atomically.
   - The package manager (`pkg`) automatically applies dynamic prefix rewriting to all newly installed packages.
2. **Third-Party Script Path Correction (`agy-fix-paths`):**
   - Package installers like `pip` and `npm` often bake hardcoded interpreter paths into console scripts. Running `agy-fix-paths` traverses the active prefix and updates shebangs idempotently.

---

## 3. Package Management (`pkg`)

Antigravity includes a lightweight native package manager written in pure Bash and AWK:

```bash
# Update local package indexes
pkg update

# Install development packages
pkg install git python nodejs bash

# Search and query packages
pkg search <pattern>
pkg list
pkg installed

# Run automated environment diagnostics
pkg selftest
```

---

## 4. Hardware Compatibility & Atomics

- **ARMv8.0-A LSE Emulation:** Legacy 64-bit processors (such as Qualcomm Snapdragon 660) lack native ARMv8.1-A Large System Extensions (LSE) atomic instructions (`ldaddal`, `swpal`). Antigravity pre-loads `liblse_compat.so` via `LD_PRELOAD`, emulating missing atomic instructions through runtime signal traps and fallback locks without crashing.
- **Shared Storage `noexec` Compliance:** Android mounts external storage (`/storage/emulated/0`) with the `noexec` flag. Compiled binaries reside strictly in `$PREFIX/bin`, while external scripts are invoked through their explicit interpreters (`python script.py`, `bash script.sh`).

---

## 5. Testing & Diagnostics

Execute the userspace verification suite:

```bash
bash env/tests/selftest.sh
```

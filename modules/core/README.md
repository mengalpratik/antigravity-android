# Antigravity Core Module (`modules/core`)

> Base runtime distribution module for Antigravity v2: archive extraction, AArch64 ELF/ABI validation, glibc runtime assembly, system shims, and cryptographic manifest generation.

---

## 1. Scope & Responsibilities

The `modules/core` subsystem serves as the runtime foundation for Antigravity on Android ARM64 (`aarch64`). It delivers the language server binary, bundled glibc runtime libraries, SSL certificates, configuration state, and toolchains for:

1. **Extraction & Permissions (`unpack/`):** Detects archive formats (`tar.gz` or raw ELF) and extracts `language_server` with correct POSIX execution bits.
2. **Binary Validation (`validate/`):** Verifies SHA-256 checksums, validates 64-bit Little-Endian ELF headers, verifies target architecture (`e_machine == 183` / `EM_AARCH64`), and confirms position-independent execution (`ET_DYN`/`ET_EXEC`).
3. **Cryptographic Manifesting (`manifest/`):** Streams chunked SHA-256 calculation (1 MB buffers), generates canonical `manifest.json`, and enforces integrity across runtime assets.
4. **Runtime Bundle Assembly (`bundle/`):** Assembles an isolated directory structure (`bin/`, `lib/`, `certs/`, `seed/`) consumed by `modules/runner` and `modules/builder`.
5. **System Shims (`shims/`):** Emulates standard desktop Linux tools (`xdg-open`, browser triggers, Shizuku `rish`).

---

## 2. Directory Structure

```
modules/core/
├── spec/                       # Declarative core specifications
│   ├── version.env             # Version tags, build hashes, dynamic loader name
│   ├── checksums.json          # Reference SHA-256 hashes and binary sizes
│   ├── glibc_deps.list         # Required glibc shared libraries (.so)
│   └── shims.list              # Shims deployed into runtime bin/
├── pristine/                   # Upstream pristine core archive
│   └── language_server.tar.gz  # Pristine aarch64 language_server binary
├── unpack/                     # Extraction and file permission management
│   ├── archive_detector.py     # Format detector (tar_gz, elf, unknown)
│   ├── extractor.py            # Stream extraction from tar.gz or raw ELF
│   └── permissions.py          # POSIX permission enforcement (+x / +r)
├── validate/                   # ELF and cryptographic verifiers
│   ├── elf_header.py           # Magic bytes (\x7fELF), 64-bit class, LE endianness
│   ├── elf_arch.py             # AArch64 architecture validation (EM_AARCH64 = 183)
│   ├── elf_compat.py           # ELF type validation (ET_EXEC / ET_DYN)
│   ├── checksum_checker.py     # Streaming SHA-256 file validation
│   └── verify_pipeline.py      # End-to-end multi-stage validation pipeline
├── manifest/                   # Manifest generation and verification
│   ├── file_hasher.py          # Chunked streaming hasher (1 MB blocks)
│   ├── manifest_builder.py     # Directory scanning and manifest.json assembly
│   └── manifest_verifier.py    # Runtime directory integrity verifier
├── bundle/                     # Final runtime assembly
│   ├── layout.py               # Directory hierarchy initialization
│   ├── pack_binaries.py        # Core binary, linker, and shim symlink packaging
│   ├── pack_libraries.py       # glibc shared object packaging and networking configs
│   ├── pack_certs.py           # SSL/TLS CA certificate bundle packaging
│   ├── pack_seed.py            # Initial seed state and workspace policies
│   └── assemble.py             # CLI & programmatic runtime orchestrator
├── glibc/                      # AArch64 glibc runtime libraries
│   ├── ld-linux-aarch64.so.1   # Dynamic linker (ELF program interpreter)
│   ├── libc.so.6, libm.so.6... # Standard glibc shared libraries
│   ├── liblse_compat.so        # LSE atomic instruction emulator for legacy CPUs
│   └── etc/                    # Network configs (hosts, nsswitch, resolv)
├── certs/                      # Security certificates
│   └── ca-certificates.crt     # System CA certificate store
├── seed/                       # Initial configuration templates
│   ├── AGENTS.md               # Agent behavior rules and system prompt
│   └── *.pbtxt                 # Initial onboarding protobuf state
└── tests/                      # Automated module tests
```

---

## 3. Testing & Verification

Execute all unit and integration tests:

```bash
python3 -m unittest discover -s modules/core/tests
```

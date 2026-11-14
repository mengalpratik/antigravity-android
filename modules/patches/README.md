# Antigravity Core Binary Patcher (`modules/patches`)

Targeted binary patching subsystem for the Antigravity core binary (`language_server`) on **Android ARM64 (aarch64)**. It circumvents platform constraints (Android seccomp filters, Bionic libc syscall differences, missing ARMv8.1-A LSE atomics on ARMv8.0-A hardware) and integrates seamlessly with the userspace runtime.

---

## 1. Architecture & Directory Layout

```
modules/patches/
├── pipeline.py              # CLI orchestrator & pipeline execution API
├── spec/
│   └── patch_manifest.json  # Expected patch specifications and policies
├── engine/                  # Low-level analysis and binary modification engines
│   ├── elf_scanner.py       # ELF64/AArch64 parser and .text section locator
│   ├── pclntab_scanner.py   # Go pclntab function table parser & MOVZ encoder
│   ├── byte_replacer.py     # In-place byte pattern search and replacement
│   ├── reporter.py          # JSON reporting for applied patches
│   └── patch_context.py     # Execution context and state model
├── groups/                  # Functional patch implementations
│   ├── gates/               # CLI gate and Enterprise Auth bypasses
│   ├── syscalls/            # Replacement of seccomp-blocked faccessat2/fchmodat2
│   ├── sigill/              # SIGILL signal handler preservation (liblse_compat)
│   ├── resolv/              # /etc/resolv.conf path redirection
│   └── failfast/            # ARMv8.0-A CPU feature suppression & atomic shims
└── tests/                   # Unit and integration test suites
    ├── test_byte_replacer.py
    ├── test_elf_scanner.py
    ├── test_pclntab_scanner.py
    ├── test_full_pipeline.py
    └── run_all.sh
```

---

## 2. Patch Groups

| Group | Module | Target & Signature | Action |
|---|---|---|---|
| **gates** | `cli_gate` | `ldrb w1, [x0,#8]` in CLI gate verification | Replaced with `mov w1, #1` (`21 00 80 52`) |
| **gates** | `enterprise_auth` | `EnterpriseAuthValidator.Validate` | Enforces validity flag unconditionally |
| **syscalls** | `faccessat` | `syscall.faccessat2` (sys_439: `0xd28036e0`) | Retargeted to `faccessat` (sys_48: `0xd2800600`) |
| **syscalls** | `fchmodat` | `syscall.fchmodat2` (sys_452: `0xd2803880`) | Retargeted to `fchmodat` (sys_53: `0xd28006a0`) |
| **sigill** | `sigill_handler` | Go runtime `rt_sigaction` invocation | Preserves `liblse_compat.so` signal handler by shielding `SIGILL` (4) |
| **resolv** | `resolv_redirect` | `/etc/resolv.conf` string (16 bytes) | Rewritten to `etc//resolv.conf` preserving exact length |
| **failfast** | `crash_suppressor` | ARMv8.0-A CPU check & preinit atomic | Branches past hardware LSE checks and shims `ldaddal` with standard load/store pairs for legacy ARMv8.0-A SoC support (Snapdragon 660, etc.) |

---

## 3. Analysis Engine Highlights

1. **`elf_scanner.py`**:
   - Validates `e_ident` header offsets with support for nested/packed ELF containers.
   - Stripped binary support (`e_shnum == 0`): automated fallback to Program Headers (`PT_LOAD` with `PF_X`).
   - Boundary-checked segment offsets and sizes for `.text`, `.shstrtab`, and section header tables.
2. **`pclntab_scanner.py`**:
   - Direct function lookup in `funcnametab` string tables.
   - Internal symbol resolution cache (`_cache`) avoiding repetitive parsing across syscall hooks.
3. **Zero-Copy Processing**:
   - Zero intermediate memory allocations during binary scans using direct Python buffer protocols (`bytearray` / `memoryview`).

---

## 4. Testing & Verification

Run the complete test suite:

```bash
bash modules/patches/tests/run_all.sh
```

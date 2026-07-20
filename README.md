## Aegis

a secure app :)

An offline-first Android password manager built for a Mobile Security capstone project. The assignment: build a working app, deliberately plant a set of security vulnerabilities, scan and document them, then fix every one and prove it with a before/after comparison.

Aegis stores accounts, usernames, URLs, and passwords behind a PIN/biometric gate, in a local vault with no cloud dependency — no Firebase, no network permission, nothing leaves the device.

> ⚠️ **The `v1.0-insecure` build is intentionally vulnerable.** Do not install it as a real password manager or store real credentials in it. See [v1.0-insecure — the planted vulnerabilities](#v10-insecure--the-planted-vulnerabilities) below.

| | |
|---|---|
| **Platform** | Native Android, Java |
| **Min SDK** | 24 (Android 7.0) |
| **Target SDK** | 36 |
| **UI** | Material 3, dark navy/gold "vault" theme |
| **Storage** | SQLCipher (AES-256) + Android Keystore |
| **Auth** | PIN (PBKDF2-hashed) + optional fingerprint (`BiometricPrompt`) |

---

### Table of contents

- [Features](#features)
- [Releases](#releases)
- [Architecture](#architecture)
- [Screens](#screens)
- [v1.0-insecure — the planted vulnerabilities](#v10-insecure--the-planted-vulnerabilities)
- [v2.0-secure — the fixes](#v20-secure--the-fixes)
- [MobSF scan evidence](#mobsf-scan-evidence)
- [Building and running locally](#building-and-running-locally)
- [Project structure](#project-structure)
- [Known limitations / accepted risks](#known-limitations--accepted-risks)
- [Team](#team)

---

### Features

- **PIN gate + biometrics** — first-run PIN setup, returning-user unlock, optional fingerprint shortcut via `androidx.biometric`.
- **Vault dashboard** — masked passwords by default, per-row reveal toggle, one-tap copy to clipboard, empty state, extended FAB to add a credential.
- **Full credential CRUD** — add, view, edit, delete (account name, URL, username, password).
- **Change PIN** — from the vault's toolbar overflow menu, requires the current PIN.
- **Encrypted local storage** — SQLCipher-backed vault database, keyed by an AES-256 key generated and held in the hardware-backed Android Keystore (v2 only — see below).
- **Failed-attempt lockout** — 5 wrong PINs locks the gate for 30 seconds (v2 only).
- **Custom shield launcher icon** and a consistent dark/gold Material 3 theme throughout.

### Releases

| Release | Description | Link |
|---|---|---|
| `v1.0-insecure` | Fully working app with 6 deliberately planted security flaws | [Release](https://github.com/MidActionJax/Aegis/releases/tag/v1.0-insecure) · [PR #1](https://github.com/MidActionJax/Aegis/pull/1) |
| `v2.0-secure` | Same app, all 6 flaws fixed, re-scanned with MobSF | *(coming up)* |

Each release has the installable APK and the full MobSF report attached as assets.

---

### Architecture

```
MainActivity (PIN/biometric gate)
      │
      ▼
VaultActivity (dashboard)
   │        │
   ▼        ▼
EntryFormActivity   CredentialDetailActivity
(add / edit)        (view / edit / delete)
   │
   ▼
ChangePinActivity
```

**Packages:**

| Package | Responsibility |
|---|---|
| `com.example.projectaegis` (root) | Activities: `MainActivity`, `VaultActivity`, `EntryFormActivity`, `CredentialDetailActivity`, `ChangePinActivity`, `CredentialAdapter`, `SecureActivity` (shared base class) |
| `.data` | `Credential` (model), `VaultDbHelper` (SQLCipher-backed `SQLiteOpenHelper`), `VaultRepository` (singleton CRUD) |
| `.auth` | `PinManager` — PIN hashing, verification, lockout |
| `.security` | `VaultKeyProvider` — generates/wraps the vault's AES-256 key via Android Keystore |
| `.util` | `ClipboardUtil` — copy-with-auto-wipe helper |

This mirrors the architecture in the project's original threat model document (Mobile Client → Local Storage → External Android System Services), minus the Firebase Cloud Sync component, which was cut early — see [Known limitations](#known-limitations--accepted-risks).

---

### Screens

| Screen | Purpose |
|---|---|
| **Auth gate** | First-run: create a 4–6 digit PIN. Returning: enter PIN or use fingerprint. |
| **Vault dashboard** | List of saved credentials, masked passwords, reveal/copy per row, toolbar menu (lock vault / change PIN), FAB to add. |
| **Add / Edit credential** | Account name, URL, username, password. |
| **Credential detail** | View/reveal/copy a single credential; edit or delete via toolbar menu. |
| **Change PIN** | Requires current PIN before accepting a new one. |

---

### v1.0-insecure — the planted vulnerabilities

Six flaws were planted on purpose, each tagged `INSECURE (v1, planted flaw)` at its exact location in the code, mapped back to the project's Week 6 threat model:

| # | Flaw | Where | Threat model row |
|---|---|---|---|
| 1 | Plaintext SQLite database — no encryption at rest | `VaultDbHelper` | Local Device Cache |
| 2 | Clipboard copy never auto-clears | `VaultActivity` / `CredentialDetailActivity` `onCopyPassword` | Android Clipboard |
| 3 | No `FLAG_SECURE` — passwords visible in Recents thumbnail/screenshots | All activities | Android OS Interface |
| 4 | `android:debuggable="true"`, `android:allowBackup="true"`, 4 internal activities marked `exported="true"` | `AndroidManifest.xml` | Manifest hardening |
| 5 | Sensitive `Log.d` calls leak the raw PIN attempt and copied passwords | `MainActivity`, `VaultActivity`, `CredentialDetailActivity` | Sensitive logging |
| 6 | Master PIN stored as plaintext in SharedPreferences, no hashing, no lockout | `PinManager` | Replaces the dropped Firebase-backup threat row |

Firebase Cloud Sync (the original threat model's optional stretch goal) was dropped entirely — the app is fully offline. That's why flaws #4 and #6 replace what were originally the "Cloud Sync MitM" and "Firebase Backup Storage leak" rows: MobSF is much better at catching on-device issues than network-layer ones, so every planted flaw stays inside what the required scanning tool can actually verify.

MobSF static analysis score on v1: **42/100**. Full report and findings mapping: [`docs/security/mobsf-v1-findings-summary.md`](docs/security/mobsf-v1-findings-summary.md).

---

### v2.0-secure — the fixes

Each flaw above was fixed 1:1:

| # | v1 flaw | v2 fix |
|---|---|---|
| 1 | Plaintext SQLite | **SQLCipher** (AES-256), key generated and held in the **Android Keystore** (`VaultKeyProvider`) — the raw key never leaves the hardware-backed keystore in plaintext; only an AES-GCM-wrapped copy is persisted. |
| 2 | Clipboard never clears | **30-second auto-wipe** after copy (`ClipboardUtil`), only clearing if the clipboard still holds the exact value we copied. |
| 3 | No `FLAG_SECURE` | Set via a shared `SecureActivity` base class every screen extends — Recents thumbnail and screenshots no longer show vault contents. |
| 4 | Manifest exposure | `android:debuggable` removed (AGP sets it correctly per build type now), `allowBackup="false"`, all 4 internal activities set `exported="false"`. |
| 5 | Sensitive logging | The log lines were deleted outright. |
| 6 | Plaintext PIN, no lockout | PIN is hashed with **PBKDF2WithHmacSHA256** (120,000 iterations) + a random per-install salt; **5 failed attempts → 30-second lockout**, wired into both the unlock screen and Change PIN. |

That's **7 total security features** in the finished app (biometric auth + PIN gate already existed, plus these 5), comfortably past the capstone's "at least 2 security features" requirement.

Full before/after MobSF comparison, including which findings are real fixes vs. scanner limitations (MobSF can't statically see a 30-second clipboard timer or a `FLAG_SECURE` window flag): [`docs/security/mobsf-v2-findings-summary.md`](docs/security/mobsf-v2-findings-summary.md).

---

### MobSF scan evidence

All scans were run against the official [`opensecurity/mobile-security-framework-mobsf`](https://hub.docker.com/r/opensecurity/mobile-security-framework-mobsf) Docker image. Reports live in [`docs/security/`](docs/security/):

| File | What it is |
|---|---|
| `mobsf-v1-insecure-report.pdf` | Full MobSF report, v1 |
| `mobsf-v1-findings-summary.md` | v1 findings mapped to each planted flaw |
| `mobsf-v2-secure-debug-report.pdf` | Full MobSF report, v2 debug build |
| `mobsf-v2-secure-release-report.pdf` | Full MobSF report, v2 **release** build (needed to verify the `debuggable`/`allowBackup`/`exported` fixes, since debug builds are always debuggable by design) |
| `mobsf-v2-findings-summary.md` | v2 before/after comparison, fix-by-fix |
| `*-raw.json` | Raw MobSF API scan output backing each report |

---

### Building and running locally

Requires Android Studio (or just the JDK + Android SDK) and an emulator/device on **API 24+**.

```bash
git clone https://github.com/MidActionJax/Aegis.git
cd Aegis
./gradlew assembleDebug
```

The debug APK lands in `app/build/outputs/apk/debug/app-debug.apk`. Install with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

No API keys, `local.properties` secrets, or network permissions are required — the app is fully offline.

> **Note:** if you're switching between an older build of this repo and the current one, uninstall the app first rather than reinstalling over it — the vault database format changed between v1 (plaintext) and v2 (SQLCipher), and there's no in-place migration path (see [Known limitations](#known-limitations--accepted-risks)).

---

### Project structure

```
app/src/main/java/com/example/projectaegis/
├── MainActivity.java              # PIN/biometric auth gate
├── SecureActivity.java            # shared FLAG_SECURE base class
├── VaultActivity.java             # dashboard
├── EntryFormActivity.java         # add/edit credential
├── CredentialDetailActivity.java  # view/edit/delete credential
├── ChangePinActivity.java         # change PIN
├── CredentialAdapter.java         # RecyclerView adapter
├── auth/
│   └── PinManager.java            # PIN hashing, verification, lockout
├── data/
│   ├── Credential.java            # model
│   ├── VaultDbHelper.java         # SQLCipher SQLiteOpenHelper
│   └── VaultRepository.java       # singleton CRUD wrapper
├── security/
│   └── VaultKeyProvider.java      # Keystore-backed vault key
└── util/
    └── ClipboardUtil.java         # copy-with-auto-wipe

docs/security/                     # MobSF reports + findings summaries (v1 and v2)
```

---

### Known limitations / accepted risks

- **No Firebase / cloud sync** — cut early to keep the app fully offline and every planted flaw within MobSF's static-analysis reach. See [v1.0-insecure](#v10-insecure--the-planted-vulnerabilities) above.
- **minSdk 24** — MobSF flags this as installable on an "unpatched" Android 7.0. Accepted for compatibility scope; not remediated.
- **No v1→v2 database migration** — switching from plaintext to SQLCipher storage means the old database format is unreadable by the new code. A real product would need a migration path; for this capstone (two separate releases, not an in-place upgrade), a clean reinstall between versions is the expected flow.
- **Unsigned release build** — no release signing config is configured; the release-variant MobSF scan (used only to verify manifest-level fixes) shows "Missing Code Signing certificate," which is a build-artifact gap, not an app vulnerability. The APK shipped in each GitHub Release is the debug build, consistent between v1 and v2 for a fair comparison.

---

### Team

| Name | Role |
|---|---|
| Jaxon Doolittle | Architecture & implementation |
| Keenan Johnson | Security analysis & threat modeling |

Built for GSW University's Mobile Security capstone (Weeks 6–8): threat model → insecure build → secure build, with MobSF-backed evidence at each stage.

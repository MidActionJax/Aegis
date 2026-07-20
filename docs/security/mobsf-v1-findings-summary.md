# MobSF Static Scan — v1.0-insecure

Scanned `app-debug.apk` (v1, intentionally insecure) with MobSF (Docker, `opensecurity/mobile-security-framework-mobsf`).
Full report: `mobsf-v1-insecure-report.pdf`. Raw API JSON: `mobsf-v1-insecure-raw.json`.

MobSF security score: **42/100**.

## Findings mapped to planted vulnerabilities

| Planted flaw (see code comments tagged `INSECURE (v1, planted flaw)`) | MobSF finding | Severity |
|---|---|---|
| Plaintext SQLite storage (`VaultDbHelper`) | "App uses SQLite Database and execute raw SQL query... sensitive information should be encrypted" | Warning |
| Clipboard never auto-cleared (`VaultActivity`/`CredentialDetailActivity` `onCopyPassword`) | "This App copies data to clipboard. Sensitive data should not be copied to clipboard" | Info |
| `android:debuggable="true"` | "Debug Enabled For App [android:debuggable=true]" | **High** |
| `android:allowBackup="true"` | "Application Data can be Backed up [android:allowBackup=true]" | Warning |
| Unnecessary `android:exported="true"` on internal activities | "Activity (VaultActivity / EntryFormActivity / CredentialDetailActivity) is not Protected. [android:exported=true]" | Warning (x3) |
| Sensitive `Log.d` calls (PIN, passwords) | "The App logs information. Sensitive information should never be logged." | Info |
| Plaintext PIN in SharedPreferences (`PinManager`) | "Files may contain hardcoded sensitive information like usernames, passwords, keys etc." | Warning |

## Bonus findings (not originally planned, worth mentioning in the report)

- **Debug-signed APK**: "Application signed with debug certificate" (High) — expected for a debug build; note in the report that the release build must use a real signing config.
- **minSdk 24**: "App can be installed on a vulnerable unpatched Android version 7.0" (High) — accepted risk for compatibility scope; not remediated in v2 unless you want to raise minSdk.

## Note for the reflection section

MobSF's static analysis does **not** flag the missing `FLAG_SECURE` (screenshot/Recents leakage) — that's a runtime window-flag issue static analysis can't see from the manifest or bytecode alone. We identified and will remediate it via manual code review / dynamic testing, not MobSF. Worth calling out in the report as a limitation of static-only scanning.

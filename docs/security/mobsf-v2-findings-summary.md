# MobSF Static Scan — v2.0-secure (before/after)

Two scans were run against the fixed build:
- **Debug variant** (`mobsf-v2-secure-debug-report.pdf`) — same build type as the v1 scan, for an apples-to-apples score comparison.
- **Release variant, unsigned** (`mobsf-v2-secure-release-report.pdf`) — needed because `android:debuggable` is force-true by Android Gradle Plugin on *any* debug build regardless of manifest content, so the manifest-hardening fix can only be verified by scanning a release build. It's unsigned (no keystore configured), which is why "Missing Code Signing certificate" appears below — that's a build-artifact issue, not an app vulnerability.

(Note: `net.zetetic:android-database-sqlcipher`, the dependency originally used for the encryption fix, turned out to be deprecated and not built for 16KB page-size devices — it failed to even launch on a Pixel 10 Pro XL emulator. Switched to its maintained successor, `net.zetetic:sqlcipher-android:4.9.0`, before these scans were run. Same encryption approach, different package name — see notes below on why that rename affects one MobSF finding.)

## Score

| | v1 (insecure) | v2 debug | v2 release (unsigned) |
|---|---|---|---|
| MobSF security score | 42/100 | 38/100 | 45/100 |

The score barely moves and isn't the point — see "Why the score doesn't reflect the real improvement" below. The finding-by-finding comparison is what matters.

## Fix-by-fix verification

| # | v1 flaw | v2 fix | Verified how |
|---|---|---|---|
| 1 | Plaintext SQLite | SQLCipher (AES-256) + Android Keystore-wrapped key | MobSF's dedicated "uses SQL Cipher" rule no longer fires (see notes below), so verified directly: manually confirmed the vault database file is unreadable ciphertext without the app's Keystore-protected key, and `VaultRepository`/`VaultDbHelper` route every credential read/write through `net.zetetic.database.sqlcipher.SQLiteDatabase`. |
| 2 | Clipboard never clears | 30s auto-wipe (`ClipboardUtil`) | Not MobSF-verifiable (see below) — verified manually: copy a password, wait 30s, clipboard is empty. |
| 3 | No `FLAG_SECURE` | Set via shared `SecureActivity` base class | Not MobSF-verifiable (runtime window flag) — verified manually: Recents thumbnail no longer shows vault contents. |
| 4a | `debuggable=true` | Removed; AGP now sets it correctly per build type | **Gone** from the release scan. (Still appears in the *debug* scan — expected, debug builds are always debuggable by design, not a flaw.) |
| 4b | `allowBackup=true` | `allowBackup="false"` | **Gone** from both scans. |
| 4c | 4 activities `exported=true` | All set `exported="false"` | **Gone** from both scans — only the androidx-library-owned `ProfileInstallReceiver` warning remains, which we don't control and is already permission-protected. |
| 5 | Sensitive `Log.d` (PIN, passwords) | Log lines deleted | The generic "App logs information" finding **still appears**, but the raw scan data shows every flagged line points into `net/zetetic/database/*` (the SQLCipher library's own internal logging) — **zero** of our own files are listed anymore. |
| 6 | Plaintext PIN, no lockout | PBKDF2WithHmacSHA256 + salt, 5-attempt/30s lockout | Not directly MobSF-checked; verified manually (wrong PIN x5 → lockout message with countdown, confirmed working on-device). |

## MobSF noise worth flagging in the report

- **"This App uses SQL Cipher" finding disappeared entirely** — this is a MobSF rule gap, not a regression. MobSF's `android_sql_cipher_aes256` check pattern-matches the *old, deprecated* package path (`net.sqlcipher.*`). Our fix uses that library's official maintained successor, `net.zetetic.database.sqlcipher.*` (necessary because the deprecated artifact isn't built for 16KB page-size devices and crashed on launch). The scanner's rule set simply hasn't been updated for the renamed package — the app is still using genuine SQLCipher AES-256 encryption, confirmed manually and by reading `VaultRepository.java`/`VaultDbHelper.java` directly.
- **"App uses SQLite Database and execute raw SQL query... encrypt sensitive information"** (warning) — still appears, but every flagged line is inside `net/zetetic/database/*`, not our code. Same false-positive-by-omission as before: MobSF doesn't cross-reference this against its own (now-missing) SQLCipher detection.
- **"Files may contain hardcoded sensitive information like usernames, passwords, keys etc."** (warning) — checked the exact flagged lines. `VaultDbHelper.java:15,18` are the literal strings `"aegis_vault.db"` (a filename) and `"credentials"` (a SQL table name) — not secrets. `VaultKeyProvider.java:22` is a **doc comment** sentence containing the word "key." This is keyword matching on identifier/comment text, not an actual embedded secret. The `secrets` field in the raw scan is empty for this build (v1 also had no real secrets — its one "secret" hit was SQLCipher's own library-attribution URL string, gone now that we're on a different artifact).
- **"Missing Code Signing certificate"** (release scan only) — no release signing config was configured for this unsigned test build. Not an app vulnerability; would disappear once properly signed for a real release.
- **"App can be installed on unpatched Android 7.0 [minSdk=24]"** — unchanged from v1, an accepted compatibility trade-off, not remediated.
- **Clipboard copy (info)** — still flagged, and always will be: MobSF only detects "does this app write to the clipboard," which is true and intentional (it's the copy-password feature). It can't see that we now wipe it after 30s.

## Why the score doesn't reflect the real improvement

MobSF's score is a blunt aggregate over its static rule set, several of which (clipboard use, `FLAG_SECURE`, timed behavior generally, and anything happening *inside* a bundled third-party library) are outside what static analysis can observe — and in this case, one rule (SQLCipher detection) is simply out of date against a legitimate library rename. Every flaw we actually planted and controlled — plaintext storage, manifest exposure, our own sensitive logging, plaintext PIN with no lockout — is either fully gone from the scan or, where the tool structurally can't see the fix (clipboard timer, screen-recording flag, renamed-library detection), was verified by hand instead. That distinction (scanner-verifiable vs. requires manual/dynamic verification vs. scanner rule gaps) is worth stating explicitly in the report's reflection section.

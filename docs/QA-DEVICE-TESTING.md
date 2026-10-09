# Isolated GoreeCloud Markdown QA device testing

**State:** Dedicated QA source variant and GitHub Actions QA build are implemented in the development branch. The exact-head CI result and physical-device acceptance must be independently checked after every change.

## Isolation

- Existing Development package: `com.goreecloud.markdown.dev`.
- Local default QA package: `com.goreecloud.markdown.qa` (retained for backwards-compatible local builds).
- CI bounded QA package: `com.goreecloud.markdown.qa<10 lowercase hex characters>` derived from the **exact source SHA**; its launcher label includes the same 10-character SHA prefix. This avoids signing collisions with earlier CI QA builds.
- The QA variant is separately installed, debuggable and uses temporary CI signing. It does not share the Development package's app-private editor state or SAF grants.
- Ephemeral CI/debug certificates are **not** a persistent, protected Development signing identity. Every new CI SHA uses a distinct QA package rather than claiming to be an update of the older one. Avoid accumulating old QA builds without a separately authorized cleanup plan. Never uninstall or clear the existing Development or QA apps merely to accommodate a new APK.

## Before installing

1. Confirm the authorized GitHub connection is GoreeCloud. Record exact branch commit SHA and a **successful** push workflow building and checking the QA APK at that SHA.
2. Verify the downloaded Actions artifact name encodes the exact **push** SHA and contains the expected `app-qa.apk`. Derive the expected package as `com.goreecloud.markdown.qa` plus the first ten lowercase hex characters of that SHA. Check APK SHA-256, derived package ID, display label with SHA prefix, permissions, and signing certificate before installation.
3. Check `adb devices -l`: the authorized Android handset must show `device`. Capture baseline Development package version, APK path and installation time **without reading its private data**.
4. Check whether the **exact derived per-SHA QA package** is already installed. If it is, stop until signing and upgrade eligibility are verified; do not uninstall, reset or force-update any installed package. The older `com.goreecloud.markdown.qa` may coexist.
5. If QA is absent and all checks passed, install **only** `app-qa.apk` for the selected device with `adb -s SERIAL install` (never a replacement of the Development package).
6. Launch QA and test with disposable synthetic Markdown only: compact Undo/Redo; Replace All confirmation/cancel/Unicode; live stats; editor focus and IME; SAF open/save/readback and read-only providers; preview safety and accessibility.
7. For every executed case, record exact source SHA, actual installed QA package, provider, observed result and redacted evidence in `docs/DEVICE-ACCEPTANCE.md`. Verify `.dev` and all earlier QA packages retain their pre-test installation state; report any already-missing package as an existing baseline condition.

Do not claim Stable, production, full Glaze/platform acceptance, or provider-safe atomic writes on the basis of an APK smoke test. Owner APK distribution requires the authorized GoreeCloud Google Drive first.

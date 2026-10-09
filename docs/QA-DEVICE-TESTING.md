# Isolated GoreeCloud Markdown QA device testing

**State:** Dedicated QA source variant and GitHub Actions QA build are implemented in the development branch. The exact-head CI result and physical-device acceptance must be independently checked after every change.

## Isolation

- Existing Development package: `com.goreecloud.markdown.dev`.
- Bounded CI QA package: `com.goreecloud.markdown.qa`, labeled **GoreeCloud Markdown QA**.
- The QA variant is separately installed, debuggable and uses temporary CI signing. It does not share the Development package's app-private editor state or SAF grants.
- Ephemeral CI/debug certificates are **not** a persistent, protected Development signing identity. They must never be treated as update-compatible across different builds. Never uninstall or clear the existing Development app to accommodate QA.

## Before installing

1. Confirm the authorized GitHub connection is GoreeCloud. Record exact branch commit SHA and a **successful** push workflow building and checking the QA APK at that SHA.
2. Verify the downloaded Actions artifact name encodes the exact SHA and contains the expected `app-qa.apk`. Check SHA-256 and APK package ID, display label, permissions, and signing certificate before installation.
3. Check `adb devices -l`: the authorized Android handset must show `device`. Capture baseline Development package version, APK path and installation time **without reading its private data**.
4. Check if `com.goreecloud.markdown.qa` is already installed. If it is, stop until its signing certificate and upgrade eligibility are verified; do not uninstall, reset or force-update an existing QA package.
5. If QA is absent and all checks passed, install **only** `app-qa.apk` for the selected device with `adb -s SERIAL install` (never a replacement of the Development package).
6. Launch QA and test with disposable synthetic Markdown only: compact Undo/Redo; Replace All confirmation/cancel/Unicode; live stats; editor focus and IME; SAF open/save/readback and read-only providers; preview safety and accessibility.
7. For every executed case, record source SHA, provider, observed result and redacted evidence in `docs/DEVICE-ACCEPTANCE.md`. Verify that `.dev` remains installed and intact after QA tests.

Do not claim Stable, production, full Glaze/platform acceptance, or provider-safe atomic writes on the basis of an APK smoke test. Owner APK distribution requires the authorized GoreeCloud Google Drive first.

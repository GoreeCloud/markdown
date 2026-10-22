# GoreeCloud Markdown

**Lifecycle:** Early development — no production or platform-conformance claim.

GoreeCloud Markdown is a standalone **native Android** application for reading, creating, editing, previewing, and managing Markdown files while preserving user ownership of ordinary open-format documents.

## Product direction

- Kotlin and Jetpack Compose, with a native Markdown preview (no WebView).
- Offline-first editing; no sign-in, advertising, analytics, or network permission.
- Android Storage Access Framework (SAF) for opening and creating user-selected files, without broad storage access.
- File integrity and recovery prioritized over silent background writes.
- Accessible, restrained user interface guided by GoreeCloud's Glaze design-and-experience system.
- Optional future integration with GoreeCloud Drive, Search, AI, and other approved capabilities, without making local use dependent on the ecosystem.
- Evaluate **all nine** GoreeCloud Integral Platform Systems against current approved contracts before any conformance or production claim.

## Present development baseline

The first implementation branch includes a document editor, preview, Open/New/Save/Save As, a dirty-document confirmation when changing documents, a size-limited strict UTF-8 reader, and a private recovery draft retained on a failed or unverified provider write. The platform's document provider still determines external-write atomicity; do **not** treat this as a guarantee of atomic replacement.

A limited OnePlus N200 / Android 16 smoke test verified that an **earlier installed Development build** cold-launches and exposes the native editor. The current development branch and isolated QA features still require representative-device validation, including Undo/Redo layout and IME. No production release or owner-compatible Development signing is authorized by that smoke test alone. See [docs/ROADMAP.md](docs/ROADMAP.md) and [docs/PLATFORM-CONFORMANCE.md](docs/PLATFORM-CONFORMANCE.md).

## Build (development)

Use JDK 17, Android SDK 36 and Gradle 8.11.1:

```sh
gradle :app:assembleDebug :app:assembleQa
gradle :app:testDebugUnitTest :app:testQaUnitTest
```

An independently installed Gradle is currently required; a verified Gradle wrapper/distribution is an outstanding setup obligation. Both Development debug and isolated QA APKs use temporary CI/debug signing and are **not** a persistent owner-installable Development channel or update-compatible signed releases. See [docs/QA-DEVICE-TESTING.md](docs/QA-DEVICE-TESTING.md) for non-destructive USB device checks.

## Repository boundary

Authoritative source: [GoreeCloud/markdown](https://github.com/GoreeCloud/markdown). No browser frontend, proprietary file container, forced cloud dependency, or WebView is part of the intended primary editor architecture.

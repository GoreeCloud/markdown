# GoreeCloud Markdown

**A local-first Markdown viewer and editor for Linux and Android.**

**Status:** 0.1.0 development preview. This public repository provides source, not an independently verified stable release or a downloadable production APK.

Built as an original Flutter workspace following [GoreeCloud UI](https://github.com/GoreeCloud/gc-ui): calm Canvas, comfortable Surface, floating Lens, Iris accent, rounded controls, generous whitespace, purposeful motion, responsive layouts and light/dark appearance.

## Capabilities

- Write and edit Markdown with native text selection and selection-aware Bold, Italic, Heading, List, Task, Quote, Code and Link tools.
- Read GitHub-flavored Markdown in a polished, selectable preview (headings, code, lists, tasks, blockquotes, tables and more).
- Desktop: side-by-side editor and preview. Compact screens: Edit/Preview switch.
- Create, Open, Save and Save As UTF-8 text. Prompt to save or discard changes before replacing an edited document.
- Android: native Storage Access Framework picker, system provider URIs, and compatible Open with/Edit intents. Google Drive, Dropbox and file managers can appear when their Android apps expose document-provider support.
- Linux: native picker for local files and user-mounted/synchronized cloud folders (including Google Drive via a compatible GVfs setup or Dropbox via sync/mount).
- Hardware keyboard shortcuts: Ctrl+S, Ctrl+Shift+S, Ctrl+O, Ctrl+N, Ctrl+B, Ctrl+I.
- Offline-capable editing, no login, tracking or GoreeCloud cloud service required.

**Important cloud limitation:** This app currently integrates with operating-system document providers, not Google Drive or Dropbox OAuth APIs. Provider availability and write permissions vary. Linux cloud files must already be mounted or synced. No automatic cloud synchronization is implemented.

## Install from source

Requires the Flutter stable SDK with Dart 3.9 or later, and the usual Linux desktop or Android build dependencies. First generate platform scaffolding using the supplied script:

    git clone https://github.com/GoreeCloud/markdown.git
    cd markdown
    bash tool/bootstrap.sh
    flutter run -d linux

For Android, list devices with **flutter devices** and run **flutter run -d DEVICE_ID**.

Build and test with:

    flutter analyze
    flutter test
    flutter build linux --debug
    flutter build apk --debug

Linux needs clang, cmake, ninja, pkg-config, libgtk-3-dev and liblzma-dev. Android builds need an Android SDK and a supported JDK. Generated Flutter Gradle/CMake files are intentionally created at bootstrap and are not hand-maintained. GitHub Actions uploads temporary **Android debug APK** and **Linux debug bundle** artifacts from successful CI runs (14-day retention). Open the successful workflow run under the repository Actions tab to download them. These are unsigned/debug development builds, **not** signed releases or Linux installers.

## File providers and permissions

**Android:** Tap Open and select a source from the system Files picker. Install and authenticate the relevant Drive, Dropbox or other provider app separately. When a provider grants write access, Save writes to the selected document URI. When it does not, Save As uses the system Create Document picker. Provider-specific operations may fail offline or reject a write; errors are shown.

**Linux:** Tap Open and select a local or user-mounted path; Save writes there. File-manager Open With desktop registration and initial file-path argument handling are future work.

Documents are capped at 4 MiB and must be valid UTF-8. Safe preview does not fetch external images or navigate external links. Inline HTML/JavaScript is not executed. Android requests no general filesystem or internet permission. Saves are manual: no crash recovery, autosave, external-edit conflict detection or all-exit close guard yet. Save before exiting.

## Source map

- **lib/screens/** responsive editor and commands
- **lib/widgets/** writing and safe reading surfaces
- **lib/services/** Linux picker and Android document boundary
- **lib/theme/** GC UI semantic role mapping
- **android/app/src/main/** native Kotlin SAF bridge and Android intent filters
- **test/** editing/document validation and widget smoke tests
- **docs/** architecture, MIPS assessment, quality gates and remaining work
- **tool/bootstrap.sh** platform-scaffold generation

See [Architecture](docs/ARCHITECTURE.md) and [Validation](docs/VALIDATION.md). This repository is not claiming Stable/Anchor acceptance or a deployed service.

## Attribution

Original GoreeCloud editor code is MIT licensed (see [LICENSE](LICENSE)). Flutter, file_selector and flutter_markdown_plus are BSD-3-Clause components; their original licenses remain theirs. GC UI semantic foundations are Apache-2.0 and remain the authoritative design source. The first-party Flutter design mapping is not asserted to be an officially tested native GC UI package.

# GoreeCloud Markdown — architecture

**Maturity:** original first-party development preview, not a certified native release.

## Platform strategy

The single Flutter interface compiles into native Linux and Android applications. This is not a WebView or Electron wrapper. Flutter manages rendering and input; the application owns all document behavior and its GC UI style mapping.

- **Android:** Kotlin Activity bridges a Dart MethodChannel to ACTION_OPEN_DOCUMENT and ACTION_CREATE_DOCUMENT. ContentResolver handles provider-granted content URIs and accepts compatible incoming ACTION_VIEW/ACTION_EDIT intents. Read/write permission is only for the selected item, with persistence attempted when the provider allows it.
- **Linux:** file_selector uses native file-selection UI. Files use Dart file I/O on local or user-mounted paths.
- **Preview:** flutter_markdown_plus renders Markdown as Flutter widgets, not HTML executing in a browser. Remote images are replaced and hyperlink activation is disabled.
- **Editor:** a TextEditingController owns text, pure selection-formatting functions transform selections, and a 150ms debounce limits preview rebuilds.

No token store, account login, cloud API, server or analytics process exists. Android explicitly accepts only content-scheme documents; file managers should use OS content providers. Linux supports already-mounted/synced cloud folders rather than direct Drive/Dropbox APIs.

## GC UI design contract

Implementation references [GoreeCloud UI](https://github.com/GoreeCloud/gc-ui) token and language sources: Iris seed #5145D9; 4px spacing progression; 12/18/26/36 corner radii; Canvas, Surface, Raised Surface and Lens layers; 220ms state transitions; light/dark themes and touch controls. Long-form content renders on solid surfaces for legibility. This native Flutter mapping is not a claim of a released Flutter GC UI component library.

## Boundaries and caveats

Input is strict UTF-8 with a 4 MiB byte limit. Invalid inputs and unavailable providers fail with an error instead of silently writing a different file. Writes are user initiated. Linux writes use flushed output, not an atomic file replacement. Android providers may reject writes or lack transactional guarantees. The app protects edited documents from accidental New/Open replacement, but process crashes and all window-close pathways are not guarded yet. Shared files are held only in app memory until save.

This is a **preview**, not proof of compatibility with every version of Google Drive, Dropbox, Android, GTK or Linux mount.

## Five MIPS assessment

| Platform | Review status |
| --- | --- |
| GoreeCloud UI | Semantic style mapping built; native design/visual acceptance not verified |
| Wardveil | Not integrated; applicability before Anchor must be assessed |
| GoreeShield | Not integrated; applicability before Anchor must be assessed |
| GoreeCloud Identity | Offline documents require no account; wider identity applicability unresolved |
| GoreeCloud Mesh | Not integrated; sync/interoperability explicitly out of current scope |

No claim of full platform-system integration or Stable/Anchor readiness. Follow [GoreeCloud/MIPS](https://github.com/GoreeCloud/MIPS) before any release promotion.

## Third-party review

Flutter, file_selector, flutter_markdown_plus are appropriately scoped frameworks/libraries, not rebranded upstream applications. Their respective BSD-3-Clause terms remain in effect. GC UI original tokens/design contract use Apache-2.0. This app is MIT licensed for original source. CI dependency installation and compilation do not replace a transitive supply-chain, license and vulnerability audit.

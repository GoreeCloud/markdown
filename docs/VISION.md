# My Vision for GoreeCloud Markdown

**Record type:** Owner product vision  
**Status:** Intended direction; not evidence of completed implementation  
**Recorded:** October 8, 2026

I want to develop GoreeCloud Markdown as a standalone, native Android application that provides a balanced experience for reading, creating, editing, and managing Markdown files.

My goal is to build a lightweight, powerful, privacy-first application that works offline, preserves my files in open formats, and integrates with the broader GoreeCloud ecosystem where appropriate.

I will prioritize reliable document editing, accurate Markdown rendering, intuitive navigation, accessibility, and data integrity.

I will maintain full ownership of the application's architecture, source code, development direction, and long-term maintenance.

I will follow GoreeCloud's established development standards and evaluate all nine Integral Platform Systems before claiming platform conformance or production readiness.

## Interpretation for implementation

- **Standalone and native** means the core app launches, reads, edits, previews and saves local Markdown files without a website, cloud account, or WebView.
- **Privacy-first and offline** means no required network permission, no user-tracking SDK and no implicit transmission of file contents.
- **Open format** means standard Markdown/UTF-8 files are authoritative, not an opaque app database.
- **Data integrity** means explicit unsaved-change handling, file-provider error propagation, recovery and verification; atomic replacement must be separately validated.
- **Glaze and platform integration** are controlled compliance workstreams, not reasons to couple basic local editing to a service.
- **Nine systems** means per-system applicability, contract versions, evidence and acceptance must be evaluated and documented; no blanket conformance claim is permitted.

See [PLATFORM-CONFORMANCE.md](PLATFORM-CONFORMANCE.md) for the nine-system evaluation.

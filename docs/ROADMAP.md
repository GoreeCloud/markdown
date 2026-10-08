# GoreeCloud Markdown — execution plan

The repository started as a README-only GitHub repository on 2026-10-08. These are development obligations, not completed feature claims.

## Development baseline (this branch)

- Native Kotlin/Compose project.
- SAF open/create, source editing, native Markdown preview, explicit Save/Save As.
- Dirty-document confirmation for New/Open/back; strict UTF-8 and bounded development-size files.
- Private recovery draft before attempting a provider save, and readback verification after the write.
- A staged private recovery-draft replacement plus a pre-write external-change comparison for saves back to the opened URI (source-level implementation; provider/device verification pending).
- Compose Find Next interface selects matching source text using the existing wraparound search helper (source-level implementation; keyboard, TalkBack and runtime acceptance pending).
- Document Outline dialog navigates ATX headings and excludes fenced code; pure source parser and JUnit regression cases committed (device accessibility acceptance pending).
- Unit checks for strict UTF-8, size boundaries, search, conflicts, save notices, recovery-read handling and heading outline.
- GitHub Actions build/test lane. See [DEVICE-ACCEPTANCE.md](DEVICE-ACCEPTANCE.md) for device/provider cases marked Not run.

## Next workstreams (pending)

1. **Data integrity:** verify pre-write Save conflict detection against representative providers, including failures and modification races; test URI revocation and read-only providers, persisted document grants across restart, provider truncation behavior, recovery-draft retention and cleanup, app process death, and background/editor state preservation. SAF does not guarantee atomic replace.
2. **Editor:** validate Find Next and Outline selection/focus and accessibility on device; add navigation to recent user-approved URIs, search/replace, selection formatting, undo/redo, headings outline, large-document performance, cursor continuity, keyboard shortcuts and dual-pane tablet layout.
3. **Preview:** CommonMark compliance fixtures, security review for links/HTML/images, table/footnote/task-list extensions after policy review, performance testing and accessibility semantics.
4. **Glaze:** use verified approved native tokens/mappings from the authoritative GoreeCloud/glaze repository, then verify dark/light themes, dynamic contrast, reduced motion, TalkBack, font scaling, and keyboard/foldable layout.
5. **Security/privacy:** dependency pinning and updates, no analytics or remote content by default, malicious markup/path/URI security reviews, provenance and SBOM, correct backup/retention policy.
6. **Integral Platform Systems:** full nine-system applicability and evidence matrix, integration where required, independently governed optional Sync.
7. **Distribution:** Gradle wrapper provenance, build reproducibility, protected Development signing identity and monotonic versionCode, CI verification, Drive-backed owner APK delivery, staged release qualification.

**Do not claim** runtime testing, APK readiness, cross-provider durability, platform conformance, or production stability until corresponding authoritative evidence exists.

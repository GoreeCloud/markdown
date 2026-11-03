# GoreeCloud Markdown

**A local-first Markdown workbench — developer preview.** I am building GoreeCloud Markdown as an original, lightweight browser writing tool that complements native GoreeCloud Notepad and the Open Chat Document Studio. This initial proposal is deliberately independent of accounts, cloud saves, subscriptions, tracking, and remotely hosted JavaScript.

> **Maturity:** proposed preview on a development branch, not an accepted stable release or deployed service. The current default branch does not yet include this application. No user-file or production migration is implied.

## Included in the development proposal

- Side-by-side source editing and a **read-only, in-memory preview** built with DOM text nodes, not executable HTML.
- Headings H1–H6, paragraphs, inline emphasis, strong text, inline code, fenced code blocks, block quotes, thematic rules, simple ordered/unordered lists, task-list indicators, and explicitly allowed links.
- Open user-selected local Markdown/TXT files (maximum 512 KiB) and explicitly **download** edited Markdown. Opening a file never silently overwrites its original path.
- Keyboard shortcuts: Ctrl/⌘+O to open; Ctrl/⌘+S to request a Markdown download. Unsaved edits prompt on close and when replacing content.
- GC UI-aligned spacing, palette, typography, motion, focus states, responsive editor layout, dark mode, and reduced-motion support. A visual/product accessibility audit on real browsers is still required.
- No API calls, background network requests, analytics, cookies, cloud sync, autosave, IndexedDB, or localStorage persistence. Preview links only navigate when intentionally clicked. This is not a private cloud sync product.

## Local development

Requires a modern browser, Python 3 (for the local static server), and Node.js 20 or later for tests. From the repository root:

~~~sh
python3 -m http.server 4174 --directory . --bind 127.0.0.1
# Open http://127.0.0.1:4174/web/
node --test tests/*.test.mjs
~~~

Do not open the HTML directly via the file protocol because its JavaScript imports are ES modules. The local static server binds only to loopback.

## Scope and security boundaries

This is a deliberate **Markdown subset**, not full CommonMark or GitHub-Flavored Markdown. Complex nested lists, tables, image rendering, raw HTML rendering, and WYSIWYG editing are not implemented. Unsupported syntax remains visible as plain text rather than being interpreted speculatively.

Untrusted Markdown is parsed into plain data and rendered using safe DOM constructors and text content. User-entered raw HTML is not interpreted. Only absolute HTTP, HTTPS and mailto links pass the allowlist; links open in a new tab with no opener or referrer. The preview sets an outbound-connection-denying Content Security Policy. The source length limit bounds typical memory use but is not a complete hostile-content or CPU security guarantee.

**File safety:** Downloads are browser-mediated copies; the app cannot verify whether a requested download was accepted or saved. Avoid using it as the sole copy of valuable notes. There is no crash recovery, and unsaved edits are held in RAM only.

## Development verification

Run Node syntax and regression checks or review the [Markdown checks workflow](.github/workflows/markdown.yml). Tests cover expected structure, unsafe URL rejection, raw-HTML inertness, malformed fences, and maximum source length. Automated passing checks do not establish finished cross-browser, screen-reader, visual, performance, hostile-input, or release acceptance.

## Planned next milestones

Full source-to-preview browser interaction tests, visual comparisons against the current GoreeCloud UI, keyboard and assistive-technology acceptance, controlled large-file performance testing, richer CommonMark syntax, and optional future interoperability contracts with Notepad/Drive/Open Chat. Those integrations are not implemented or promised by this preview.

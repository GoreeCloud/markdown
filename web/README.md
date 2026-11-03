# GoreeCloud Markdown — optional browser workbench proposal

**Status: separate developer preview, draft PR #3, not merged.** The canonical repository's default-branch product is now the native Flutter **Linux and Android editor** introduced by merged PR #2. This browser workbench is an optional, separate first-party local preview, not a replacement for the native clients, their README, document-provider APIs, or release program.

## Scope

- Plain-text Markdown editor with an in-memory preview created through safe DOM text nodes and explicit HTML element constructors.
- Supported preview subset: headings, paragraphs, bold/italic, code, simple lists, task indicators, blockquotes, thematic rules, and allowlisted HTTP/HTTPS/mailto links.
- Open local Markdown or TXT files (up to 512 KiB) and request a new Markdown **download**. Opening never overwrites the input path.
- Browser-native keyboard commands: Ctrl/⌘+O for opening, Ctrl/⌘+S for downloading. Unsaved-change confirmation when replacing content.
- GC UI-inspired light/dark, focus treatment, responsive split/single column and reduced-motion styles.

This prototype has **no account, cloud provider, background persistence, remote images, service worker, browser storage, or analytics**. Opening an external preview link requires an intentional click. It does not have the native client's filesystem integrations or document-provider Save/Save As.

## Run locally

Requires Node.js 20+ for source tests and Python 3 (or equivalent) for a local static web server.

~~~sh
python3 -m http.server 4174 --directory . --bind 127.0.0.1
# Open http://127.0.0.1:4174/web/
node --test tests/*.test.mjs
~~~

Do not open web/index.html directly as a file URL: the application imports an ES module from ../src/markdown.mjs. The server should remain bound to loopback for private development.

## Risk and acceptance

The parser is a deliberately **limited subset**, not full CommonMark or GitHub-Flavored Markdown. Tables, nested list fidelity, remote images, raw HTML interpretation and WYSIWYG editing are not implemented. Unrecognized syntax stays literal. Download requests are browser-managed; the application cannot confirm a saved file, and it does not keep crash recovery copies.

The CSP denies implicit network fetches; safe absolute outbound preview links still navigate on click. Script/HTML/eval insertion are intentionally absent. A length cap controls source size, but not every possible adversarial parser or browser resource case.

**Before integration:** verify actual Chromium/Firefox import/download semantics, malicious input behavior, keyboard and screen-reader flows, responsive light/dark visual acceptance, performance at maximum source size, coexistence with the native Flutter source and shared design/format conventions. Do not publish or merge solely on source-level CI. Stable native-platform acceptance is independent.

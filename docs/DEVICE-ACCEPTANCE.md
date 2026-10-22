# Android acceptance matrix — GoreeCloud Markdown

**Full acceptance: NOT RUN.** This remains a development test plan. Limited device smoke evidence below is not full runtime, provider, security, or platform acceptance.
Before executing, record exact source SHA, CI run URL, Android API level, device, document provider, input method, tester, build signing identity, and timestamp. Publish redacted reproduction evidence only; never attach private document contents.

## Document integrity and permissions

| ID | Case | Expected behavior | State |
| --- | --- | --- | --- |
| SAF-01 | Open strict UTF-8 Markdown through Android Files | Exact bytes/text preserved without broad storage permission | Not run |
| SAF-02 | Create, Save, close and reopen an MD document | Readback and reopened content match | Not run |
| SAF-03 | Modify opened document in an external editor then Save | Detect observed external change; preserve local recovery | Not run |
| SAF-04 | Revoke provider read/write grant | Clear failure and no false verified-save message | Not run |
| SAF-05 | Provider returns null, fails, truncates or changes bytes | Error and recoverable draft, no success claim | Not run |
| SAF-06 | Terminate application during staging and provider write | Existing recovery data not silently discarded | Not run |
| SAF-07 | Force private recovery cleanup failure after verified readback | Save-success message plus separate cleanup warning | Not run |
| SAF-08 | Malformed UTF-8 input or UTF-16 candidate | Refuse lossy conversion and protect document | Not run |
| SAF-09 | Document exceeds 2 MiB development cap | Explicit error, bounded memory use | Not run |
| SAF-10 | Relaunch and choose Restore or Keep provider version | User choice honored, no automatic overwrite | Not run |
| SAF-11 | Repeat core flows on a distinct SAF/cloud provider | Provider-specific behavior recorded, no atomicity claim | Not run |
| SAF-12 | External modification in check-to-write gap | Documented residual race; no race-free claim | Not run |
| SAF-13 | Private recovery draft unreadable but provider file valid | Open selected file, warn and retain damaged draft without deletion | Not run |

## User experience and accessibility

| ID | Case | Expected behavior | State |
| --- | --- | --- | --- |
| UI-01 | Find Next, case-insensitivity and wraparound | Match highlighted and useful focus/line feedback | Not run |
| UI-02 | IME composition, text selection, hardware keyboard, Back key and viewport | Back dismisses visible keyboard before dirty-document confirmation; editor, Undo and Redo remain reachable without obscuring system bars; no composition/cursor corruption | Partial smoke pass: first Back hid IME without warning; second Back displayed dirty-document confirmation. Composition, cursor, hardware keyboard and screen scaling not run |
| UI-03 | New/Open/Back with dirty document | Declining discard retains unsaved changes | Not run |
| UI-04 | Editor changes while Save runs | Saved snapshot baseline retained, newer changes remain dirty | Not run |
| UI-05 | Dark mode, font scaling, rotation, tablet | Controls remain visible and accessible | Not run |
| UI-06 | TalkBack and keyboard-only control navigation | Accurate labels, order and feedback | Not run |
| UI-07 | Preview Markdown fixtures and malicious links | Fidelity without unintended network access | Not run |
| UI-08 | Background/process death during unsaved edit | Observe actual behavior, no invented recovery guarantee | Not run |
| UI-09 | Navigate long document with heading outline | Select correct heading and place editor focus, including with keyboard/TalkBack | Not run |
| UI-10 | Search/save/outline messages with accessibility service | Polite announcements and understandable status without stealing focus | Not run |
| UI-11 | Replace selected match using Previous/Next, Unicode and IME | Only the selected match changes; other occurrences and provider bytes remain unchanged until Save; rejected requests show a message | Not run |
| UI-12 | Undo/Redo across edit, save and recovery restoration | Correct text and dirty baseline; no implicit provider write, same-document history only | Partial smoke pass: synthetic A, Undo to empty, Redo to A, unsaved marker remained. Save, recovery, cross-document cases not run |
| UI-13 | Undo/Redo with long Unicode text, IME composition, and saved selection | No lost edits or composition corruption; history bounded and UI remains responsive | Not run |
| UI-14 | Open a different file after editing, then attempt Undo/Redo | Old document history unavailable; no cross-file text restoration | Not run |
| UI-15 | Compact phone toolbar: reach Redo after Undo | Undo and Redo stay visible together without horizontal scrolling; both remain accessible at larger font scales | Partial smoke pass: Undo/Redo simultaneously visible and tappable on OnePlus N200 at default font size. Large font scales not run |
| UI-16 | Replace All confirmation, cancel, dirty-state and Unicode | Snapshot guard, non-overlapping case-insensitive count, reject oversized/invalid output; only editor changes before Save; session Undo when history budget allows | Not run |
| UI-17 | Live source statistics with emoji, combining marks and long documents | Line, word and Unicode code-point counts accurate without lag; remains readable at large font scales | Partial smoke pass: empty to synthetic A to empty to A displayed 0/1 word and character counts. Emoji, combining marks, long document and font scaling not run |

| UI-18 | Whole-application visual quality and color-coded semantics | Beautiful, polished, recognizably GoreeCloud writing workspace across source editor, preview, file actions, Undo/Redo, Find/Replace, Outline, notices and all dialogs; contextual light/dark palette, semantic text-plus-color for dirty/saved/busy/error/recovery and statistics, accessible contrast and no horizontal action clipping at supported phone/tablet widths and larger text. Require physical light/dark screenshots and maintainer-verified current Stable Glaze consumer acceptance (no outside human approval prerequisite). | **Not run — owner-mandated release blocker** |
| UI-19 | Owner-supplied Markdown redesign: focused editor | Compare exact native screen to owner image/HTML reference: compact GoreeCloud identity+overflow file menu, filename/Unsaved/Save row, Edit/Preview segment and search/outline icons, large source-first card with upper text caption; no tall statistics dashboard or horizontally scrolling file toolbar | **Not run — current layout diverges from reference** |
| UI-20 | Owner-supplied Find & Replace bottom sheet | Modal sheet with dim scrim, handle, close, editable find and replacement inputs, active/total match feedback, previous/next, replace selected/all with confirmation; editor stays behind sheet without shrinking; proper IME/Back/TalkBack handling | **Not run — current implementation uses inline panel** |
| UI-21 | Owner-supplied footer and editor details | Bottom Undo/Redo/Find, current line/word/character count and line/column; actual multiline syntax styling and line-number gutter without lost cursor/IME composition, verified on real device and large fonts | **Not run — native source editor does not match reference** |

## Bounded physical-device smoke — 2026-10-15

- **Verified code source:** `058058a70645b639d071b5119a4949859a2c2f82`. Both exact-head Android checks passed: push run `38801659961` and PR run `38801669499`.
- **Test artifact:** CI push QA artifact `12004995393`, APK SHA-256 `c8b570b8bcf67167ea5c2f5f6df815fefee602d7d287db6e28d6d5a1a58583ff`; ephemeral Android Debug signer cert SHA-256 `c5c5fc1846ad4f26179efd0c8b005a859ab92c624f4328079a15336541f31aff`. This is not protected Development signing.
- **Environment:** OnePlus N200 DE2117, Android API 36, USB ADB `fa90cf85`. Installed isolated `com.goreecloud.markdown.qa058058a706` (prior `com.goreecloud.markdown.qa` remained installed), and verified successful cold launch.
- **Observed:** default-scale Undo and Redo visible, synthetic `A` typed without Save, Undo→empty then Redo→`A`, live 1→0→1 word/character counts; first Back dismissed keyboard without discard, second Back prompted discard. Discarded test edit. No user documents, SAF files or private data accessed.
- **Limits:** No real SAF provider/recovery, Save/readback, external conflict, process death, Unicode IME, large font, TalkBack or Glaze acceptance. These cases remain open. Documentation-only changes after this code SHA do not retroactively extend the device evidence.

## Required evidence before release

Run successful exact-head Android compilation and tests; validate representative devices and providers, dependency and source security, native Glaze 1.7.0 adoption, and all nine Integral Platform Systems. Establish protected repeatable Development signing with increasing versionCode. Deliver a qualified owner APK through GoreeCloud Google Drive first. Keep draft PR and task record open until separately accepted.

**External Android Storage Access Framework providers do not guarantee atomic replacement.**
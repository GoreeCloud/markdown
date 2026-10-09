# Android acceptance matrix — GoreeCloud Markdown

**Status: NOT RUN.** This is a development test plan, not evidence of runtime or platform acceptance.
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
| UI-02 | IME composition, text selection, hardware keyboard | No composition or cursor corruption | Not run |
| UI-03 | New/Open/Back with dirty document | Declining discard retains unsaved changes | Not run |
| UI-04 | Editor changes while Save runs | Saved snapshot baseline retained, newer changes remain dirty | Not run |
| UI-05 | Dark mode, font scaling, rotation, tablet | Controls remain visible and accessible | Not run |
| UI-06 | TalkBack and keyboard-only control navigation | Accurate labels, order and feedback | Not run |
| UI-07 | Preview Markdown fixtures and malicious links | Fidelity without unintended network access | Not run |
| UI-08 | Background/process death during unsaved edit | Observe actual behavior, no invented recovery guarantee | Not run |
| UI-09 | Navigate long document with heading outline | Select correct heading and place editor focus, including with keyboard/TalkBack | Not run |
| UI-10 | Search/save/outline messages with accessibility service | Polite announcements and understandable status without stealing focus | Not run |
| UI-11 | Replace selected match using Previous/Next, Unicode and IME | Only the selected match changes; other occurrences and provider bytes remain unchanged until Save; rejected requests show a message | Not run |
| UI-12 | Undo/Redo across edit, save and recovery restoration | Correct text and dirty baseline; no implicit provider write, same-document history only | Not run |
| UI-13 | Undo/Redo with long Unicode text, IME composition, and saved selection | No lost edits or composition corruption; history bounded and UI remains responsive | Not run |
| UI-14 | Open a different file after editing, then attempt Undo/Redo | Old document history unavailable; no cross-file text restoration | Not run |
| UI-15 | Compact phone toolbar: reach Redo after Undo | Undo and Redo stay visible together without horizontal scrolling; both remain accessible at larger font scales | Not run on fixed build |
| UI-16 | Replace All confirmation, cancel, dirty-state and Unicode | Snapshot guard, non-overlapping case-insensitive count, reject oversized/invalid output; only editor changes before Save; session Undo when history budget allows | Not run |
| UI-17 | Live source statistics with emoji, combining marks and long documents | Line, word and Unicode code-point counts accurate without lag; remains readable at large font scales | Not run |

## Required evidence before release

Run successful exact-head Android compilation and tests; validate representative devices and providers, dependency and source security, native Glaze 1.7.0 adoption, and all nine Integral Platform Systems. Establish protected repeatable Development signing with increasing versionCode. Deliver a qualified owner APK through GoreeCloud Google Drive first. Keep draft PR and task record open until separately accepted.

**External Android Storage Access Framework providers do not guarantee atomic replacement.**
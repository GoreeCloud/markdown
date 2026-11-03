# Validation and remaining acceptance

Run these source/build checks from an SDK-equipped machine after bootstrap:

    bash tool/bootstrap.sh
    flutter analyze
    flutter test
    flutter build linux --debug
    flutter build apk --debug

GitHub Actions attempts automated Linux and Android builds; a successful compile does **not** prove physical device functionality or provider compatibility.

## Verified CI checkpoint — 2026-11-03

Native bounded Linux reader [PR #4](https://github.com/GoreeCloud/markdown/pull/4)
merged to `main` at `93956a8bacd7f1f5d15eb8c14df4adc08007c7ac`.
The exact-head [post-merge CI run 41730292334](https://github.com/GoreeCloud/markdown/actions/runs/41730292334)
completed successfully for Linux analysis, unit/widget tests, desktop debug build,
and Android debug APK build. The corrected Unicode fixture uses UTF-8 bytes.
This validates configured automated checks, not mounted-filesystem concurrency,
device-provider behavior, graphics compatibility, or a signed release.

Release-relevant work still open: [Linux conflict-safe and failure-safe saving #6](https://github.com/GoreeCloud/markdown/issues/6),
[Linux file-manager Open With integration #7](https://github.com/GoreeCloud/markdown/issues/7),
[Android SAF write-capability checks #8](https://github.com/GoreeCloud/markdown/issues/8),
and unresolved [Save As suffix PR #5](https://github.com/GoreeCloud/markdown/pull/5).
PR #5 is draft and conflicted with the bounded-reader merge; do not treat its
previous green CI as proof of a combined implementation.

## Manual test matrix — pending

| Platform | Scenario | Expected behavior |
| --- | --- | --- |
| Android | Files picker selects Markdown | Opens without broad storage permission |
| Android | Google Drive DocumentsProvider | Read; direct Save when writable; Save As when read-only |
| Android | Dropbox DocumentsProvider | Same, contingent on provider implementation |
| Android | Compatible file-manager Open with | Receives shared content URI |
| Android | Picker cancellation / revoked permission | No document loss, explicit error |
| Linux | Local UTF-8 Markdown | Open, edit and save correctly |
| Linux | Google Drive via GVfs or Dropbox sync/mount | Read and write through mounted path |
| Both | Invalid UTF-8 / file larger than 4 MiB | Reject with useful error |
| Both | Dirty doc then New/Open | Save, Discard or Cancel prompt |
| Both | Remote image/link in Markdown | No image download or link navigation |
| Both | Keyboard, screen reader, contrast, text scaling | Usable controls and readable preview |
| Both | Responsive layout and reduced-motion accessibility | Controls remain operable |

## Next priorities

1. Confirm CI results and fix analyzer, test, Linux and Android build errors.
2. Test Android Drive/Dropbox and local document providers on actual devices; verify persistable URI permissions and Save As.
3. Verify Linux GTK picker in supported desktop sessions, including GVfs and Dropbox mounted folders.
4. Add close-window/lifecycle unsaved guard and reliable opt-in crash recovery.
5. Detect external modification and provider conflicts; make desktop save atomic where supported.
6. Add Linux desktop MIME registration and file-manager launch arguments.
7. Provide signed installers/APKs, SBOM and reproducible release security review.
8. Perform manual accessibility, responsiveness, motion and GC UI visual acceptance tests.

**No acceptance check above is marked passed without evidence.** Current source is a development baseline.

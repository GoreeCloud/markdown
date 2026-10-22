# Native Glaze V1.7 adoption — GoreeCloud Markdown

**Status: adoption-required. Not accepted. Not production-eligible.**

The governing Glaze release is **1.7.0**, as defined by [GoreeCloud/glaze](https://github.com/GoreeCloud/glaze) (CONSUMERS.md, GLAZE_V1_7.md, contracts/v1.7/stable-scope.json). Application source is the root of GoreeCloud/markdown; this Android Kotlin/Compose app does not consume Glaze's JavaScript runtime.

A draft registration of Markdown as an `adoption-required` native consumer is proposed in [Glaze PR #415](https://github.com/GoreeCloud/glaze/pull/415). The draft registration **does not establish any acceptance**.

## Mapping work pending

- Official native semantic colors, typography, spacing, surface and component patterns, including Android TextView preview styling.
- Compact phone, large text, tablet, foldable and landscape adaptation.
- Touch targets, TalkBack order, focus, status announcements, contrast and reduced-motion preferences.
- Editor state mapping: offline, loading, busy, dirty, conflict, provider failures and private recovery.
- Evidence of rendered states on representative Android environments, security/privacy review, performance and rollback capability.

## Evidence required

For each accepted behavior, record Glaze contract version and revision, exact Markdown source commit, applicable platform/API level, test evidence, reviewer/acceptance decision and known deviations. Source-level Material 3 customization, a passing unit-test CI run, or an `adoption-required` registry entry alone is insufficient.

All nine Integral Platform Systems remain separately evaluated. Production eligibility requires independent device, packaging, signing, privacy, security and product acceptance.

#!/usr/bin/env bash
set -euo pipefail

if ! command -v flutter >/dev/null 2>&1; then
  echo "Flutter SDK was not found in PATH." >&2
  exit 1
fi

# Preserve the checked-in Android SAF integration. Flutter fills missing
# platform Gradle/CMake files without --overwrite.
flutter create \
  --platforms=android,linux \
  --org com.goreecloud \
  --project-name goreecloud_markdown \
  --no-pub \
  .
# Flutter creates a generic MyApp smoke test that is invalid for this app.
# Remove it only when it still contains the untouched template reference.
if [ -f test/widget_test.dart ] && grep -q 'const MyApp()' test/widget_test.dart; then
  rm test/widget_test.dart
fi
flutter pub get

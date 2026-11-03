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
flutter pub get

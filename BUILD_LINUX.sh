#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle is required. This project is intended to use Gradle 8.7." >&2
  exit 1
fi
GRADLE_VERSION=$(gradle --version | awk '/^Gradle / {print $2; exit}')
if [ "$GRADLE_VERSION" != "8.7" ]; then
  echo "Keeply expects Gradle 8.7. Found Gradle $GRADLE_VERSION." >&2
  echo "Run: sdk use gradle 8.7" >&2
  exit 1
fi
JAVA_MAJOR=$(java -version 2>&1 | awk -F'"' '/version/ {print $2}' | cut -d. -f1)
if [ "${JAVA_MAJOR:-0}" -gt 21 ]; then
  echo "Keeply expects JDK 21 or lower to run Gradle 8.7. Current Java: $JAVA_MAJOR" >&2
  exit 1
fi
gradle :app:assembleDebug -Pkotlin.compiler.execution.strategy=in-process --no-daemon
printf '\nKeeply APK: %s/app/build/outputs/apk/debug/app-debug.apk\n' "$PWD"

#!/bin/bash
set -euo pipefail
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_DIR"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
export ANDROID_HOME="$PROJECT_DIR/.tools/android-sdk"
export PATH="$JAVA_HOME/bin:/opt/homebrew/bin:$PATH"
if [ ! -x "$JAVA_HOME/bin/java" ] || [ ! -d "$ANDROID_HOME/platforms/android-35" ]; then
  echo "Can JDK 17 va Android SDK 35. Bo SDK demo tren may nay nam trong .tools/android-sdk."
  exit 1
fi
bash ./gradlew assembleDebug --console=plain
if [ "${1:-}" = "--build-only" ]; then
  exit 0
fi
if [ ! -f .tools/start_demo.py ]; then
  echo "Thieu bo demo cuc bo .tools/start_demo.py."
  exit 1
fi
exec /opt/homebrew/bin/python3 .tools/start_demo.py

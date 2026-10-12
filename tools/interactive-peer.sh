#!/bin/bash
set -euo pipefail
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
classes_dir="$(mktemp -d)"
trap 'rm -rf "$classes_dir"' EXIT
"${JAVA_HOME:+$JAVA_HOME/bin/}javac" -encoding UTF-8 -d "$classes_dir" \
  "$project_dir"/app/src/main/java/vn/edu/usth/connect/model/*.java \
  "$project_dir"/app/src/main/java/vn/edu/usth/connect/irc/*.java "$project_dir/tools/InteractivePeer.java"
"${JAVA_HOME:+$JAVA_HOME/bin/}java" -cp "$classes_dir" InteractivePeer "${1:-127.0.0.1}" "${2:-6667}" "${3:-false}" "${4:-an_demo}" "${5:-#web-general}"

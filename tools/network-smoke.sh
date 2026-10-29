#!/bin/bash
set -euo pipefail

project_dir="$(cd "$(dirname "$0")/.." && pwd)"
classes_dir="$(mktemp -d)"
trap 'rm -rf "$classes_dir"' EXIT

java_bin="${JAVA_HOME:+$JAVA_HOME/bin/}java"
javac_bin="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
server_host="${1:-127.0.0.1}"
server_port="${2:-6667}"
use_tls="${3:-false}"

"$javac_bin" -encoding UTF-8 -d "$classes_dir" \
  "$project_dir"/app/src/main/java/vn/edu/usth/connect/model/*.java \
  "$project_dir"/app/src/main/java/vn/edu/usth/connect/irc/*.java \
  "$project_dir/tools/NetworkSmoke.java" \
  "$project_dir/tools/AdvancedNetworkSmoke.java"

"$java_bin" -cp "$classes_dir" NetworkSmoke \
  "$server_host" "$server_port" "$use_tls"
"$java_bin" -cp "$classes_dir" AdvancedNetworkSmoke \
  "$server_host" "$server_port" "$use_tls"

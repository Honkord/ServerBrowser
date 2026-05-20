#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [ -x "$ROOT/gradlew" ]; then
  "$ROOT/gradlew" compileJava
  exit 0
fi

if command -v mvn >/dev/null 2>&1; then
  mvn -q compile
  exit 0
fi

# Legacy fallback
JDBC="$ROOT/lib/sqlite-jdbc.jar"
SRC="$ROOT/src/server"
OUT="$ROOT/build/server"
mkdir -p "$OUT"
javac -cp "$JDBC" -d "$OUT" "$SRC"/com/server_browser/*.java
echo "Compiled server classes to $OUT"

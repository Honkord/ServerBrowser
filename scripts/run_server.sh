#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if command -v lsof >/dev/null 2>&1 && lsof -ti :8443 >/dev/null 2>&1; then
  echo "Port 8443 is in use — stopping the previous server..."
  "$ROOT/scripts/stop_server.sh"
fi

if [ -x "$ROOT/gradlew" ]; then
  exec "$ROOT/gradlew" run
fi

if command -v mvn >/dev/null 2>&1; then
  mvn -q compile exec:java
  exit 0
fi

echo "Run ./gradlew run or mvn exec:java (Java 17+ required)." >&2
exit 1

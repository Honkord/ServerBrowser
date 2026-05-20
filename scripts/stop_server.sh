#!/usr/bin/env bash
set -euo pipefail

if command -v lsof >/dev/null 2>&1; then
  PIDS=$(lsof -ti :8443 2>/dev/null || true)
elif command -v fuser >/dev/null 2>&1; then
  PIDS=$(fuser 8443/tcp 2>/dev/null | tr -s ' ' '\n' | grep -E '^[0-9]+$' || true)
else
  PIDS=$(pgrep -f 'com.server_browser.server_runtime' 2>/dev/null || true)
fi

if [ -z "${PIDS:-}" ]; then
  echo "No process listening on port 8443."
  exit 0
fi

echo "Stopping server (PID(s): $PIDS)..."
kill $PIDS 2>/dev/null || true
sleep 0.5
if ss -tln 2>/dev/null | grep -q ':8443 '; then
  echo "Port still in use; sending SIGKILL..."
  kill -9 $PIDS 2>/dev/null || true
fi
echo "Port 8443 is free."

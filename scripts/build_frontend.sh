#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [ -x "$ROOT/gradlew" ]; then
  "$ROOT/gradlew" buildFrontend
  exit 0
fi

cd "$ROOT/frontend"
if ! command -v npm >/dev/null 2>&1; then
  echo "npm is required. Install Node.js 20+ and retry." >&2
  exit 1
fi
if [ ! -d node_modules ]; then
  npm install
fi
npm run build
echo "Built UI to frontend/dist"

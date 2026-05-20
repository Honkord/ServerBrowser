#!/usr/bin/env bash
# Installs local hosts entries so *.server_browser.org resolves to this machine.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
HOSTS_FILE="${1:-/etc/hosts}"
MARKER="# Server Browser phonebook (server_browser.org)"
SNIPPET="$ROOT/hosts/phonebook.hosts"

if [[ ! -f "$SNIPPET" ]]; then
  echo "Missing $SNIPPET" >&2
  exit 1
fi

if grep -qF "$MARKER" "$HOSTS_FILE" 2>/dev/null; then
  echo "Phonebook hosts already installed in $HOSTS_FILE"
  exit 0
fi

echo "Adding phonebook hosts to $HOSTS_FILE (sudo required)"
{
  echo ""
  echo "$MARKER"
  grep -v '^#' "$SNIPPET" | grep -v '^[[:space:]]*$'
} | sudo tee -a "$HOSTS_FILE" >/dev/null

echo "Done. Use https://example.server_browser.org:8443/ (after starting the server)."
echo "Or open https://localhost:8443 and search for example.server_browser.org"

#!/usr/bin/env bash
set -euo pipefail
ZUFFY_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if ! command -v node >/dev/null 2>&1 || ! command -v npm >/dev/null 2>&1; then
  echo "For frontend development, install Node.js 22.12+ or newer first."
  echo "You can run the included compiled UI without Node: bash start-backend.sh"
  exit 1
fi
cd "$ZUFFY_ROOT/frontend"
if [ ! -d node_modules ]; then npm ci; fi
echo "Frontend: http://localhost:5173 — keep the backend running on port 8081."
exec npm run dev

#!/usr/bin/env bash
set -euo pipefail
ZUFFY_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ZUFFY_ROOT/frontend"
if [ ! -d node_modules ]; then npm ci; fi
npm run build
ZUFFY_STATIC="$ZUFFY_ROOT/spring-boot/src/main/resources/static"
mkdir -p "$ZUFFY_STATIC"
# Only this generated static directory is replaced; Java sources and properties stay intact.
if [ -d "$ZUFFY_STATIC/assets" ]; then rm -rf "$ZUFFY_STATIC/assets"; fi
cp -R dist/. "$ZUFFY_STATIC/"
echo "React build copied into Spring Boot. Restart the backend to see it at http://localhost:8081."

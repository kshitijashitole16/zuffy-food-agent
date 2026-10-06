#!/usr/bin/env bash
set -euo pipefail
ZUFFY_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if ! command -v java >/dev/null 2>&1 || ! java -version >/dev/null 2>&1; then
  echo "Java is required. Install a JDK 17 or newer (JDK 21 recommended), then run this script again."
  exit 1
fi
cd "$ZUFFY_ROOT/spring-boot"
echo "Starting Zuffy at http://localhost:${PORT:-8081}"
echo "Maven is included through the wrapper; the first run downloads dependencies."
exec sh ./mvnw spring-boot:run

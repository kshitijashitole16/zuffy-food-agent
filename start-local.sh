#!/usr/bin/env bash
set -euo pipefail
ZUFFY_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ZUFFY_MODEL="${OLLAMA_MODEL:-llama3.2:3b}"
ZUFFY_OLLAMA_URL="${OLLAMA_BASE_URL:-http://localhost:11434}"
ZUFFY_OLLAMA_PID=""
ZUFFY_BACKEND_PID=""

cleanup() {
  if [ -n "$ZUFFY_BACKEND_PID" ]; then kill "$ZUFFY_BACKEND_PID" 2>/dev/null || true; fi
  if [ -n "$ZUFFY_OLLAMA_PID" ]; then kill "$ZUFFY_OLLAMA_PID" 2>/dev/null || true; fi
}
trap cleanup EXIT
trap 'exit 130' INT TERM

if ! command -v java >/dev/null 2>&1 || ! java -version >/dev/null 2>&1; then
  echo "Install a JDK 17+ (21 recommended), then run bash start-local.sh again."
  exit 1
fi
if ! command -v ollama >/dev/null 2>&1; then
  echo "Install and open Ollama from https://ollama.com/download. Restart Terminal if needed."
  echo "Then run: bash start-local.sh"
  exit 1
fi

export OLLAMA_MODEL="$ZUFFY_MODEL"
export OLLAMA_BASE_URL="$ZUFFY_OLLAMA_URL"
export OLLAMA_HOST="$ZUFFY_OLLAMA_URL"

if ! curl --fail --silent --max-time 3 "$ZUFFY_OLLAMA_URL/api/tags" >/dev/null; then
  case "$ZUFFY_OLLAMA_URL" in
    http://localhost:11434|http://127.0.0.1:11434)
      mkdir -p "$ZUFFY_ROOT/.logs"
      echo "Starting local Ollama…"
      ollama serve >"$ZUFFY_ROOT/.logs/ollama.log" 2>&1 &
      ZUFFY_OLLAMA_PID=$!
      for ZUFFY_ATTEMPT in {1..30}; do
        if curl --fail --silent --max-time 2 "$ZUFFY_OLLAMA_URL/api/tags" >/dev/null; then break; fi
        sleep 1
      done
      if ! curl --fail --silent --max-time 3 "$ZUFFY_OLLAMA_URL/api/tags" >/dev/null; then
        echo "Ollama did not start. Open the Ollama app or check .logs/ollama.log."
        exit 1
      fi
      ;;
    *) echo "Cannot reach $ZUFFY_OLLAMA_URL. Start that Ollama server first."; exit 1 ;;
  esac
fi

if ! ollama show "$ZUFFY_MODEL" >/dev/null 2>&1; then
  echo "Downloading $ZUFFY_MODEL (about 2 GB for the default model; first run only)…"
  ollama pull "$ZUFFY_MODEL"
fi

echo "Model ready. Starting Zuffy at http://localhost:${PORT:-8081}"
echo "Wait for 'Started ZuffyApplication' before opening the page. Ctrl+C stops this run."
cd "$ZUFFY_ROOT/spring-boot"
sh ./mvnw spring-boot:run &
ZUFFY_BACKEND_PID=$!
wait "$ZUFFY_BACKEND_PID"

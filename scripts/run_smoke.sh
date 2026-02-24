#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GRADLE_CMD="${GRADLE_CMD:-$ROOT_DIR/gradlew}"
EMU_FILE="${EMU_FILE:-ROMS/Apple2e.emu}"
PASTE_FILE="${PASTE_FILE:-$ROOT_DIR/ROMS/opcode_smoke_loader_hgr_mem_16k.mon}"
STEPS="${STEPS:-80000000}"
HALT_EXECUTION="${HALT_EXECUTION:-0x33D2,0x33C0}"
EXTRA_ARGS="${EXTRA_ARGS:-}"

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  cat <<'EOF'
Usage: ./scripts/run_smoke.sh [extra emulator args]

Environment overrides:
  EMU_FILE        default: ROMS/Apple2e.emu
  PASTE_FILE      default: <repo>/ROMS/opcode_smoke_loader_hgr_mem_16k.mon
  STEPS           default: 80000000
  HALT_EXECUTION  default: 0x33D2,0x33C0
  EXTRA_ARGS      default: (empty)
  GRADLE_CMD      default: <repo>/gradlew
  GRADLE_USER_HOME default: /tmp/gradle-home

Example:
  STEPS=120000000 HALT_EXECUTION=0x1234,0x5678 ./scripts/run_smoke.sh
EOF
  exit 0
fi

if [[ ! -x "$GRADLE_CMD" ]]; then
  echo "Error: gradle wrapper not executable: $GRADLE_CMD" >&2
  exit 1
fi

if [[ ! -f "$PASTE_FILE" ]]; then
  echo "Error: smoke paste file not found: $PASTE_FILE" >&2
  exit 1
fi

JAVA_MAJOR="$(java -version 2>&1 | sed -n '1s/.*version "\(.*\)".*/\1/p' | cut -d. -f1)"
if [[ -n "$JAVA_MAJOR" && "$JAVA_MAJOR" -lt 25 ]]; then
  if [[ -x /usr/libexec/java_home ]]; then
    JAVA25_HOME="$(/usr/libexec/java_home -v 25 2>/dev/null || true)"
    if [[ -n "$JAVA25_HOME" ]]; then
      export JAVA_HOME="$JAVA25_HOME"
      export PATH="$JAVA_HOME/bin:$PATH"
      echo "Using JAVA_HOME=$JAVA_HOME"
    fi
  fi
fi

RUN_ARGS="$EMU_FILE --steps $STEPS --paste-file $PASTE_FILE --halt-execution $HALT_EXECUTION --print-text-at-exit --debug"
RUN_ARGS="$RUN_ARGS --no-sound"
if [[ -n "$EXTRA_ARGS" ]]; then
  RUN_ARGS="$RUN_ARGS $EXTRA_ARGS"
fi

cd "$ROOT_DIR"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/tmp/gradle-home}"
exec "$GRADLE_CMD" runHeadless --args="$RUN_ARGS"

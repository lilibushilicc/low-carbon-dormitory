#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_PATH="$SCRIPT_DIR/low-carbon-dormitory-0.0.1-SNAPSHOT.jar"
ENV_PATH="${BACKEND_ENV_FILE:-$SCRIPT_DIR/backend.env}"

if [[ ! -f "$JAR_PATH" ]]; then
  echo "未找到后端 Jar: $JAR_PATH" >&2
  exit 1
fi

if [[ -f "$ENV_PATH" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_PATH"
  set +a
fi

exec java -jar "$JAR_PATH"

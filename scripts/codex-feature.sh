#!/usr/bin/env bash
set -euo pipefail

REQUIREMENT="${*:-}"

if [ -z "$REQUIREMENT" ]; then
  echo "Usage: ./scripts/codex-feature.sh \"你的需求\""
  exit 1
fi

PROMPT_FILE="docs/prompts/FEATURE_REQUEST_AUTOMATION.md"

if [ ! -f "$PROMPT_FILE" ]; then
  echo "Missing prompt file: $PROMPT_FILE"
  exit 1
fi

PROMPT="$(cat "$PROMPT_FILE")"
PROMPT="${PROMPT//'{{REQUIREMENT}}'/$REQUIREMENT}"

codex "$PROMPT"

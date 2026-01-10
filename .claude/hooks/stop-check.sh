#!/usr/bin/env bash
set -euo pipefail

stop_active=$(jq -r '.stop_hook_active // false')

if [[ "$stop_active" == "true" ]]; then
  exit 0
else
  cat <<EOF >&2
Before ending this turn, quickly assess:

(1) Did the user correct you on a mistake you made?
(2) Did you backtrack due to your own error?
(3) Discover something non-obvious, useful or important?
(4) Waste time on something avoidable?

If YES to any, or there's a different reason to remember something, determine if this is something that should be remembered temporarily or
permanently. Do this by taking into consideration the context in which this happened - if you're not sure, ask the user. If this turns out
to be something to be remembered permanently, append a detailed note to @agent-memory/short-term-memory/observations.md. If it's only
relevant temporarily, add it to an appropriate place in @agent-memory/working-memory. You are encouraged to make multiple observations at
once, and enriching both short-term memory and working-memory with different observations at once.
EOF
  exit 2
fi

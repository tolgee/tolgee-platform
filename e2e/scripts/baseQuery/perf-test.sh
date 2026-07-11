#!/usr/bin/env bash
#
# Translation View query performance test against a real Tolgee instance.
#
# Auto-discovers project languages via the API, then benchmarks the
# GET /v2/projects/{id}/translations endpoint across a matrix of filter,
# sort, search, and pagination scenarios.
#
# Usage:
#   ./perf-test.sh <base-url> <project-id> <api-token>
#   ./perf-test.sh https://app.tolgee.io 42 tgpat_xxxx
#
# Optional env vars:
#   WARMUP=2          Number of warmup runs per scenario (default: 1)
#   RUNS=5            Number of measured runs per scenario (default: 3)
#   PAGE_SIZE=20      Page size for queries (default: 20)
#   REQUEST_DELAY=2   Seconds between requests to avoid rate limiting (default: 2)
#   SCENARIO_DELAY=3  Seconds between scenarios (default: 3)
#   SORT_LANG=en      Language tag to use for sort-by-text scenarios (default: first language)
#   FILTER_LANG=      Language tag for single-lang filter scenarios (default: auto-detected)
#

set -euo pipefail

# ── Arguments ─────────────────────────────────────────────────────────────────

if [[ $# -lt 3 ]]; then
  echo "Usage: $0 <base-url> <project-id> <api-token>"
  echo "Example: $0 https://app.tolgee.io 42 tgpat_xxxx"
  exit 1
fi

BASE_URL="$1"
PROJECT_ID="$2"
API_KEY="$3"

WARMUP="${WARMUP:-1}"
RUNS="${RUNS:-3}"
PAGE_SIZE="${PAGE_SIZE:-20}"

API="${BASE_URL}/v2/projects/${PROJECT_ID}/translations"

# ── Auto-discover languages ───────────────────────────────────────────────────

echo "Discovering languages for project ${PROJECT_ID}..."
LANG_JSON=$(curl -s -H "X-API-Key: ${API_KEY}" -H "Accept: application/json" \
  "${BASE_URL}/v2/projects/${PROJECT_ID}/languages?size=1000" 2>/dev/null)

readarray -t ALL_LANGS < <(python3 -c "
import json, sys
d = json.loads('''${LANG_JSON}''')
langs = d.get('_embedded', {}).get('languages', [])
for l in langs:
    print(l['tag'])
" 2>/dev/null)

LANG_COUNT=${#ALL_LANGS[@]}
if [[ $LANG_COUNT -eq 0 ]]; then
  echo "ERROR: No languages found. Check the URL, project ID, and API token."
  exit 1
fi

# Pick a sort language (first language or SORT_LANG override)
SORT_LANG="${SORT_LANG:-${ALL_LANGS[0]}}"

# Pick a filter language for single-lang scenarios: prefer a partially-translated
# language if one exists, otherwise fall back to SORT_LANG.
if [[ -n "${FILTER_LANG:-}" ]]; then
  : # use the override
else
  FILTER_LANG="${ALL_LANGS[1]:-${ALL_LANGS[0]}}"
fi

echo "  Found ${LANG_COUNT} languages: ${ALL_LANGS[*]}"
echo "  Sort language: ${SORT_LANG}"
echo "  Filter language: ${FILTER_LANG}"
echo ""

# ── Helpers ───────────────────────────────────────────────────────────────────

build_qs() {
  local qs=""
  for kv in "$@"; do
    if [[ -n "$qs" ]]; then
      qs="${qs}&${kv}"
    else
      qs="${kv}"
    fi
  done
  echo "$qs"
}

langs_params() {
  local count="${1:-${LANG_COUNT}}"
  local params=()
  for ((i = 0; i < count && i < LANG_COUNT; i++)); do
    params+=("languages=${ALL_LANGS[$i]}")
  done
  echo "${params[*]}"
}

run_request() {
  local url="$1"
  local tmpfile
  tmpfile=$(mktemp)

  local timing
  timing=$(curl -s -o "$tmpfile" -w '%{time_total} %{http_code}' \
    -H "X-API-Key: ${API_KEY}" \
    -H "Accept: application/json" \
    "$url" 2>/dev/null)

  local time_s http_code
  read -r time_s http_code <<< "$timing"

  local ms
  ms=$(echo "$time_s" | awk '{printf "%.0f", $1 * 1000}')

  local total_elements="?"
  if [[ "$http_code" == "200" ]]; then
    total_elements=$(python3 -c "
import json, sys
try:
    d = json.load(open('$tmpfile'))
    p = d.get('page', {})
    print(p.get('totalElements', '?'))
except:
    print('ERR')
" 2>/dev/null || echo "ERR")
  fi

  rm -f "$tmpfile"

  echo "${ms}|${http_code}|${total_elements}"
}

run_scenario() {
  local title="$1"
  local url="$2"

  echo "────────────────────────────────────────────────────────────────────────"
  echo "SCENARIO: $title"
  echo "  URL: $url"

  for ((w = 1; w <= WARMUP; w++)); do
    local result
    result=$(run_request "$url")
    local ms=${result%%|*}
    echo "  warmup #${w}: ${ms}ms"
    sleep "${REQUEST_DELAY:-2}"
  done

  local timings=()
  for ((r = 1; r <= RUNS; r++)); do
    local result
    result=$(run_request "$url")
    IFS='|' read -r ms status total <<< "$result"
    timings+=("$ms")
    echo "  run #${r}: ${ms}ms (HTTP ${status}, totalElements=${total})"
    sleep "${REQUEST_DELAY:-2}"
  done

  local sorted
  sorted=($(printf '%s\n' "${timings[@]}" | sort -n))
  local count=${#sorted[@]}
  local median=${sorted[$((count / 2))]}
  local min=${sorted[0]}
  local max=${sorted[$((count - 1))]}

  echo ">>> RESULT [$title]: median=${median}ms min=${min}ms max=${max}ms"
  echo "────────────────────────────────────────────────────────────────────────"
  echo ""

  SUMMARY+=("$(printf '%-55s  median=%4sms  min=%4sms  max=%4sms' "$title" "$median" "$min" "$max")")
  sleep "${SCENARIO_DELAY:-3}"
}

# ── Scenarios ─────────────────────────────────────────────────────────────────

SUMMARY=()

echo "========================================================================"
echo "Translation View Query Performance Test"
echo "  Instance:   ${BASE_URL}"
echo "  Project:    ${PROJECT_ID}"
echo "  Languages:  ${LANG_COUNT}"
echo "  Page size:  ${PAGE_SIZE}"
echo "  Warmup:     ${WARMUP} run(s)"
echo "  Measured:   ${RUNS} run(s)"
echo "========================================================================"
echo ""

# Page load — all languages
qs=$(build_qs $(langs_params) "size=${PAGE_SIZE}")
run_scenario \
  "Page load: ${PAGE_SIZE} keys x ${LANG_COUNT} langs" \
  "${API}?${qs}"

# Page load — half the languages
half=$(( LANG_COUNT / 2 ))
if [[ $half -gt 0 && $half -ne $LANG_COUNT ]]; then
  qs=$(build_qs $(langs_params $half) "size=${PAGE_SIZE}")
  run_scenario \
    "Page load: ${PAGE_SIZE} keys x ${half} langs" \
    "${API}?${qs}"
fi

# Page load — 5 languages (if project has more)
if [[ $LANG_COUNT -gt 5 ]]; then
  qs=$(build_qs $(langs_params 5) "size=${PAGE_SIZE}")
  run_scenario \
    "Page load: ${PAGE_SIZE} keys x 5 langs" \
    "${API}?${qs}"
fi

# Full-text search
qs=$(build_qs $(langs_params) "size=${PAGE_SIZE}" "search=button")
run_scenario \
  "Full-text search 'button' x ${LANG_COUNT} langs" \
  "${API}?${qs}"

qs=$(build_qs $(langs_params) "size=${PAGE_SIZE}" "search=save")
run_scenario \
  "Full-text search 'save' x ${LANG_COUNT} langs" \
  "${API}?${qs}"

# Sort by translation text
qs=$(build_qs $(langs_params) "size=${PAGE_SIZE}" "sort=translations.${SORT_LANG}.text,asc" "sort=keyName,asc")
run_scenario \
  "Sort by translations.${SORT_LANG}.text ASC x ${LANG_COUNT} langs" \
  "${API}?${qs}"

qs=$(build_qs $(langs_params) "size=${PAGE_SIZE}" "sort=translations.${SORT_LANG}.text,desc" "sort=keyName,asc")
run_scenario \
  "Sort by translations.${SORT_LANG}.text DESC x ${LANG_COUNT} langs" \
  "${API}?${qs}"

# filterUntranslatedAny
qs=$(build_qs $(langs_params) "size=${PAGE_SIZE}" "filterUntranslatedAny=true")
run_scenario \
  "filterUntranslatedAny x ${LANG_COUNT} langs" \
  "${API}?${qs}"

# filterUntranslatedInLang
qs=$(build_qs $(langs_params) "size=${PAGE_SIZE}" "filterUntranslatedInLang=${FILTER_LANG}")
run_scenario \
  "filterUntranslatedInLang=${FILTER_LANG} x ${LANG_COUNT} langs" \
  "${API}?${qs}"

# filterTranslatedAny
qs=$(build_qs $(langs_params) "size=${PAGE_SIZE}" "filterTranslatedAny=true")
run_scenario \
  "filterTranslatedAny x ${LANG_COUNT} langs" \
  "${API}?${qs}"

# filterTranslatedInLang
qs=$(build_qs $(langs_params) "size=${PAGE_SIZE}" "filterTranslatedInLang=${SORT_LANG}")
run_scenario \
  "filterTranslatedInLang=${SORT_LANG} x ${LANG_COUNT} langs" \
  "${API}?${qs}"

# filterState — UNTRANSLATED across all languages (homogeneous)
state_params=()
for lang in "${ALL_LANGS[@]}"; do
  state_params+=("filterState=${lang},UNTRANSLATED")
done
qs=$(build_qs $(langs_params) "size=${PAGE_SIZE}" "${state_params[@]}")
run_scenario \
  "filterState=UNTRANSLATED all ${LANG_COUNT} langs (homogeneous)" \
  "${API}?${qs}"

# filterState — TRANSLATED for a single language
qs=$(build_qs $(langs_params) "size=${PAGE_SIZE}" "filterState=${FILTER_LANG},TRANSLATED")
run_scenario \
  "filterState=${FILTER_LANG},TRANSLATED x ${LANG_COUNT} langs" \
  "${API}?${qs}"

# Cursor-based pagination — page 2
echo "────────────────────────────────────────────────────────────────────────"
echo "SCENARIO: Cursor pagination (page 2)"
page1_qs=$(build_qs $(langs_params) "size=${PAGE_SIZE}")
page1_tmp=$(mktemp)
curl -s -H "X-API-Key: ${API_KEY}" -H "Accept: application/json" \
  "${API}?${page1_qs}" > "$page1_tmp" 2>/dev/null
cursor=$(python3 -c "
import json, sys
d = json.load(open('$page1_tmp'))
nc = d.get('nextCursor', '')
print(nc)
" 2>/dev/null || echo "")
rm -f "$page1_tmp"

if [[ -n "$cursor" ]]; then
  qs=$(build_qs $(langs_params) "size=${PAGE_SIZE}" "cursor=${cursor}")
  run_scenario \
    "Cursor page 2: ${PAGE_SIZE} keys x ${LANG_COUNT} langs" \
    "${API}?${qs}"
else
  echo "  (skipped — no cursor in page 1 response)"
  echo ""
fi

# Large page size
qs=$(build_qs $(langs_params) "size=100")
run_scenario \
  "Large page: 100 keys x ${LANG_COUNT} langs" \
  "${API}?${qs}"

# filterOutdatedLanguage
qs=$(build_qs $(langs_params) "size=${PAGE_SIZE}" "filterOutdatedLanguage=${SORT_LANG}")
run_scenario \
  "filterOutdatedLanguage=${SORT_LANG} x ${LANG_COUNT} langs" \
  "${API}?${qs}"

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "========================================================================"
echo "SUMMARY"
echo "========================================================================"
for line in "${SUMMARY[@]}"; do
  echo "  $line"
done
echo "========================================================================"

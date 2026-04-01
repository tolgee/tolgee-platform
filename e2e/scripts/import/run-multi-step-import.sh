#!/bin/bash
set -e

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
parse_args "$@"

echo "=== Multi-step Import Performance Test ==="
echo "Keys:         $NUM_KEYS"
echo "Languages:    $NUM_LANGUAGES"
echo "Translations: $TOTAL_TRANSLATIONS"
echo ""

setup_all

echo ""
echo "=== Step 1: Add files ==="
echo "Uploading $NUM_KEYS keys x $NUM_LANGUAGES languages..."

STEP1_START=$(date +%s)

HTTP_CODE=$(curl -s -o /dev/null -w '%{http_code}' \
    -X POST "$BASE_URL/v2/projects/$PROJECT_ID/import" \
    -H "Authorization: Bearer $JWT_TOKEN" \
    "${CURL_ARGS[@]}")

STEP1_END=$(date +%s)
STEP1_DURATION=$((STEP1_END - STEP1_START))

if [ "$HTTP_CODE" -ne 200 ]; then
    echo "Add files failed with HTTP $HTTP_CODE! Check $LOG_FILE for details."
    exit 1
fi
echo "Add files completed in ${STEP1_DURATION}s (HTTP $HTTP_CODE)"

echo ""
echo "=== Step 2: Apply import ==="

STEP2_START=$(date +%s)

APPLY_RESPONSE=$(curl -s -w '\n%{http_code}' \
    -X PUT "$BASE_URL/v2/projects/$PROJECT_ID/import/apply?forceMode=OVERRIDE" \
    -H "Authorization: Bearer $JWT_TOKEN")

STEP2_END=$(date +%s)
STEP2_DURATION=$((STEP2_END - STEP2_START))
HTTP_CODE=$(echo "$APPLY_RESPONSE" | tail -1)

if [ "$HTTP_CODE" -ne 200 ]; then
    APPLY_BODY=$(echo "$APPLY_RESPONSE" | sed '$d')
    echo "Apply failed with HTTP $HTTP_CODE: $APPLY_BODY"
    echo "Check $LOG_FILE for details."
    exit 1
fi
echo "Apply completed in ${STEP2_DURATION}s (HTTP $HTTP_CODE)"

TOTAL_DURATION=$((STEP2_END - STEP1_START))
TOTAL_MIN=$((TOTAL_DURATION / 60))
TOTAL_SEC=$((TOTAL_DURATION % 60))

echo ""
echo "=== Results ==="
echo "Keys:         $NUM_KEYS"
echo "Languages:    $NUM_LANGUAGES"
echo "Translations: $TOTAL_TRANSLATIONS"
echo "Step 1 (add files): ${STEP1_DURATION}s"
echo "Step 2 (apply):     ${STEP2_DURATION}s"
echo "Total:              ${TOTAL_MIN}m ${TOTAL_SEC}s"
echo ""
echo "Import completed successfully!"

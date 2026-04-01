#!/bin/bash
set -e

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
parse_args "$@"

echo "=== Single-step Import Performance Test ==="
echo "Keys:         $NUM_KEYS"
echo "Languages:    $NUM_LANGUAGES"
echo "Translations: $TOTAL_TRANSLATIONS"
echo ""

setup_all

echo ""
echo "=== Starting single-step import ==="
echo "Importing $NUM_KEYS keys x $NUM_LANGUAGES languages = $TOTAL_TRANSLATIONS translations..."

PARAMS='{"forceMode":"OVERRIDE","createNewKeys":true}'
START_TIME=$(date +%s)

HTTP_CODE=$(curl -s -o /dev/null -w '%{http_code}' \
    -X POST "$BASE_URL/v2/projects/$PROJECT_ID/single-step-import" \
    -H "Authorization: Bearer $JWT_TOKEN" \
    "${CURL_ARGS[@]}" \
    -F "params=$PARAMS;type=application/json")

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))
MIN=$((DURATION / 60))
SEC=$((DURATION % 60))

echo ""
echo "=== Results ==="
echo "Keys:         $NUM_KEYS"
echo "Languages:    $NUM_LANGUAGES"
echo "Translations: $TOTAL_TRANSLATIONS"
echo "Duration:     ${MIN}m ${SEC}s"
echo "HTTP status:  $HTTP_CODE"

if [ "$HTTP_CODE" -ne 200 ]; then
    echo ""
    echo "Import failed! Check $LOG_FILE for details."
    exit 1
fi

echo ""
echo "Import completed successfully!"

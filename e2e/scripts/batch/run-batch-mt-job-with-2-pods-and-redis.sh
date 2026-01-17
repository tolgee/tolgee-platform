#!/bin/bash
set -e

# Detect script directory and project root
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/../../.." && pwd )"

# Check for required tools
if ! command -v psql &> /dev/null; then
    echo "Error: psql is not installed. Please install it to run this script:"
    echo "brew install libpq && brew link --force libpq"
    exit 1
fi

if ! command -v jq &> /dev/null; then
    echo "Error: jq is not installed. Please install it to run this script:"
    echo "brew install jq"
    exit 1
fi

# Check for existing tolgee_postgres container
if [ "$(docker ps -aq -f name=^tolgee_postgres$)" ]; then
    echo "Existing docker container 'tolgee_postgres' found."
    read -p "It will be deleted. Press Enter to continue or Ctrl+C to cancel..."
    echo "Removing 'tolgee_postgres' container..."
    docker rm -f tolgee_postgres
fi

# Set number of keys (default to 10000 if not provided)
NUM_KEYS=${1:-10000}
KEYS_FILE="$SCRIPT_DIR/${NUM_KEYS}-keys.json"
echo "Project root detected at: $PROJECT_ROOT"
echo "Number of keys to process: $NUM_KEYS"

# 1. Build the JAR
echo "Building the JAR..."
cd "$PROJECT_ROOT"
./gradlew clean :server-app:bootJar

# Find the generated JAR file
JAR_FILE="$PROJECT_ROOT/backend/app/build/libs/tolgee-local.jar"

if [ ! -f "$JAR_FILE" ]; then
    # Try with project version if 'local' is not used
    VERSION=$(./gradlew -q properties --console=plain | grep "^version:" | awk '{print $2}')
    JAR_FILE="$PROJECT_ROOT/backend/app/build/libs/tolgee-${VERSION}.jar"
fi

if [ ! -f "$JAR_FILE" ]; then
    echo "Could not find the generated JAR file at $JAR_FILE!"
    exit 1
fi

echo "Found JAR: $JAR_FILE"

# 2. Start Redis via Docker
echo "Starting Redis..."
docker run --name tolgee-test-redis -p 6379:6379 -d --rm redis:alpine

# Function to stop Redis and background processes on exit
PIDS=()
cleanup() {
    echo "Cleaning up..."
    docker stop tolgee-test-redis || true
    for pid in "${PIDS[@]}"; do
        kill "$pid" || true
    done
}
trap cleanup EXIT

# Function to start Tolgee on a specific port
start_instance() {
    local port=$1
    local log_file="$SCRIPT_DIR/tolgee-$port.log"
    
    echo "Starting instance on port $port (logs: tolgee-$port.log)..."
    java -jar "$JAR_FILE" \
      --server.port=$port \
      --spring.data.redis.host=localhost \
      --spring.data.redis.port=6379 \
      --tolgee.cache.use-redis=true > "$log_file" 2>&1 &
    
    local pid=$!
    PIDS+=("$pid")

    echo "Waiting for instance on port $port to start..."
    local MAX_RETRIES=60
    local COUNT=0
    while ! grep -q "Tomcat started on port" "$log_file"; do
        if [ $COUNT -ge $MAX_RETRIES ]; then
            echo "Timeout waiting for instance on port $port to start. Check $log_file"
            exit 1
        fi
        sleep 2
        COUNT=$((COUNT + 1))
        echo -n "."
    done
    echo " Instance on port $port started!"
}

# 3. Start the JAR twice
start_instance 8080
start_instance 8081

# 4. Import the keys
import_file() {
    local port=$1
    local file_path="$KEYS_FILE"

    if [ ! -f "$file_path" ]; then
        echo "File $file_path not found! Generating it using jq..."
        jq -n --arg num "$NUM_KEYS" 'reduce range(1; ($num|tonumber) + 1) as $i ({}; . + {("key" + ($i|tostring)): ("value-" + ($i|tostring))})' > "$file_path"
    fi

    echo "Importing $file_path to instance on port $port..."

    # Get project ID (assuming the demo project is created)
    local project_id=$(curl -s -X GET "http://localhost:$port/v2/projects" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

    if [ -z "$project_id" ]; then
        echo "No project found on instance on port $port"
        return
    fi

    echo "Using Project ID: $project_id"

    # Perform import
    echo "Adding files to import..."
    local add_files_response=$(curl -s -X POST "http://localhost:$port/v2/projects/$project_id/import" \
        -F "files=@$file_path")
    
    echo "Add files response: $add_files_response"

    # Extract importLanguageId
    local import_language_id=$(echo "$add_files_response" | jq -r '.result._embedded.languages[0].id')
    echo "Extracted Import Language ID: $import_language_id"

    echo "Selecting existing language..."
    local select_response=$(curl -s -X PUT "http://localhost:$port/v2/projects/$project_id/import/result/languages/$import_language_id/select-existing/1000000001")
    echo "Select existing response: $select_response"

    echo "Applying import..."
    local apply_response=$(curl -s -X PUT "http://localhost:$port/v2/projects/$project_id/import/apply?forceMode=OVERRIDE")

    echo "Apply response: $apply_response"
}

import_file 8080

echo "Querying the database for key IDs starting with 'key'..."
KEY_IDS_JSON=$(PGPASSWORD=postgres psql -h localhost -p 25432 -U postgres -d postgres -t -A -c "SELECT json_agg(id) FROM public.key WHERE name LIKE 'key%';" | tr -d '[:space:]')

if [ -z "$KEY_IDS_JSON" ] || [ "$KEY_IDS_JSON" == "null" ]; then
    echo "No keys starting with 'key' found in the database."
else
    # Get project ID
    PROJECT_ID=$(curl -s -X GET "http://localhost:8080/v2/projects" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    
    if [ -n "$PROJECT_ID" ] && [ "$PROJECT_ID" != "null" ]; then
        KEY_COUNT=$(echo "$KEY_IDS_JSON" | jq '. | length')
        echo "Found $KEY_COUNT keys starting with 'key'."
        echo "Starting batch machine translation job for project $PROJECT_ID..."
        BATCH_JOB_RESPONSE=$(curl -s -X POST "http://localhost:8080/v2/projects/$PROJECT_ID/start-batch-job/machine-translate" \
            -H "Content-Type: application/json" \
            -d "{
                \"keyIds\": $KEY_IDS_JSON,
                \"targetLanguageIds\": [1000000002]
            }")
        echo "Batch job response: $BATCH_JOB_RESPONSE"

        BATCH_JOB_ID=$(echo "$BATCH_JOB_RESPONSE" | jq -r '.id')

        if [ -n "$BATCH_JOB_ID" ] && [ "$BATCH_JOB_ID" != "null" ]; then
            echo "Monitoring batch job $BATCH_JOB_ID..."
            while true; do
                JOB_STATUS_DATA=$(PGPASSWORD=postgres psql -h localhost -p 25432 -U postgres -d postgres -t -A -F '|' -c "SELECT status, created_at, updated_at FROM public.tolgee_batch_job WHERE id = $BATCH_JOB_ID;")
                
                # Format: STATUS|CREATED_AT|UPDATED_AT
                STATUS=$(echo "$JOB_STATUS_DATA" | cut -d'|' -f1)
                CREATED_AT=$(echo "$JOB_STATUS_DATA" | cut -d'|' -f2)
                UPDATED_AT=$(echo "$JOB_STATUS_DATA" | cut -d'|' -f3)

                # Get chunk execution stats
                CHUNK_STATS=$(PGPASSWORD=postgres psql -h localhost -p 25432 -U postgres -d postgres -t -A -F ':' -c "SELECT status, count(*) FROM public.tolgee_batch_job_chunk_execution WHERE batch_job_id = $BATCH_JOB_ID GROUP BY status order by status;")
                
                # Format chunk stats for output
                STATS_STRING=""
                if [ -n "$CHUNK_STATS" ]; then
                    while IFS=: read -r c_status count; do
                        STATS_STRING+="$c_status: $count, "
                    done <<< "$CHUNK_STATS"
                    STATS_STRING=" (${STATS_STRING%, })"
                fi

                echo "Current status: $STATUS$STATS_STRING"

                if [ "$STATUS" == "SUCCESS" ]; then
                    echo "Batch job finished successfully!"
                    
                    # Calculate duration
                    # PostgreSQL timestamps are like 2026-01-16 21:33:53.123
                    # We use date command to convert them to seconds since epoch
                    # Note: MacOS 'date' command is different from Linux 'date'
                    if [[ "$OSTYPE" == "darwin"* ]]; then
                        # MacOS version
                        START_SEC=$(date -j -f "%Y-%m-%d %H:%M:%S" "$(echo $CREATED_AT | cut -d. -f1)" "+%s")
                        END_SEC=$(date -j -f "%Y-%m-%d %H:%M:%S" "$(echo $UPDATED_AT | cut -d. -f1)" "+%s")
                    else
                        # Linux version
                        START_SEC=$(date -d "$CREATED_AT" "+%s")
                        END_SEC=$(date -d "$UPDATED_AT" "+%s")
                    fi
                    
                    DURATION=$((END_SEC - START_SEC))
                    MIN=$((DURATION / 60))
                    SEC=$((DURATION % 60))
                    
                    echo "Execution time: ${MIN}m ${SEC}s"
                    break
                elif [ "$STATUS" == "FAILED" ] || [ "$STATUS" == "CANCELLED" ]; then
                    echo "Batch job failed or was cancelled with status: $STATUS"
                    exit 1
                fi
                
                sleep 5
            done
        else
            echo "Failed to get batch job ID from response."
        fi
    else
        echo "Could not find project ID for batch job."
    fi
fi

echo "Both instances started, file imported and batch job initiated. Press Ctrl+C to stop."
wait

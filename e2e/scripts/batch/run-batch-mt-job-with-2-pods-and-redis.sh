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

# Parse command line arguments
NUM_KEYS=10000
NUM_INSTANCES=2
CONCURRENCY=20
START_PORT=""
POSTGRES_CONTAINER="tolgee-batch-jobs-perf-test-postgres"
REDIS_CONTAINER="tolgee-batch-jobs-perf-test-redis"
POSTGRES_PORT=25433
REDIS_PORT=6380

usage() {
    echo "Usage: $0 [-k NUM_ITEMS] [-n NUM_INSTANCES] [-C CONCURRENCY] [-p START_PORT] [-c POSTGRES_CONTAINER] [-r REDIS_CONTAINER] [-P POSTGRES_PORT] [-R REDIS_PORT]"
    echo "  -k NUM_ITEMS           Number of items to process (default: 10000)"
    echo "  -n NUM_INSTANCES       Number of Tolgee instances to start (default: 2)"
    echo "  -C CONCURRENCY         Batch job concurrency per instance (default: 20)"
    echo "  -p START_PORT          Starting port number (default: 10020)"
    echo "  -c POSTGRES_CONTAINER  PostgreSQL container name (default: tolgee-batch-jobs-perf-test-postgres)"
    echo "  -r REDIS_CONTAINER     Redis container name (default: tolgee-batch-jobs-perf-test-redis)"
    echo "  -P POSTGRES_PORT       PostgreSQL port (default: 25433)"
    echo "  -R REDIS_PORT          Redis port (default: 6380)"
    exit 1
}

while getopts "k:n:C:p:c:r:P:R:h" opt; do
    case $opt in
        k) NUM_KEYS="$OPTARG" ;;
        n) NUM_INSTANCES="$OPTARG" ;;
        C) CONCURRENCY="$OPTARG" ;;
        p) START_PORT="$OPTARG" ;;
        c) POSTGRES_CONTAINER="$OPTARG" ;;
        r) REDIS_CONTAINER="$OPTARG" ;;
        P) POSTGRES_PORT="$OPTARG" ;;
        R) REDIS_PORT="$OPTARG" ;;
        h) usage ;;
        *) usage ;;
    esac
done

# Use default start port if not specified
if [ -z "$START_PORT" ]; then
    START_PORT=10020
fi

# Remove existing containers if they exist
if [ "$(docker ps -aq -f name=^${REDIS_CONTAINER}$)" ]; then
    echo "Removing existing Redis container '$REDIS_CONTAINER'..."
    docker rm -f "$REDIS_CONTAINER"
fi

if [ "$(docker ps -aq -f name=^${POSTGRES_CONTAINER}$)" ]; then
    echo "Existing docker container '$POSTGRES_CONTAINER' found."
    echo "Removing '$POSTGRES_CONTAINER' container..."
    docker rm -f "$POSTGRES_CONTAINER"
fi

echo "Project root detected at: $PROJECT_ROOT"
echo "Number of items to process: $NUM_KEYS"
echo "Number of instances: $NUM_INSTANCES"
echo "Batch concurrency per instance: $CONCURRENCY"
echo "Starting port: $START_PORT"
echo "PostgreSQL port: $POSTGRES_PORT"
echo "Redis port: $REDIS_PORT"

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

# 2. Start PostgreSQL via Docker
echo "Starting PostgreSQL on port $POSTGRES_PORT..."
docker run --name "$POSTGRES_CONTAINER" \
    -e POSTGRES_PASSWORD=postgres \
    -e POSTGRES_USER=postgres \
    -e POSTGRES_DB=postgres \
    -p "$POSTGRES_PORT:5432" \
    -d --rm postgres:17-alpine

# Wait for PostgreSQL to be ready
echo "Waiting for PostgreSQL to be ready..."
MAX_PG_RETRIES=30
PG_COUNT=0
while ! PGPASSWORD=postgres psql -h localhost -p "$POSTGRES_PORT" -U postgres -c "SELECT 1" &>/dev/null; do
    if [ $PG_COUNT -ge $MAX_PG_RETRIES ]; then
        echo "Timeout waiting for PostgreSQL to start."
        exit 1
    fi
    sleep 1
    PG_COUNT=$((PG_COUNT + 1))
    echo -n "."
done
echo " PostgreSQL is ready!"

# 3. Start Redis via Docker
echo "Starting Redis on port $REDIS_PORT..."
docker run --name "$REDIS_CONTAINER" -p "$REDIS_PORT:6379" -d --rm redis:alpine

# Function to stop containers and background processes on exit
PIDS=()
cleanup() {
    echo "Cleaning up..."
    docker stop "$POSTGRES_CONTAINER" || true
    docker stop "$REDIS_CONTAINER" || true
    for pid in "${PIDS[@]}"; do
        kill "$pid" || true
    done
}
trap cleanup EXIT

# Function to launch Tolgee on a specific port (non-blocking)
launch_instance() {
    local port=$1
    local log_file="$SCRIPT_DIR/tolgee-$port.log"

    echo "Launching instance on port $port (logs: tolgee-$port.log)..."
    java -jar "$JAR_FILE" \
      --server.port=$port \
      --spring.datasource.url="jdbc:postgresql://localhost:$POSTGRES_PORT/postgres" \
      --spring.datasource.username=postgres \
      --spring.datasource.password=postgres \
      --spring.data.redis.host=localhost \
      --spring.data.redis.port="$REDIS_PORT" \
      --tolgee.cache.use-redis=true \
      --tolgee.billing.enabled=false \
      --tolgee.internal.controller-enabled=true \
      --tolgee.postgres-autostart.enabled=false \
      --tolgee.batch.concurrency=$CONCURRENCY \
      > "$log_file" 2>&1 &

    local pid=$!
    PIDS+=("$pid")
}

# Function to wait for all instances to be ready
wait_for_instances() {
    local ports=("$@")
    local MAX_RETRIES=120
    local COUNT=0

    echo "Waiting for ${#ports[@]} instances to start..."

    while true; do
        local all_ready=true
        local ready_count=0

        for port in "${ports[@]}"; do
            local log_file="$SCRIPT_DIR/tolgee-$port.log"
            if grep -q "Tomcat started on port" "$log_file" 2>/dev/null; then
                ready_count=$((ready_count + 1))
            else
                all_ready=false
            fi
        done

        if [ "$all_ready" = true ]; then
            echo ""
            echo "All ${#ports[@]} instances started!"
            return 0
        fi

        if [ $COUNT -ge $MAX_RETRIES ]; then
            echo ""
            echo "Timeout waiting for instances to start. Check log files."
            exit 1
        fi

        sleep 2
        COUNT=$((COUNT + 1))
        echo -ne "\r  Ready: $ready_count/${#ports[@]} (attempt $COUNT/$MAX_RETRIES)"
    done
}

# 4. Start the JAR instances (first one starts earlier to initialize database schema)
PORTS=()
for i in $(seq 0 $((NUM_INSTANCES - 1))); do
    PORT=$((START_PORT + i))
    PORTS+=("$PORT")
    launch_instance "$PORT"

    # After starting the first instance, wait 3 seconds before starting others
    # This prevents race conditions with database schema initialization (Liquibase)
    if [ $i -eq 0 ] && [ $NUM_INSTANCES -gt 1 ]; then
        echo "Waiting 3 seconds for first instance to initialize database schema..."
        sleep 3
    fi
done

# Wait for all instances to be ready
wait_for_instances "${PORTS[@]}"

# Use the first port for API calls
PRIMARY_PORT="${PORTS[0]}"

# 5. Generate fake item IDs and start NO_OP batch job
echo "Generating $NUM_KEYS fake item IDs..."
ITEM_IDS_JSON=$(jq -n --arg num "$NUM_KEYS" '[range(1; ($num|tonumber) + 1)]')

echo "Starting NO_OP batch job with $NUM_KEYS items..."
BATCH_JOB_RESPONSE=$(curl -s -X POST "http://localhost:$PRIMARY_PORT/internal/batch-jobs/start-no-op" \
    -H "Content-Type: application/json" \
    -d "{\"itemIds\": $ITEM_IDS_JSON}")
echo "Batch job response: $BATCH_JOB_RESPONSE"

BATCH_JOB_ID=$(echo "$BATCH_JOB_RESPONSE" | jq -r '.id')

if [ -z "$BATCH_JOB_ID" ] || [ "$BATCH_JOB_ID" == "null" ]; then
    echo "Error: Failed to get batch job ID from response."
    exit 1
fi

echo "Monitoring batch job $BATCH_JOB_ID..."
while true; do
    JOB_STATUS_DATA=$(PGPASSWORD=postgres psql -h localhost -p $POSTGRES_PORT -U postgres -d postgres -t -A -F '|' -c "SELECT status, created_at, updated_at FROM public.tolgee_batch_job WHERE id = $BATCH_JOB_ID;")

    # Format: STATUS|CREATED_AT|UPDATED_AT
    STATUS=$(echo "$JOB_STATUS_DATA" | cut -d'|' -f1)
    CREATED_AT=$(echo "$JOB_STATUS_DATA" | cut -d'|' -f2)
    UPDATED_AT=$(echo "$JOB_STATUS_DATA" | cut -d'|' -f3)

    # Get chunk execution stats
    CHUNK_STATS=$(PGPASSWORD=postgres psql -h localhost -p $POSTGRES_PORT -U postgres -d postgres -t -A -F ':' -c "SELECT status, count(*) FROM public.tolgee_batch_job_chunk_execution WHERE batch_job_id = $BATCH_JOB_ID GROUP BY status order by status;")

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

echo "All $NUM_INSTANCES instances started (ports: ${PORTS[*]}), NO_OP batch job completed. Press Ctrl+C to stop."
wait

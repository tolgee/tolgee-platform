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
NUM_JOBS=1
DELAY_MS=0
NUM_PROJECTS=0

usage() {
    echo "Usage: $0 [-k NUM_ITEMS] [-n NUM_INSTANCES] [-C CONCURRENCY] [-j NUM_JOBS] [-d DELAY_MS] [-e NUM_PROJECTS] [-p START_PORT] [-c POSTGRES_CONTAINER] [-r REDIS_CONTAINER] [-P POSTGRES_PORT] [-R REDIS_PORT]"
    echo "  -k NUM_ITEMS           Number of items to process (default: 10000)"
    echo "  -n NUM_INSTANCES       Number of Tolgee instances to start (default: 2)"
    echo "  -C CONCURRENCY         Batch job concurrency per instance (default: 20)"
    echo "  -j NUM_JOBS            Number of jobs to split items across (default: 1)"
    echo "  -d DELAY_MS            Chunk processing delay in milliseconds (default: 0)"
    echo "  -e NUM_PROJECTS        Number of projects for exclusive mode (default: 0 = non-exclusive)"
    echo "  -p START_PORT          Starting port number (default: 10020)"
    echo "  -c POSTGRES_CONTAINER  PostgreSQL container name (default: tolgee-batch-jobs-perf-test-postgres)"
    echo "  -r REDIS_CONTAINER     Redis container name (default: tolgee-batch-jobs-perf-test-redis)"
    echo "  -P POSTGRES_PORT       PostgreSQL port (default: 25433)"
    echo "  -R REDIS_PORT          Redis port (default: 6380)"
    exit 1
}

while getopts "k:n:C:p:c:r:P:R:j:d:e:h" opt; do
    case $opt in
        k) NUM_KEYS="$OPTARG" ;;
        n) NUM_INSTANCES="$OPTARG" ;;
        C) CONCURRENCY="$OPTARG" ;;
        p) START_PORT="$OPTARG" ;;
        c) POSTGRES_CONTAINER="$OPTARG" ;;
        r) REDIS_CONTAINER="$OPTARG" ;;
        P) POSTGRES_PORT="$OPTARG" ;;
        R) REDIS_PORT="$OPTARG" ;;
        j) NUM_JOBS="$OPTARG" ;;
        d) DELAY_MS="$OPTARG" ;;
        e) NUM_PROJECTS="$OPTARG" ;;
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
echo "Number of jobs: $NUM_JOBS"
echo "Chunk processing delay: ${DELAY_MS}ms"
if [ "$NUM_PROJECTS" -gt 0 ]; then
    echo "Projects: $NUM_PROJECTS (exclusive mode)"
else
    echo "Projects: none (non-exclusive mode)"
fi
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

# Function to fetch and print timing reports from all instances
print_timing_reports() {
    echo ""
    echo "================================================================================"
    echo "BATCH JOB OPERATION TIMING REPORTS"
    echo "================================================================================"

    for port in "${PORTS[@]}"; do
        local report
        report=$(curl -s "http://localhost:$port/internal/batch-jobs/timing-report" 2>/dev/null)

        if [ -z "$report" ] || [ "$report" = "{}" ] || [ "$report" = "null" ]; then
            echo ""
            echo "Instance :$port — no timing data available"
            continue
        fi

        echo ""
        echo "Instance :$port"
        echo "--------------------------------------------------------------------------------"
        printf "%-45s %8s %12s %10s %10s %10s\n" "Operation" "Count" "Total(ms)" "Avg(ms)" "Min(ms)" "Max(ms)"
        echo "--------------------------------------------------------------------------------"

        # Parse JSON and print table rows, sorted by totalMs descending
        echo "$report" | jq -r '
            to_entries
            | sort_by(-.value.totalMs)
            | .[]
            | [.key, .value.count, .value.totalMs, .value.avgMs, .value.minMs, .value.maxMs]
            | @tsv' | while IFS=$'\t' read -r op count total avg min max; do
            printf "%-45s %8s %12.2f %10.2f %10.2f %10.2f\n" "$op" "$count" "$total" "$avg" "$min" "$max"
        done

        echo "--------------------------------------------------------------------------------"
        # Print summary line
        local total_ops total_time
        total_ops=$(echo "$report" | jq '[.[] | .count] | add')
        total_time=$(echo "$report" | jq '[.[] | .totalMs] | add')
        echo "Total operations: $total_ops | Total measured time: ${total_time}ms"
    done

    # Print aggregated report across all instances
    echo ""
    echo "================================================================================"
    echo "AGGREGATED TIMING (all instances combined)"
    echo "================================================================================"
    printf "%-45s %8s %12s %10s %10s %10s\n" "Operation" "Count" "Total(ms)" "Avg(ms)" "Min(ms)" "Max(ms)"
    echo "--------------------------------------------------------------------------------"

    # Collect all reports into a temp file for aggregation
    local all_reports=""
    for port in "${PORTS[@]}"; do
        local report
        report=$(curl -s "http://localhost:$port/internal/batch-jobs/timing-report" 2>/dev/null)
        if [ -n "$report" ] && [ "$report" != "{}" ] && [ "$report" != "null" ]; then
            if [ -z "$all_reports" ]; then
                all_reports="$report"
            else
                all_reports="$all_reports
$report"
            fi
        fi
    done

    if [ -n "$all_reports" ]; then
        echo "$all_reports" | jq -rs '
            reduce .[] as $report ({};
                reduce ($report | to_entries[]) as $entry (.;
                    .[$entry.key].count += ($entry.value.count // 0)
                    | .[$entry.key].totalMs += ($entry.value.totalMs // 0)
                    | .[$entry.key].minMs = (
                        if .[$entry.key].minMs == null then $entry.value.minMs
                        elif ($entry.value.minMs // 999999) < .[$entry.key].minMs then $entry.value.minMs
                        else .[$entry.key].minMs end
                      )
                    | .[$entry.key].maxMs = (
                        if .[$entry.key].maxMs == null then $entry.value.maxMs
                        elif ($entry.value.maxMs // 0) > .[$entry.key].maxMs then $entry.value.maxMs
                        else .[$entry.key].maxMs end
                      )
                )
            )
            | to_entries
            | map(.value.avgMs = (if .value.count > 0 then .value.totalMs / .value.count else 0 end))
            | sort_by(-.value.totalMs)
            | .[]
            | [.key, .value.count, .value.totalMs, .value.avgMs, .value.minMs, .value.maxMs]
            | @tsv' | while IFS=$'\t' read -r op count total avg min max; do
            printf "%-45s %8s %12.2f %10.2f %10.2f %10.2f\n" "$op" "$count" "$total" "$avg" "$min" "$max"
        done

        echo "--------------------------------------------------------------------------------"
        local grand_total_ops grand_total_time
        grand_total_ops=$(echo "$all_reports" | jq -s '[.[] | to_entries[] | .value.count] | add')
        grand_total_time=$(echo "$all_reports" | jq -s '[.[] | to_entries[] | .value.totalMs] | add')
        echo "Total operations: $grand_total_ops | Total measured time: ${grand_total_time}ms"
    fi

    echo "================================================================================"
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

# 5. Start NO_OP batch job(s)
BATCH_JOB_IDS=()

if [ "$NUM_JOBS" -gt 1 ] || [ "$NUM_PROJECTS" -gt 0 ]; then
    # Use multi endpoint when multiple jobs or when projects are needed for exclusive mode
    if [ "$NUM_JOBS" -le 1 ]; then
        NUM_JOBS=1
    fi
    EXCLUSIVE_INFO=""
    if [ "$NUM_PROJECTS" -gt 0 ]; then
        EXCLUSIVE_INFO=" across $NUM_PROJECTS project(s) (exclusive)"
    fi
    echo "Starting $NUM_JOBS NO_OP batch jobs with $NUM_KEYS total items${EXCLUSIVE_INFO} (delay: ${DELAY_MS}ms)..."
    MULTI_RESPONSE=$(curl -s -X POST "http://localhost:$PRIMARY_PORT/internal/batch-jobs/start-no-op-multi" \
        -H "Content-Type: application/json" \
        -d "{\"totalItems\": $NUM_KEYS, \"numberOfJobs\": $NUM_JOBS, \"chunkProcessingDelayMs\": $DELAY_MS, \"numberOfProjects\": $NUM_PROJECTS}")
    echo "Multi-job response: $MULTI_RESPONSE"

    # Extract all job IDs from the array response
    while IFS= read -r id; do
        BATCH_JOB_IDS+=("$id")
    done < <(echo "$MULTI_RESPONSE" | jq -r '.[].id')

    if [ ${#BATCH_JOB_IDS[@]} -eq 0 ]; then
        echo "Error: Failed to get batch job IDs from response."
        exit 1
    fi
    echo "Created ${#BATCH_JOB_IDS[@]} batch jobs: ${BATCH_JOB_IDS[*]}"
else
    echo "Generating $NUM_KEYS fake item IDs..."
    ITEM_IDS_JSON=$(jq -n --arg num "$NUM_KEYS" '[range(1; ($num|tonumber) + 1)]')

    echo "Starting NO_OP batch job with $NUM_KEYS items (delay: ${DELAY_MS}ms)..."
    BATCH_JOB_RESPONSE=$(curl -s -X POST "http://localhost:$PRIMARY_PORT/internal/batch-jobs/start-no-op" \
        -H "Content-Type: application/json" \
        -d "{\"itemIds\": $ITEM_IDS_JSON, \"chunkProcessingDelayMs\": $DELAY_MS}")
    echo "Batch job response: $BATCH_JOB_RESPONSE"

    BATCH_JOB_ID=$(echo "$BATCH_JOB_RESPONSE" | jq -r '.id')

    if [ -z "$BATCH_JOB_ID" ] || [ "$BATCH_JOB_ID" == "null" ]; then
        echo "Error: Failed to get batch job ID from response."
        exit 1
    fi
    BATCH_JOB_IDS+=("$BATCH_JOB_ID")
fi

# Build SQL IN clause for job IDs
JOB_IDS_SQL=$(IFS=,; echo "${BATCH_JOB_IDS[*]}")

echo "Monitoring ${#BATCH_JOB_IDS[@]} batch job(s): ${BATCH_JOB_IDS[*]}..."
while true; do
    # Query status for all jobs
    ALL_JOBS_DATA=$(PGPASSWORD=postgres psql -h localhost -p $POSTGRES_PORT -U postgres -d postgres -t -A -F '|' -c \
        "SELECT id, status, created_at, updated_at FROM public.tolgee_batch_job WHERE id IN ($JOB_IDS_SQL) ORDER BY id;")

    # Get chunk execution stats across all jobs
    CHUNK_STATS=$(PGPASSWORD=postgres psql -h localhost -p $POSTGRES_PORT -U postgres -d postgres -t -A -F ':' -c \
        "SELECT status, count(*) FROM public.tolgee_batch_job_chunk_execution WHERE batch_job_id IN ($JOB_IDS_SQL) GROUP BY status ORDER BY status;")

    # Count job statuses
    TOTAL_JOBS=${#BATCH_JOB_IDS[@]}
    SUCCESS_COUNT=0
    FAILED_COUNT=0
    RUNNING_COUNT=0
    PENDING_COUNT=0
    EARLIEST_CREATED=""
    LATEST_UPDATED=""

    while IFS='|' read -r j_id j_status j_created j_updated; do
        [ -z "$j_id" ] && continue
        case "$j_status" in
            SUCCESS) SUCCESS_COUNT=$((SUCCESS_COUNT + 1)) ;;
            FAILED|CANCELLED) FAILED_COUNT=$((FAILED_COUNT + 1)) ;;
            RUNNING) RUNNING_COUNT=$((RUNNING_COUNT + 1)) ;;
            *) PENDING_COUNT=$((PENDING_COUNT + 1)) ;;
        esac
        # Track earliest created and latest updated
        if [ -z "$EARLIEST_CREATED" ] || [[ "$j_created" < "$EARLIEST_CREATED" ]]; then
            EARLIEST_CREATED="$j_created"
        fi
        if [ -z "$LATEST_UPDATED" ] || [[ "$j_updated" > "$LATEST_UPDATED" ]]; then
            LATEST_UPDATED="$j_updated"
        fi
    done <<< "$ALL_JOBS_DATA"

    # Format chunk stats
    STATS_STRING=""
    if [ -n "$CHUNK_STATS" ]; then
        while IFS=: read -r c_status count; do
            [ -z "$c_status" ] && continue
            STATS_STRING+="$c_status: $count, "
        done <<< "$CHUNK_STATS"
        STATS_STRING=" chunks(${STATS_STRING%, })"
    fi

    echo "Jobs: ${SUCCESS_COUNT}/${TOTAL_JOBS} done, ${RUNNING_COUNT} running, ${PENDING_COUNT} pending, ${FAILED_COUNT} failed${STATS_STRING}"

    if [ $((SUCCESS_COUNT + FAILED_COUNT)) -eq $TOTAL_JOBS ]; then
        if [ $FAILED_COUNT -gt 0 ]; then
            echo "Some batch jobs failed! ($FAILED_COUNT/$TOTAL_JOBS failed)"
            exit 1
        fi

        echo "All ${TOTAL_JOBS} batch job(s) finished successfully!"

        # Calculate duration using earliest created and latest updated
        CREATED_AT="$EARLIEST_CREATED"
        UPDATED_AT="$LATEST_UPDATED"

        if [[ "$OSTYPE" == "darwin"* ]]; then
            START_SEC=$(date -j -f "%Y-%m-%d %H:%M:%S" "$(echo $CREATED_AT | cut -d. -f1)" "+%s")
            END_SEC=$(date -j -f "%Y-%m-%d %H:%M:%S" "$(echo $UPDATED_AT | cut -d. -f1)" "+%s")
        else
            START_SEC=$(date -d "$CREATED_AT" "+%s")
            END_SEC=$(date -d "$UPDATED_AT" "+%s")
        fi

        DURATION=$((END_SEC - START_SEC))
        MIN=$((DURATION / 60))
        SEC=$((DURATION % 60))

        echo "Execution time: ${MIN}m ${SEC}s"

        # Fetch and print timing reports from all instances
        print_timing_reports

        # Query actual chunk count from DB
        CHUNKS=$(PGPASSWORD=postgres psql -h localhost -p $POSTGRES_PORT -U postgres -d postgres -t -A -c \
            "SELECT count(*) FROM public.tolgee_batch_job_chunk_execution WHERE batch_job_id IN ($JOB_IDS_SQL);")

        if [ "$DURATION" -gt 0 ]; then
            THROUGHPUT=$(echo "scale=2; $CHUNKS / $DURATION" | bc)
            ITEMS_PER_SEC=$(echo "scale=2; $NUM_KEYS / $DURATION" | bc)
        else
            THROUGHPUT="N/A"
            ITEMS_PER_SEC="N/A"
        fi
        echo ""
        echo "================================================================================"
        echo "THROUGHPUT SUMMARY"
        echo "================================================================================"
        PROJ_INFO=""
        if [ "$NUM_PROJECTS" -gt 0 ]; then
            PROJ_INFO=" | Projects: $NUM_PROJECTS (exclusive)"
        fi
        echo "  Items: $NUM_KEYS | Chunks: $CHUNKS | Jobs: $TOTAL_JOBS | Instances: $NUM_INSTANCES | Concurrency: $CONCURRENCY | Delay: ${DELAY_MS}ms${PROJ_INFO}"
        echo "  Duration: ${MIN}m ${SEC}s (${DURATION}s)"
        echo "  Throughput: ${THROUGHPUT} chunks/s | ${ITEMS_PER_SEC} items/s"
        echo "================================================================================"
        break
    fi

    sleep 5
done

echo "All $NUM_INSTANCES instances started (ports: ${PORTS[*]}), NO_OP batch job(s) completed. Press Ctrl+C to stop."
wait

#!/bin/bash
# Common functions for import performance test scripts.
# Source this file — do not execute directly.

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[1]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/../../.." && pwd )"

# Defaults
NUM_KEYS=1000000
NUM_LANGUAGES=5
PORT=10030
POSTGRES_CONTAINER="tolgee-import-perf-test-postgres"
POSTGRES_PORT=25434
SKIP_BUILD=false

parse_args() {
    while getopts "k:l:p:c:P:Sh" opt; do
        case $opt in
            k) NUM_KEYS="$OPTARG" ;;
            l) NUM_LANGUAGES="$OPTARG" ;;
            p) PORT="$OPTARG" ;;
            c) POSTGRES_CONTAINER="$OPTARG" ;;
            P) POSTGRES_PORT="$OPTARG" ;;
            S) SKIP_BUILD=true ;;
            h) usage ;;
            *) usage ;;
        esac
    done
    TOTAL_TRANSLATIONS=$((NUM_KEYS * NUM_LANGUAGES))
}

usage() {
    echo "Usage: $0 [-k NUM_KEYS] [-l NUM_LANGUAGES] [-p PORT] [-c POSTGRES_CONTAINER] [-P POSTGRES_PORT] [-S]"
    echo "  -k NUM_KEYS            Number of keys to import (default: 1000000)"
    echo "  -l NUM_LANGUAGES       Number of languages (default: 5)"
    echo "  -p PORT                Tolgee server port (default: 10030)"
    echo "  -c POSTGRES_CONTAINER  PostgreSQL container name (default: tolgee-import-perf-test-postgres)"
    echo "  -P POSTGRES_PORT       PostgreSQL port (default: 25434)"
    echo "  -S                     Skip JAR build (use existing JAR)"
    exit 1
}

check_tools() {
    if ! command -v jq &> /dev/null; then
        echo "Error: jq is not installed. Please install it:"
        echo "brew install jq"
        exit 1
    fi
}

build_jar() {
    cd "$PROJECT_ROOT"
    if [ "$SKIP_BUILD" = false ]; then
        echo "Building the JAR..."
        ./gradlew clean :server-app:bootJar
    fi

    JAR_FILE="$PROJECT_ROOT/backend/app/build/libs/tolgee-local.jar"
    if [ ! -f "$JAR_FILE" ]; then
        VERSION=$(./gradlew -q properties --console=plain | grep "^version:" | awk '{print $2}')
        JAR_FILE="$PROJECT_ROOT/backend/app/build/libs/tolgee-${VERSION}.jar"
    fi

    if [ ! -f "$JAR_FILE" ]; then
        echo "Could not find the generated JAR file!"
        exit 1
    fi
    echo "Found JAR: $JAR_FILE"
}

start_postgres() {
    if [ "$(docker ps -aq -f name=^${POSTGRES_CONTAINER}$)" ]; then
        echo "Removing existing PostgreSQL container '$POSTGRES_CONTAINER'..."
        docker rm -f "$POSTGRES_CONTAINER"
    fi

    echo "Starting PostgreSQL on port $POSTGRES_PORT..."
    docker run --name "$POSTGRES_CONTAINER" \
        -e POSTGRES_PASSWORD=postgres \
        -e POSTGRES_USER=postgres \
        -e POSTGRES_DB=postgres \
        -p "$POSTGRES_PORT:5432" \
        -d --rm postgres:17-alpine \
        postgres -c max_connections=10000 -c random_page_cost=1.0 \
        -c fsync=off -c synchronous_commit=off -c full_page_writes=off

    echo -n "Waiting for PostgreSQL..."
    local max_retries=30 count=0
    while ! docker exec "$POSTGRES_CONTAINER" pg_isready -U postgres &>/dev/null; do
        if [ $count -ge $max_retries ]; then
            echo " timeout!"
            exit 1
        fi
        sleep 1
        count=$((count + 1))
        echo -n "."
    done
    echo " ready!"
}

TOLGEE_PID=""
LOG_FILE="$SCRIPT_DIR/tolgee-import-test.log"

start_tolgee() {
    echo "Starting Tolgee on port $PORT (logs: $LOG_FILE)..."
    # Connection properties mirror production (deployment/azure/production/tolgee/values.yaml):
    #   reWriteBatchedInserts=true  — combine multi-row INSERT VALUES
    #   prepareThreshold=0         — disable server-side prepared statements
    #   preparedStatementCacheQueries=0
    # Hikari pool settings also match production (maximum-pool-size=100, minimum-idle=10).
    java -Xmx12g -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath="$SCRIPT_DIR/heapdump.hprof" -jar "$JAR_FILE" \
        --server.port=$PORT \
        --spring.datasource.url="jdbc:postgresql://localhost:$POSTGRES_PORT/postgres?reWriteBatchedInserts=true&prepareThreshold=0&preparedStatementCacheQueries=0" \
        --spring.datasource.username=postgres \
        --spring.datasource.password=postgres \
        --spring.datasource.type=com.zaxxer.hikari.HikariDataSource \
        --spring.datasource.hikari.maximum-pool-size=100 \
        --spring.datasource.hikari.minimum-idle=10 \
        --tolgee.billing.enabled=false \
        --tolgee.postgres-autostart.enabled=false \
        --tolgee.authentication.initial-username=admin \
        --tolgee.authentication.initial-password=admin \
        --tolgee.max-upload-file-size=512000 \
        > "$LOG_FILE" 2>&1 &

    TOLGEE_PID=$!

    echo -n "Waiting for Tolgee to start..."
    local max_retries=120 count=0
    while ! grep -q "Tomcat started on port" "$LOG_FILE" 2>/dev/null; do
        if ! kill -0 "$TOLGEE_PID" 2>/dev/null; then
            echo " process died! Check $LOG_FILE"
            exit 1
        fi
        if [ $count -ge $max_retries ]; then
            echo " timeout!"
            exit 1
        fi
        sleep 2
        count=$((count + 1))
        echo -n "."
    done
    echo " started!"
}

setup_cleanup() {
    cleanup() {
        echo ""
        echo "Cleaning up..."
        if [ -n "$TOLGEE_PID" ]; then
            kill "$TOLGEE_PID" 2>/dev/null || true
            wait "$TOLGEE_PID" 2>/dev/null || true
        fi
        docker stop "$POSTGRES_CONTAINER" 2>/dev/null || true
        rm -f "$SCRIPT_DIR"/import-lang-*.json
    }
    trap cleanup EXIT
}

authenticate() {
    BASE_URL="http://localhost:$PORT"

    echo -n "Signing in..."
    local max_retries=30 count=0
    while true; do
        LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/public/generatetoken" \
            -H "Content-Type: application/json" \
            -d '{"username":"admin","password":"admin"}')
        JWT_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.accessToken')

        if [ -n "$JWT_TOKEN" ] && [ "$JWT_TOKEN" != "null" ]; then
            break
        fi

        if [ $count -ge $max_retries ]; then
            echo ""
            echo "Error: Failed to get JWT token after $max_retries attempts."
            echo "$LOGIN_RESPONSE"
            exit 1
        fi
        sleep 1
        count=$((count + 1))
        echo -n "."
    done
    echo " authenticated."
}

create_project() {
    ORG_ID=$(curl -s "$BASE_URL/v2/organizations?size=1" \
        -H "Authorization: Bearer $JWT_TOKEN" | jq -r '._embedded.organizations[0].id')

    echo "Creating project (org: $ORG_ID)..."
    PROJECT_RESPONSE=$(curl -s -X POST "$BASE_URL/v2/projects" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -d "{\"name\":\"Import Perf Test\",\"organizationId\":$ORG_ID,\"languages\":[{\"tag\":\"en\",\"name\":\"English\",\"originalName\":\"English\"}]}")
    PROJECT_ID=$(echo "$PROJECT_RESPONSE" | jq -r '.id')

    if [ -z "$PROJECT_ID" ] || [ "$PROJECT_ID" == "null" ]; then
        echo "Error: Failed to create project."
        echo "$PROJECT_RESPONSE"
        exit 1
    fi
    echo "Project created (ID: $PROJECT_ID)"

    for i in $(seq 2 $NUM_LANGUAGES); do
        curl -s -X POST "$BASE_URL/v2/projects/$PROJECT_ID/languages" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $JWT_TOKEN" \
            -d "{\"tag\":\"lang-$i\",\"name\":\"Language $i\",\"originalName\":\"Language $i\"}" > /dev/null
    done
    echo "Created $NUM_LANGUAGES languages."
}

generate_import_files() {
    echo "Generating $NUM_LANGUAGES JSON files with $NUM_KEYS keys each..."

    LANG_TAGS=("en")
    for i in $(seq 2 $NUM_LANGUAGES); do
        LANG_TAGS+=("lang-$i")
    done

    for tag in "${LANG_TAGS[@]}"; do
        local file="$SCRIPT_DIR/import-lang-$tag.json"
        jq -n --arg tag "$tag" --argjson n "$NUM_KEYS" \
            '[range(1; $n + 1) | {("key_\(.)"): "value_\($tag)_\(.)"}] | add' \
            > "$file"
        local file_size
        file_size=$(du -h "$file" | cut -f1)
        echo "  Generated $file ($file_size)"
    done
}

build_curl_file_args() {
    CURL_ARGS=()
    for tag in "${LANG_TAGS[@]}"; do
        CURL_ARGS+=(-F "files=@$SCRIPT_DIR/import-lang-$tag.json;filename=$tag.json")
    done
}

# Common setup: call all infrastructure steps
setup_all() {
    check_tools
    setup_cleanup
    build_jar
    start_postgres
    start_tolgee
    authenticate
    create_project
    generate_import_files
    build_curl_file_args
}

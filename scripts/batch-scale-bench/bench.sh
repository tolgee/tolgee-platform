#!/usr/bin/env bash
# Batch operations scale benchmark driver. See README.md.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"

: "${BENCH_HOME:=$REPO/build/batch-scale-bench}"
: "${BENCH_PG_PORT:=54715}"
: "${BENCH_REDIS_PORT:=53715}"
: "${BENCH_BASE_APP_PORT:=18715}"
: "${BENCH_HEAP:=10g}"
: "${BENCH_BATCH_CONCURRENCY:=10}"
: "${PG_BIN:=/opt/homebrew/opt/postgresql@17/bin}"
: "${REDIS_BIN:=/opt/homebrew/opt/redis/bin}"
: "${BENCH_JAVA_HOME:=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home}"
export JAVA_HOME="$BENCH_JAVA_HOME"
export BENCH_PG_PORT BENCH_REDIS_PORT
export BENCH_OUT="${BENCH_OUT:-$BENCH_HOME/runs}"

JAR="${BENCH_JAR:-$REPO/backend/app/build/libs/tolgee-local.jar}"
mkdir -p "$BENCH_HOME"

pg_up() {
  if [ ! -d "$BENCH_HOME/pgdata" ]; then
    "$PG_BIN/initdb" -D "$BENCH_HOME/pgdata" -U postgres --locale=en_US.UTF-8 -E UTF-8 >/dev/null
    cat >>"$BENCH_HOME/pgdata/postgresql.conf" <<EOF
port = $BENCH_PG_PORT
listen_addresses = 'localhost'
max_connections = 300
shared_buffers = ${BENCH_PG_SHARED_BUFFERS:-4GB}
work_mem = 32MB
maintenance_work_mem = 1GB
effective_cache_size = 16GB
max_wal_size = 16GB
checkpoint_timeout = 15min
random_page_cost = 1.1
shared_preload_libraries = 'pg_stat_statements'
pg_stat_statements.max = 10000
pg_stat_statements.track = all
track_io_timing = on
log_min_duration_statement = 5000
logging_collector = on
log_directory = 'log'
max_parallel_workers_per_gather = 4
EOF
  fi
  if ! "$PG_BIN/pg_ctl" -D "$BENCH_HOME/pgdata" status >/dev/null 2>&1; then
    "$PG_BIN/pg_ctl" -D "$BENCH_HOME/pgdata" -l "$BENCH_HOME/pg.log" start
    sleep 2
  fi
  "$PG_BIN/psql" -p "$BENCH_PG_PORT" -U postgres -h localhost -tc "select 1 from pg_database where datname = 'tolgee'" |
    grep -q 1 || "$PG_BIN/psql" -p "$BENCH_PG_PORT" -U postgres -h localhost -c "create database tolgee"
}

redis_up() {
  if ! "$REDIS_BIN/redis-cli" -p "$BENCH_REDIS_PORT" ping >/dev/null 2>&1; then
    "$REDIS_BIN/redis-server" --port "$BENCH_REDIS_PORT" --save '' --appendonly no --maxmemory 2gb \
      --daemonize yes --pidfile "$BENCH_HOME/redis.pid" --logfile "$BENCH_HOME/redis.log" --dir "$BENCH_HOME"
  fi
}

start_nodes() {
  local nodes="${1:-1}"
  local use_redis=false
  if [ "$nodes" -gt 1 ] || [ "${BENCH_USE_REDIS:-false}" = true ]; then use_redis=true; redis_up; fi
  for i in $(seq 1 "$nodes"); do
    local port=$((BENCH_BASE_APP_PORT + i - 1))
    BENCH_APP_PORT=$port BENCH_USE_REDIS=$use_redis BENCH_BATCH_CONCURRENCY=$BENCH_BATCH_CONCURRENCY \
      nohup "$JAVA_HOME/bin/java" -Xms2g "-Xmx$BENCH_HEAP" -XX:+UseG1GC \
      -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints -XX:+EnableDynamicAgentLoading \
      ${BENCH_JVM_ARGS:-} -jar "$JAR" --spring.config.additional-location="file:$HERE/bench-config.yaml" ${BENCH_APP_ARGS:-} \
      >"$BENCH_HOME/app-node$i.log" 2>&1 &
    echo $! >"$BENCH_HOME/app-node$i.pid"
  done
  for i in $(seq 1 "$nodes"); do
    local port=$((BENCH_BASE_APP_PORT + i - 1))
    local up=false
    for _ in $(seq 1 120); do
      if curl -sf "localhost:$port/actuator/health" >/dev/null 2>&1; then up=true; break; fi
      kill -0 "$(cat "$BENCH_HOME/app-node$i.pid")" 2>/dev/null || break
      sleep 2
    done
    if [ "$up" != true ]; then echo "node$i failed to start, see $BENCH_HOME/app-node$i.log" >&2; exit 1; fi
    echo "node$i up on $port (pid $(cat "$BENCH_HOME/app-node$i.pid"), redis=$use_redis)"
  done
}

stop_nodes() {
  for f in "$BENCH_HOME"/app-node*.pid; do
    [ -f "$f" ] || continue
    local pid
    pid=$(cat "$f")
    kill "$pid" 2>/dev/null || true
    for _ in $(seq 1 60); do kill -0 "$pid" 2>/dev/null || break; sleep 1; done
    kill -9 "$pid" 2>/dev/null || true
    rm -f "$f"
  done
}

node_env() {
  local urls="" pids=""
  for f in "$BENCH_HOME"/app-node*.pid; do
    [ -f "$f" ] || continue
    local i=${f##*app-node}; i=${i%.pid}
    urls="$urls${urls:+,}http://localhost:$((BENCH_BASE_APP_PORT + i - 1))"
    pids="$pids${pids:+,}$(cat "$f")"
  done
  export BENCH_APP_URLS="$urls" BENCH_PIDS="$pids"
}

case "${1:-}" in
  infra-up) pg_up ;;
  infra-down)
    stop_nodes
    "$PG_BIN/pg_ctl" -D "$BENCH_HOME/pgdata" stop || true
    [ -f "$BENCH_HOME/redis.pid" ] && kill "$(cat "$BENCH_HOME/redis.pid")" || true
    ;;
  build) (cd "$REPO" && ./gradlew :server-app:bootJar --no-watch-fs --console=plain) ;;
  start) pg_up; start_nodes "${2:-1}" ;;
  stop) stop_nodes ;;
  restart) stop_nodes; start_nodes "${2:-1}" ;;
  seed) shift; node_env; python3 "$HERE/bench.py" seed "$@" ;;
  add-langs) shift; node_env; python3 "$HERE/bench.py" add-langs "$@" ;;
  run) shift; node_env; python3 "$HERE/bench.py" run "$@" ;;
  *)
    echo "usage: $0 infra-up|infra-down|build|start [nodes]|stop|restart [nodes]|seed ...|add-langs ...|run ..." >&2
    exit 1
    ;;
esac

# Batch operations scale benchmark

Repeatable benchmark for batch jobs on large projects (100k to millions of keys, dozens of languages).
It is not part of CI. It runs one or more real Tolgee server nodes (the `bootJar`) against a local
Postgres (and Redis for multi-node), seeds data straight into the schema with `generate_series`, starts
batch jobs through the REST API and records timing, DB, Redis, JVM and profiler data per scenario.

## Requirements

- JDK 25 (`/opt/homebrew/opt/openjdk@25` by default, override with `BENCH_JAVA_HOME`)
- PostgreSQL 17 binaries with `pg_stat_statements` (`brew install postgresql@17`), Redis (`brew install redis`)
- Optional: async-profiler (`brew install async-profiler`) for `--profile cpu|wall|alloc|lock`
- Python 3 (stdlib only)

Postgres and Redis run natively (not in Docker) so the database gets real memory and CPUs. The data
directory, logs and results live in `BENCH_HOME` (default `build/batch-scale-bench`).

## One command

```bash
export BENCH_HOME=/path/to/bench-home        # optional
scripts/batch-scale-bench/bench.sh build      # bootJar
scripts/batch-scale-bench/bench.sh start 1    # Postgres + 1 node (2+ nodes also start Redis)
scripts/batch-scale-bench/bench.sh seed --name p100k --keys 100000 --langs 30 --fill 0.7
scripts/batch-scale-bench/bench.sh run --project p100k --scenarios all --label baseline --profile wall
scripts/batch-scale-bench/bench.sh stop
scripts/batch-scale-bench/bench.sh infra-down
```

Environment knobs: `BENCH_HEAP` (default `10g`), `BENCH_BATCH_CONCURRENCY` (`tolgee.batch.concurrency`, default
10), `BENCH_PG_PORT` (54715), `BENCH_REDIS_PORT` (53715), `BENCH_BASE_APP_PORT` (18715, node N listens on base+N-1),
`BENCH_USE_REDIS=true` (Redis also for a single node). Server config: `bench-config.yaml` (fake MT providers,
authentication off, internal controllers on).

## Seeding

`add-langs --project p100k --count 3` adds empty languages to a seeded project (they get no translations, so
jobs over them have to create the rows); select them with `--lang-offset`.

`seed` creates the project and its languages through the API (real permissions, base language), then inserts
namespaces (20), tags (50, 0 to 3 per key), key metas, keys and translations (base language complete, other
languages filled to `--fill`) with SQL. Texts come from a 100k entry pool of realistic UI strings (2 to 31
words, some ICU placeholders and HTML). Non-unique indexes of `translation`, `key` and `key_meta` are dropped
for the load and rebuilt afterwards, so the measured schema equals production. Seeded projects are recorded in
`$BENCH_HOME/runs/projects.json`. Rough timings on an M-series laptop: 100k x 30 languages ~2 min,
1M x 50 languages (fill 0.6, ~30M translations) ~40 min.

## Scenarios

`bench.py run --scenarios a,b,c` (or `all`). `--keys N` / `--langs N` limit the target size.

| Scenario | What it does |
|---|---|
| `set_state` | SET_TRANSLATIONS_STATE to REVIEWED, all keys x target languages |
| `tag`, `untag` | TAG_KEYS / UNTAG_KEYS with one tag |
| `set_namespace` | SET_KEYS_NAMESPACE (namespaces restored by SQL afterwards) |
| `clear`, `copy` | CLEAR_TRANSLATIONS / COPY_TRANSLATIONS for one language (texts restored afterwards) |
| `machine_translate` | MACHINE_TRANSLATE with the fake provider |
| `pre_translate_tm` | PRE_TRANSLATE_BT_TM |
| `qa_check` | QA_CHECK (EE, enabled through `/internal/features/toggle`) |
| `delete_restore` | DELETE_KEYS then RESTORE_KEYS |
| `noop_launcher` | NO_OP job (chunk size 1) through `/internal/batch-jobs/start-no-op`: pure launcher/queue overhead |
| `auto_translate` | base text change of N keys via `keys/import-resolvable`, measures the AUTO_TRANSLATE job it spawns |
| `mixed` | MT + QA + set state + tag concurrently on one project with the API probe on |
| `mt_create_cancel` | starts a MACHINE_TRANSLATE job over all keys x `--langs` and cancels it after the first chunk: job start cost (request, chunk rows, target size) and cancel cost |
| `qa_project_recheck` | marks all translations QA-stale and switches project QA off/on (one hidden QA job per 32k items); measures the request and 120 s of throughput, then cancels |
| `idle_probe` | 30 s of the API probe without jobs (reference latencies) |

New scenarios are functions decorated with `@scenario("name")` in `scenarios.py`; `run_jobs()` handles start,
polling, follow-up jobs and measurement. Ordering scenarios (registration, gate microbenchmarks, pipelines,
two nodes, chaos, fence) plug in the same way; Micrometer metrics with the prefix `tolgee_batch_ordering` are
copied into each result automatically.

## What is measured (per scenario, `runs/<label>/<scenario>-<project>/`)

- `result.json`: items, chunks, wall time, items/s, chunks/s, HTTP time of the start request, time to the first
  chunk, per-job facts (target jsonb size, executions, failed executions, activity entities), follow-up jobs
  (e.g. QA after a text change), DB statement count and time, queries and DB ms per chunk, row counters
  (`pg_stat_database`), Redis command stats, chunk execution and queue wait histograms (p50/p95/p99 from the
  Micrometer timers), GC pauses, max heap, max active / lock-waiting DB sessions, API probe latencies.
- `pg_top_statements.csv`: top 60 statements by total time (`pg_stat_statements`, reset per scenario).
- `samples.csv`: 1 s samples of heap and DB sessions.
- Profiles with `--profile`: `jfr` (JDK Flight Recorder, `nodeN.jfr`), or async-profiler `cpu|wall|alloc|lock`
  (`nodeN-<mode>.jfr`, `.html` flame graph, `.collapsed`). Summarize with
  `profile_summary.py nodeN-wall.collapsed --stack-filter DefaultDispatcher`.
- `--probe` runs an API user simulation (translation list + translation save every 0.5 s) during the scenario.

## Multi-node

`bench.sh start 2` starts two nodes sharing Postgres and Redis (`tolgee.cache.use-redis`,
`tolgee.websocket.use-redis`). `bench.sh run` passes all node URLs and PIDs to the runner, so metrics and
profiles are collected per node.

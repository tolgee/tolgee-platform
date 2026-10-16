"""Measurement around one scenario: DB statement stats, Redis command stats, Micrometer histograms, JVM
profiling (JFR / async-profiler), a 1 s sampler (job progress, heap, DB sessions) and an API latency probe."""

import json
import os
import random
import re
import shutil
import subprocess
import threading
import time

from common import Ctx, log, percentile

JCMD = os.environ.get("BENCH_JCMD", "/opt/homebrew/opt/openjdk@25/bin/jcmd")
ASPROF = os.environ.get("BENCH_ASPROF", shutil.which("asprof") or "/opt/homebrew/bin/asprof")


def histogram_quantiles(before: dict, after: dict, name: str, label_filter=None):
    """Computes count, sum and p50/p95/p99 (bucket upper bounds) of a Micrometer timer between two scrapes."""
    buckets = {}
    count = total = 0.0
    for key, value in after.items():
        if not key.startswith(name):
            continue
        labels = key[len(name):]
        if label_filter and not all(f'{k}="{v}"' in labels for k, v in label_filter.items()):
            continue
        delta = value - before.get(key, 0.0)
        if key.startswith(name + "_bucket"):
            le = re.search(r'le="([^"]+)"', labels).group(1)
            le = float("inf") if le == "+Inf" else float(le)
            buckets[le] = buckets.get(le, 0.0) + delta
        elif key.startswith(name + "_count"):
            count += delta
        elif key.startswith(name + "_sum"):
            total += delta
    result = {"count": count, "sum_s": total, "avg_ms": (total / count * 1000) if count else None}
    if count and buckets:
        ordered = sorted(buckets.items())
        for q in (0.5, 0.95, 0.99, 1.0):
            target = q * count
            for le, cumulative in ordered:
                if cumulative >= target:
                    result[f"p{int(q * 100)}_ms"] = le * 1000 if le != float("inf") else None
                    break
    return result


def sum_metric(scrape: dict, name: str, label_filter=None) -> float:
    total = 0.0
    for key, value in scrape.items():
        if key == name or key.startswith(name + "{"):
            if label_filter and not all(f'{k}="{v}"' in key for k, v in label_filter.items()):
                continue
            total += value
    return total


JFRCONV = os.environ.get("BENCH_JFRCONV", os.path.join(os.path.dirname(os.path.realpath(ASPROF)), "jfrconv"))


class Profiler:
    """mode jfr: JDK Flight Recorder (profile settings). cpu / wall / alloc / lock: async-profiler, recorded as
    JFR and converted to an HTML flame graph plus collapsed stacks (input of profile_summary.py)."""

    def __init__(self, mode: str, pids: list, out_dir: str, name: str):
        self.mode = mode
        self.pids = pids
        self.out_dir = out_dir
        self.name = re.sub(r"[^a-zA-Z0-9_]", "_", name)

    def _base(self, i):
        return os.path.join(self.out_dir, f"node{i + 1}-{self.mode}")

    def start(self):
        for i, pid in enumerate(self.pids):
            if self.mode == "jfr":
                subprocess.run([JCMD, str(pid), "JFR.start", f"name={self.name}", "settings=profile"],
                               capture_output=True)
            elif self.mode in ("cpu", "wall", "alloc", "lock"):
                extra = ["-t", "-i", "5ms"] if self.mode == "wall" else []
                res = subprocess.run([ASPROF, "start", "-e", self.mode, *extra, "-f", self._base(i) + ".jfr",
                                      str(pid)], capture_output=True, text=True)
                if res.returncode != 0:
                    log(f"asprof start failed: {res.stdout} {res.stderr}")

    def stop(self):
        for i, pid in enumerate(self.pids):
            if self.mode == "jfr":
                path = os.path.join(self.out_dir, f"node{i + 1}.jfr")
                subprocess.run([JCMD, str(pid), "JFR.stop", f"name={self.name}", f"filename={path}"],
                               capture_output=True)
            elif self.mode in ("cpu", "wall", "alloc", "lock"):
                base = self._base(i)
                res = subprocess.run([ASPROF, "stop", str(pid)], capture_output=True, text=True)
                if res.returncode != 0:
                    log(f"asprof stop failed: {res.stdout} {res.stderr}")
                kind = f"--{self.mode}"
                threads = ["-t"] if self.mode == "wall" else []
                subprocess.run([JFRCONV, kind, *threads, base + ".jfr", base + ".html"], capture_output=True)
                subprocess.run([JFRCONV, kind, *threads, "-o", "collapsed", base + ".jfr", base + ".collapsed"],
                               capture_output=True)


class ApiProbe(threading.Thread):
    """Simulates a user working in the project while jobs run: lists translations and saves a translation."""

    def __init__(self, ctx: Ctx, project: dict, interval_s: float = 0.5):
        super().__init__(daemon=True)
        self.ctx = ctx
        self.project = project
        self.interval_s = interval_s
        self.stop_event = threading.Event()
        self.samples = {"list": [], "save": [], "errors": 0}

    def run(self):
        pid = self.project["id"]
        lo, hi = self.project["keyIdRange"]
        tags = self.ctx.sql_rows(f"select tag from language where project_id = {pid} order by id limit 3")
        lang_query = "&".join(f"languages={t[0]}" for t in tags)
        while not self.stop_event.is_set():
            try:
                _, ms = self.ctx.timed_api("GET", f"/v2/projects/{pid}/translations?size=40&{lang_query}")
                self.samples["list"].append(ms)
                key_id = random.randint(lo, hi)
                lang = tags[1][0]
                _, ms = self.ctx.timed_api(
                    "PUT", f"/v2/projects/{pid}/translations",
                    {"key": self._key_name(key_id), "namespace": self._namespace(key_id),
                     "translations": {lang: f"probe edit {time.time()}"}},
                )
                self.samples["save"].append(ms)
            except Exception as e:  # noqa: BLE001
                self.samples["errors"] += 1
                if self.samples["errors"] < 5:
                    log(f"api probe error: {e}")
            self.stop_event.wait(self.interval_s)

    def _key_name(self, key_id):
        i = key_id - self.project["keyIdRange"][0]
        return f"module{i % 500}.screen{i % 37}.item_{i}"

    def _namespace(self, key_id):
        i = key_id - self.project["keyIdRange"][0]
        return None if i % 10 < 3 else f"ns-{i % 20}"

    def summary(self):
        out = {"errors": self.samples["errors"]}
        for k in ("list", "save"):
            v = self.samples[k]
            out[k] = {"n": len(v), "p50_ms": percentile(v, 50), "p95_ms": percentile(v, 95),
                      "p99_ms": percentile(v, 99), "max_ms": max(v) if v else None}
        return out


class Sampler(threading.Thread):
    def __init__(self, ctx: Ctx, out_path: str, interval_s: float = 1.0):
        super().__init__(daemon=True)
        self.ctx = ctx
        self.out_path = out_path
        self.interval_s = interval_s
        self.stop_event = threading.Event()
        self.max_heap = {}
        self.max_db_active = 0
        self.max_db_waiting = 0

    def run(self):
        started = time.time()
        with open(self.out_path, "w") as f:
            f.write("t_s,node,heap_used_mb,db_active,db_idle_in_tx,db_lock_waiting\n")
            while not self.stop_event.is_set():
                t = time.time() - started
                try:
                    db = self.ctx.sql_rows(
                        "select count(*) filter (where state = 'active'), "
                        "count(*) filter (where state like 'idle in transaction%'), "
                        "count(*) filter (where wait_event_type = 'Lock') "
                        "from pg_stat_activity where datname = 'tolgee' and pid <> pg_backend_pid()"
                    )[0]
                    self.max_db_active = max(self.max_db_active, int(db[0]))
                    self.max_db_waiting = max(self.max_db_waiting, int(db[2]))
                    for i, url in enumerate(self.ctx.app_urls):
                        scrape = self.ctx.prometheus(url)
                        heap = sum_metric(scrape, "jvm_memory_used_bytes", {"area": "heap"}) / 1e6
                        self.max_heap[i] = max(self.max_heap.get(i, 0), heap)
                        f.write(f"{t:.1f},{i + 1},{heap:.0f},{db[0]},{db[1]},{db[2]}\n")
                    f.flush()
                except Exception as e:  # noqa: BLE001
                    f.write(f"# sampler error {e}\n")
                self.stop_event.wait(self.interval_s)


class Measurement:
    """Wraps one scenario run."""

    def __init__(self, ctx: Ctx, name: str, profile: str, pids: list, project: dict = None, probe: bool = False):
        self.ctx = ctx
        self.name = name
        self.dir = os.path.join(ctx.out_dir, name)
        os.makedirs(self.dir, exist_ok=True)
        self.profiler = Profiler(profile, pids, self.dir, name)
        self.project = project
        self.probe = ApiProbe(ctx, project) if (probe and project) else None
        self.sampler = None
        self.result = {"scenario": name}

    def __enter__(self):
        ctx = self.ctx
        ctx.sql("create extension if not exists pg_stat_statements; select pg_stat_statements_reset(); select pg_stat_reset();")
        self.db_before = self._db_counters()
        if ctx.redis_available():
            ctx.redis("config", "resetstat")
        self.prom_before = [ctx.prometheus(u) for u in ctx.app_urls]
        self.sampler = Sampler(ctx, os.path.join(self.dir, "samples.csv"))
        self.sampler.start()
        if self.probe:
            self.probe.start()
        self.profiler.start()
        self.t0 = time.time()
        return self

    def _db_counters(self):
        row = self.ctx.sql_rows(
            "select sum(xact_commit), sum(xact_rollback), sum(tup_inserted), sum(tup_updated), sum(tup_deleted), "
            "sum(tup_fetched), sum(blks_hit), sum(blks_read), sum(temp_bytes) from pg_stat_database"
        )[0]
        keys = ["xact_commit", "xact_rollback", "tup_inserted", "tup_updated", "tup_deleted", "tup_fetched",
                "blks_hit", "blks_read", "temp_bytes"]
        return {k: int(float(v or 0)) for k, v in zip(keys, row)}

    def __exit__(self, exc_type, exc, tb):
        self.result["measured_wall_s"] = round(time.time() - self.t0, 2)
        self.profiler.stop()
        if self.probe:
            self.probe.stop_event.set()
            self.probe.join(5)
            self.result["api_probe"] = self.probe.summary()
        self.sampler.stop_event.set()
        self.sampler.join(5)
        self.result["max_heap_mb"] = {f"node{k + 1}": round(v) for k, v in self.sampler.max_heap.items()}
        self.result["max_db_active_sessions"] = self.sampler.max_db_active
        self.result["max_db_lock_waiting"] = self.sampler.max_db_waiting
        self._collect_db()
        self._collect_redis()
        self._collect_prometheus()
        if exc is not None:
            self.result["error"] = repr(exc)
        with open(os.path.join(self.dir, "result.json"), "w") as f:
            json.dump(self.result, f, indent=2, default=str)
        log(f"[{self.name}] {json.dumps(self.summary_line(), default=str)}")
        return False

    def summary_line(self):
        r = self.result
        keys = ["items", "chunks", "wall_s", "items_per_s", "http_start_ms", "first_progress_s", "status",
                "db_exec_ms", "db_calls", "queries_per_chunk", "db_ms_per_chunk", "redis_cmds"]
        return {k: r.get(k) for k in keys if k in r}

    def _collect_db(self):
        ctx = self.ctx
        after = self._db_counters()
        self.result["db_counters"] = {k: after[k] - self.db_before[k] for k in after}
        ctx.sql_to_csv(
            "select round(total_exec_time::numeric, 1) total_ms, calls, round(mean_exec_time::numeric, 3) mean_ms, "
            "rows, shared_blks_hit, shared_blks_read, shared_blks_dirtied, temp_blks_written, "
            "regexp_replace(query, '\\s+', ' ', 'g') query from pg_stat_statements "
            "where dbid = (select oid from pg_database where datname = 'tolgee') "
            "order by total_exec_time desc limit 60",
            os.path.join(self.dir, "pg_top_statements.csv"),
        )
        row = ctx.sql_rows(
            "select coalesce(sum(total_exec_time), 0), coalesce(sum(calls), 0) from pg_stat_statements "
            "where dbid = (select oid from pg_database where datname = 'tolgee') "
            "and query not ilike '%pg_stat%' and query not ilike '%pg_database%'"
        )[0]
        self.result["db_exec_ms"] = round(float(row[0]))
        self.result["db_calls"] = int(float(row[1]))
        chunks = self.result.get("chunks")
        if chunks:
            self.result["queries_per_chunk"] = round(self.result["db_calls"] / chunks, 1)
            self.result["db_ms_per_chunk"] = round(self.result["db_exec_ms"] / chunks, 2)

    def _collect_redis(self):
        if not self.ctx.redis_available():
            return
        info = self.ctx.redis("info", "commandstats")
        cmds = {}
        for line in info.splitlines():
            m = re.match(r"cmdstat_(\w+):calls=(\d+),usec=(\d+)", line)
            if m:
                cmds[m.group(1)] = {"calls": int(m.group(2)), "usec": int(m.group(3))}
        self.result["redis_cmds"] = sum(c["calls"] for c in cmds.values())
        self.result["redis_commandstats"] = dict(sorted(cmds.items(), key=lambda kv: -kv[1]["calls"])[:15])

    def _collect_prometheus(self):
        out = {}
        for i, url in enumerate(self.ctx.app_urls):
            before = self.prom_before[i]
            after = self.ctx.prometheus(url)
            node = {
                "chunk_execution": histogram_quantiles(before, after, "tolgee_batch_chunk_execution_seconds"),
                "queue_wait": histogram_quantiles(before, after, "tolgee_batch_job_queue_wait_seconds"),
                "gc_pause_s": round(sum_metric(after, "jvm_gc_pause_seconds_sum")
                                    - sum_metric(before, "jvm_gc_pause_seconds_sum"), 3),
                "gc_pauses": sum_metric(after, "jvm_gc_pause_seconds_count")
                - sum_metric(before, "jvm_gc_pause_seconds_count"),
                "cpu_process_avg": sum_metric(after, "process_cpu_usage"),
            }
            for name in ("tolgee_batch_ordering",):
                extra = {k: v for k, v in after.items() if k.startswith(name)}
                if extra:
                    node[name] = extra
            out[f"node{i + 1}"] = node
        self.result["prometheus"] = out

import json
import os
import re
import subprocess
import sys
import time
import urllib.error
import urllib.request

PSQL = os.environ.get("BENCH_PSQL", "/opt/homebrew/opt/postgresql@17/bin/psql")
REDIS_CLI = os.environ.get("BENCH_REDIS_CLI", "/opt/homebrew/opt/redis/bin/redis-cli")


def log(msg: str):
    print(time.strftime("%H:%M:%S"), msg, flush=True)


class ApiError(Exception):
    pass


class Ctx:
    def __init__(self, out_dir: str, pg_port: int, redis_port: int, app_urls: list):
        self.out_dir = out_dir
        self.pg_port = pg_port
        self.redis_port = redis_port
        self.app_urls = app_urls
        os.makedirs(out_dir, exist_ok=True)

    @property
    def app_url(self):
        return self.app_urls[0]

    # ---- HTTP -------------------------------------------------------------------------------------------
    def api(self, method: str, path: str, body=None, url: str = None, timeout: float = 600, raw: bool = False):
        data = None if body is None else json.dumps(body).encode()
        req = urllib.request.Request((url or self.app_url) + path, data=data, method=method)
        req.add_header("Content-Type", "application/json")
        req.add_header("Accept", "text/plain;version=0.0.4" if raw else "application/json")
        try:
            with urllib.request.urlopen(req, timeout=timeout) as resp:
                payload = resp.read()
        except urllib.error.HTTPError as e:
            raise ApiError(f"{method} {path} -> {e.code}: {e.read()[:500]!r}") from None
        if raw:
            return payload
        return json.loads(payload) if payload else None

    def timed_api(self, method: str, path: str, body=None, url: str = None):
        started = time.perf_counter()
        result = self.api(method, path, body, url=url)
        return result, (time.perf_counter() - started) * 1000

    def prometheus(self, url: str = None) -> dict:
        text = self.api("GET", "/actuator/prometheus", url=url, raw=True).decode()
        metrics = {}
        for line in text.splitlines():
            if not line or line.startswith("#"):
                continue
            m = re.match(r"^([a-zA-Z_:][a-zA-Z0-9_:]*)(\{[^}]*\})?\s+(\S+)", line)
            if m:
                try:
                    metrics[m.group(1) + (m.group(2) or "")] = float(m.group(3))
                except ValueError:
                    pass
        return metrics

    # ---- SQL --------------------------------------------------------------------------------------------
    def _psql(self, query: str, extra_args=()):
        cmd = [PSQL, "-p", str(self.pg_port), "-U", "postgres", "-h", "localhost", "-d", "tolgee",
               "-v", "ON_ERROR_STOP=1", "-X", "-q", *extra_args]
        res = subprocess.run(cmd, input=query, capture_output=True, text=True)
        if res.returncode != 0:
            raise RuntimeError(f"psql failed: {res.stderr[:2000]}\nquery: {query[:500]}")
        return res.stdout

    def sql(self, query: str):
        self._psql(query)

    def sql_rows(self, query: str) -> list:
        out = self._psql(query, ("-At", "-F", "\t"))
        return [line.split("\t") for line in out.splitlines() if line]

    def sql_value(self, query: str) -> str:
        rows = self.sql_rows(query)
        return rows[0][0] if rows else None

    def sql_to_csv(self, query: str, path: str):
        out = self._psql(f"copy ({query}) to stdout with csv header")
        with open(path, "w") as f:
            f.write(out)

    # ---- Redis ------------------------------------------------------------------------------------------
    def redis(self, *args) -> str:
        res = subprocess.run([REDIS_CLI, "-p", str(self.redis_port), *args], capture_output=True, text=True)
        return res.stdout

    def redis_available(self) -> bool:
        try:
            return self.redis("ping").strip() == "PONG"
        except FileNotFoundError:
            return False

    # ---- state ------------------------------------------------------------------------------------------
    @property
    def _projects_file(self):
        return os.path.join(os.path.dirname(self.out_dir.rstrip("/")), "projects.json")

    def projects(self) -> dict:
        if not os.path.exists(self._projects_file):
            return {}
        with open(self._projects_file) as f:
            return json.load(f)

    def save_project(self, name: str, data: dict):
        projects = self.projects()
        projects[name] = data
        with open(self._projects_file, "w") as f:
            json.dump(projects, f, indent=2)

    def project(self, name: str) -> dict:
        p = self.projects().get(name)
        if not p:
            sys.exit(f"Unknown project {name}, seed it first")
        return p


def percentile(values, p):
    if not values:
        return None
    s = sorted(values)
    k = max(0, min(len(s) - 1, int(round(p / 100.0 * (len(s) - 1)))))
    return s[k]

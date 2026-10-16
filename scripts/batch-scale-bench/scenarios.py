"""Scenario registry. A scenario starts one or more batch jobs (or drives the API) inside a Measurement and
records job-level numbers. New scenarios (e.g. the ordering S1-S6) are added with @scenario."""

import json
import os
import time

from common import ApiError, Ctx, log
from measure import Measurement
from seed import TEXT_POOL_SIZE

SCENARIOS = {}


def scenario(name, mutates_text=False):
    def wrap(fn):
        SCENARIOS[name] = {"fn": fn, "mutates_text": mutates_text}
        return fn

    return wrap


class Run:
    """Arguments shared by all scenarios."""

    def __init__(self, ctx: Ctx, project_name: str, project: dict, key_limit: int, lang_limit: int,
                 profile: str, pids: list, probe: bool, timeout_s: int, lang_offset: int = 0):
        self.ctx = ctx
        self.project_name = project_name
        self.project = project
        self.key_limit = key_limit
        self.lang_limit = lang_limit
        self.profile = profile
        self.pids = pids
        self.probe = probe
        self.timeout_s = timeout_s
        self.lang_offset = lang_offset

    @property
    def pid(self):
        return self.project["id"]

    def key_ids(self, limit=None):
        limit = limit or self.key_limit
        rows = self.ctx.sql_rows(
            f"select id from key where project_id = {self.pid} and deleted_at is null order by id"
            + (f" limit {limit}" if limit else "")
        )
        return [int(r[0]) for r in rows]

    @property
    def base_language_id(self):
        return self.project["languages"][0]

    def target_language_ids(self, limit=None):
        limit = limit or self.lang_limit
        langs = self.project["languages"][1 + self.lang_offset:]
        return langs[:limit] if limit else langs

    def measurement(self, name):
        full = f"{name}-{self.project_name}"
        return Measurement(self.ctx, full, self.profile, self.pids, self.project, self.probe)


# ---- job helpers ------------------------------------------------------------------------------------------

def start_job(run: Run, path: str, body: dict, internal=False):
    url_path = path if internal else f"/v2/projects/{run.pid}/start-batch-job/{path}"
    job, ms = run.ctx.timed_api("POST", url_path, body)
    return job, ms


def wait_for_jobs(run: Run, jobs: list, m: Measurement, poll_s=0.25):
    """Polls until all jobs are terminal. Records per job wall time and time to first progress."""
    ctx = run.ctx
    started = time.time()
    state = {j["id"]: {"first_progress_s": None, "done_s": None, "status": j.get("status")} for j in jobs}
    last_log = 0
    while True:
        ids = ",".join(str(j["id"]) for j in jobs)
        rows = ctx.sql_rows(f"select id, status from tolgee_batch_job where id in ({ids})")
        now = time.time() - started
        for job_id, status in rows:
            s = state[int(job_id)]
            s["status"] = status
            if s["first_progress_s"] is None and status != "PENDING":
                s["first_progress_s"] = round(now, 2)
            if s["done_s"] is None and status in ("SUCCESS", "FAILED", "CANCELLED"):
                s["done_s"] = round(now, 2)
        if all(s["done_s"] is not None for s in state.values()):
            break
        if now > run.timeout_s:
            log(f"timeout after {now:.0f}s, cancelling jobs")
            for j in jobs:
                try:
                    ctx.api("PUT", f"/v2/projects/{run.pid}/batch-jobs/{j['id']}/cancel")
                except Exception as e:  # noqa: BLE001
                    log(f"cancel failed: {e}")
            m.result["timed_out"] = True
            break
        if now - last_log > 30:
            last_log = now
            prog = ctx.sql_rows(
                "select batch_job_id, count(*) filter (where status = 'SUCCESS'), count(*) "
                f"from tolgee_batch_job_chunk_execution where batch_job_id in ({ids}) group by 1"
            )
            log(f"  {now:.0f}s: " + ", ".join(f"job {r[0]}: {r[1]}/{r[2]} chunks" for r in prog))
        time.sleep(poll_s)
    return state


def job_facts(ctx: Ctx, job_id: int) -> dict:
    row = ctx.sql_rows(
        "select type, status, total_items, total_chunks, chunk_size, pg_column_size(target), "
        "extract(epoch from (updated_at - created_at)) "
        f"from tolgee_batch_job where id = {job_id}"
    )[0]
    execs = ctx.sql_rows(
        "select count(*), count(*) filter (where status = 'FAILED'), "
        "extract(epoch from (max(updated_at) - min(created_at))) "
        f"from tolgee_batch_job_chunk_execution where batch_job_id = {job_id}"
    )[0]
    activity = ctx.sql_rows(
        "select ar.id, (select count(*) from activity_modified_entity me where me.activity_revision_id = ar.id), "
        "(select count(*) from activity_describing_entity de where de.activity_revision_id = ar.id) "
        f"from activity_revision ar where ar.batch_job_id = {job_id}"
    )
    return {
        "type": row[0], "status": row[1], "items": int(row[2]), "chunks": int(row[3]), "chunk_size": int(row[4]),
        "target_jsonb_bytes": int(row[5] or 0), "job_db_span_s": round(float(row[6] or 0), 2),
        "executions": int(execs[0]), "failed_executions": int(execs[1]),
        "executions_span_s": round(float(execs[2] or 0), 2),
        "activity_modified_entities": int(activity[0][1]) if activity else 0,
        "activity_describing_entities": int(activity[0][2]) if activity else 0,
    }


def max_job_id(ctx: Ctx) -> int:
    return int(ctx.sql_value("select coalesce(max(id), 0) from tolgee_batch_job"))


def record_followups(run: Run, result: dict, before_id: int, primary_ids: set):
    """Jobs started as a consequence of the measured jobs (e.g. QA after a text change) run outside the
    measurement; they are listed and waited for so the next scenario starts on an idle project."""
    started = time.time()
    wait_project_idle(run)
    rows = run.ctx.sql_rows(f"select id from tolgee_batch_job where id > {before_id}")
    followups = [int(r[0]) for r in rows if int(r[0]) not in primary_ids]
    if followups:
        result["followups"] = [job_facts(run.ctx, j) for j in followups]
        result["followups_wait_s"] = round(time.time() - started, 2)


def wait_project_idle(run: Run, timeout_s=1800):
    started = time.time()
    while time.time() - started < timeout_s:
        n = int(run.ctx.sql_value(
            f"select count(*) from tolgee_batch_job where project_id = {run.pid} "
            "and status in ('PENDING', 'RUNNING')"))
        if n == 0:
            return
        time.sleep(0.5)
    log("project did not become idle")


def run_jobs(run: Run, name: str, starts: list, before_start=None):
    """starts: list of (path, body, internal). Measures everything until all jobs finish."""
    wait_project_idle(run)
    with run.measurement(name) as m:
        if before_start:
            before_start(m)
        before_id = max_job_id(run.ctx)
        jobs = []
        http_ms = []
        start_errors = []
        for path, body, internal in starts:
            try:
                job, ms = start_job(run, path, body, internal)
            except ApiError as e:
                if len(starts) == 1:
                    raise
                start_errors.append(f"{path}: {e}")
                log(f"  start of {path} failed: {e}")
                continue
            jobs.append(job)
            http_ms.append(round(ms))
        if start_errors:
            m.result["start_errors"] = start_errors
        state = wait_for_jobs(run, jobs, m)
        facts = [job_facts(run.ctx, j["id"]) for j in jobs]
        items = sum(f["items"] for f in facts)
        chunks = sum(f["chunks"] for f in facts)
        wall = max(s["done_s"] or 0 for s in state.values()) or m.result.get("measured_wall_s", 0)
        m.result.update({
            "jobs": [{**f, **state[j["id"]], "http_start_ms": http_ms[i]} for i, (j, f) in enumerate(zip(jobs, facts))],
            "items": items, "chunks": chunks, "wall_s": wall,
            "items_per_s": round(items / wall, 1) if wall else None,
            "chunks_per_s": round(chunks / wall, 2) if wall else None,
            "http_start_ms": http_ms[0] if len(http_ms) == 1 else http_ms,
            "first_progress_s": state[jobs[0]["id"]]["first_progress_s"],
            "status": ",".join(sorted({s["status"] for s in state.values()})),
        })
    record_followups(run, m.result, before_id, {j["id"] for j in jobs})
    return m.result


# ---- restore helpers --------------------------------------------------------------------------------------

def restore_texts(run: Run, language_ids: list):
    """Puts seeded text back for the given languages (rows that exist)."""
    p = run.project
    wait_project_idle(run)
    started = time.time()
    lang_order = {lid: i for i, lid in enumerate(p["languages"])}
    key_base = p["keyIdRange"][0]
    for lid in language_ids:
        o = lang_order[lid]
        run.ctx.sql(
            f"""
            update translation t set text = bp.text, word_count = bp.words, character_count = bp.chars, state = case when (t.key_id - {key_base} + {o}) % 10 = 0
              then 2 else 1 end, auto = false, mt_provider = null, outdated = false, qa_checks_stale = false
            from bench_text_pool bp
            where t.language_id = {lid} and t.key_id between {p['keyIdRange'][0]} and {p['keyIdRange'][1]}
              and bp.id = ((t.key_id - {key_base}) * 13 + {o} * 101) % {TEXT_POOL_SIZE}
              and (t.text is distinct from bp.text or t.word_count is distinct from bp.words)
            """
        )
    log(f"  restored texts of {len(language_ids)} languages in {time.time() - started:.0f}s")


def restore_namespaces(run: Run):
    p = run.project
    wait_project_idle(run)
    kb = p["keyIdRange"][0]
    nb = p["namespaceIds"][0]
    run.ctx.sql(
        f"""
        insert into namespace(id, created_at, updated_at, name, project_id)
        select {nb} + i, now(), now(), 'ns-' || i, {p['id']} from generate_series(0, 19) i
        on conflict do nothing;
        update key set namespace_id = case when (id - {kb}) % 10 < 3 then null else {nb} + ((id - {kb}) % 20) end
        where project_id = {p['id']}
        """
    )


# ---- scenarios --------------------------------------------------------------------------------------------

@scenario("set_state")
def sc_set_state(run: Run):
    langs = run.target_language_ids()
    return run_jobs(run, "set_state", [
        ("set-translation-state", {"keyIds": run.key_ids(), "languageIds": langs, "state": "REVIEWED"}, False)])


@scenario("tag")
def sc_tag(run: Run):
    return run_jobs(run, "tag", [("tag-keys", {"keyIds": run.key_ids(), "tags": ["bench-tag"]}, False)])


@scenario("untag")
def sc_untag(run: Run):
    return run_jobs(run, "untag", [("untag-keys", {"keyIds": run.key_ids(), "tags": ["bench-tag"]}, False)])


@scenario("set_namespace")
def sc_set_namespace(run: Run):
    try:
        return run_jobs(run, "set_namespace",
                        [("set-keys-namespace", {"keyIds": run.key_ids(), "namespace": f"bench-ns-{int(time.time())}"}, False)])
    finally:
        restore_namespaces(run)


@scenario("clear", mutates_text=True)
def sc_clear(run: Run):
    langs = run.target_language_ids(1)
    try:
        return run_jobs(run, "clear", [("clear-translations", {"keyIds": run.key_ids(), "languageIds": langs}, False)])
    finally:
        restore_texts(run, langs)


@scenario("copy", mutates_text=True)
def sc_copy(run: Run):
    langs = run.target_language_ids(1)
    try:
        return run_jobs(run, "copy", [("copy-translations", {
            "keyIds": run.key_ids(), "sourceLanguageId": run.base_language_id, "targetLanguageIds": langs}, False)])
    finally:
        restore_texts(run, langs)


@scenario("machine_translate", mutates_text=True)
def sc_mt(run: Run):
    langs = run.target_language_ids()
    try:
        return run_jobs(run, "machine_translate", [("machine-translate", {
            "keyIds": run.key_ids(), "targetLanguageIds": langs}, False)])
    finally:
        restore_texts(run, langs)


@scenario("pre_translate_tm", mutates_text=True)
def sc_tm(run: Run):
    langs = run.target_language_ids()
    try:
        return run_jobs(run, "pre_translate_tm", [("pre-translate-by-tm", {
            "keyIds": run.key_ids(), "targetLanguageIds": langs}, False)])
    finally:
        restore_texts(run, langs)


@scenario("qa_check")
def sc_qa(run: Run):
    enable_qa(run)
    langs = run.target_language_ids()
    return run_jobs(run, "qa_check", [("qa-check", {"keyIds": run.key_ids(), "languageIds": langs}, False)])


@scenario("delete_restore")
def sc_delete_restore(run: Run):
    ids = run.key_ids()
    r1 = run_jobs(run, "delete_keys", [("delete-keys", {"keyIds": ids}, False)])
    r2 = run_jobs(run, "restore_keys", [("restore-keys", {"keyIds": ids}, False)])
    return {"delete": r1, "restore": r2}


@scenario("noop_launcher")
def sc_noop(run: Run):
    n = run.key_limit or 100_000
    return run_jobs(run, "noop_launcher", [("/internal/batch-jobs/start-no-op", {"itemIds": list(range(n))}, True)])


@scenario("mt_create_cancel")
def sc_mt_create_cancel(run: Run):
    """Only the start of a large MACHINE_TRANSLATE job: request time, stored target size, chunk rows, heap; the
    job is cancelled right after its first chunk ran."""
    wait_project_idle(run)
    ctx = run.ctx
    ids = run.key_ids()
    langs = run.target_language_ids()
    with run.measurement("mt_create_cancel") as m:
        job, ms = start_job(run, "machine-translate", {"keyIds": ids, "targetLanguageIds": langs})
        m.result["http_start_ms"] = round(ms)
        started = time.time()
        while ctx.sql_value(f"select status from tolgee_batch_job where id = {job['id']}") == "PENDING":
            if time.time() - started > 600:
                break
            time.sleep(0.1)
        m.result["first_progress_s"] = round(time.time() - started, 2)
        _, cancel_ms = ctx.timed_api("PUT", f"/v2/projects/{run.pid}/batch-jobs/{job['id']}/cancel")
        m.result["cancel_http_ms"] = round(cancel_ms)
        wait_project_idle(run)
        m.result["cancel_done_s"] = round(time.time() - started, 2)
        facts = job_facts(ctx, job["id"])
        m.result.update({"jobs": [facts], "items": facts["items"], "chunks": facts["chunks"],
                         "status": facts["status"]})
    return m.result


@scenario("auto_translate", mutates_text=True)
def sc_auto_translate(run: Run):
    """Changes the base text of N keys through import-resolvable; the import activity starts AUTO_TRANSLATE
    (and QA when enabled) for all target languages."""
    ctx = run.ctx
    ctx.api("PUT", f"/v2/projects/{run.pid}/auto-translation-settings", {
        "usingTranslationMemory": False, "usingMachineTranslation": True, "enableForImport": True})
    ids = run.key_ids()
    base = run.project["keyIdRange"][0]
    stamp = int(time.time())
    keys = []
    for kid in ids:
        i = kid - base
        keys.append({
            "name": f"module{i % 500}.screen{i % 37}.item_{i}",
            "namespace": None if i % 10 < 3 else f"ns-{i % 20}",
            "translations": {"en": {"text": f"Changed base text {i} at {stamp}", "resolution": "OVERRIDE"}},
        })
    with run.measurement("auto_translate") as m:
        before = int(ctx.sql_value("select coalesce(max(id), 0) from tolgee_batch_job"))
        _, import_ms = ctx.timed_api("POST", f"/v2/projects/{run.pid}/keys/import-resolvable", {"keys": keys})
        m.result["import_http_ms"] = round(import_ms)
        jobs = []
        deadline = time.time() + 60
        while not jobs and time.time() < deadline:
            rows = ctx.sql_rows(
                f"select id, status from tolgee_batch_job where project_id = {run.pid} and id > {before} "
                "and type = 'AUTO_TRANSLATE'"
            )
            jobs = [{"id": int(r[0]), "status": r[1]} for r in rows]
            time.sleep(0.2)
        state = wait_for_jobs(run, jobs, m)
        facts = [job_facts(ctx, j["id"]) for j in jobs]
        wall = max(s["done_s"] or 0 for s in state.values()) if state else None
        items = sum(f["items"] for f in facts)
        m.result.update({
            "jobs": facts, "items": items, "chunks": sum(f["chunks"] for f in facts), "wall_s": wall,
            "items_per_s": round(items / wall, 1) if wall else None,
            "status": ",".join(sorted({s["status"] for s in state.values()})),
        })
    restore_texts(run, [run.base_language_id] + run.target_language_ids(0))
    return m.result


@scenario("qa_project_recheck")
def sc_qa_project_recheck(run: Run):
    """Marks all translations of the first --keys keys (default 30k) QA-stale and switches project QA off and on,
    which rechecks every stale translation of the project (one hidden QA job per 32k items). Measures the switch request and 120 s of processing,
    then cancels the remaining jobs."""
    ctx = run.ctx
    enable_qa(run)
    wait_project_idle(run)
    key_limit = run.key_limit or 30_000
    lo = run.project["keyIdRange"][0]
    ctx.sql(
        f"update translation set qa_checks_stale = true where qa_checks_stale = false "
        f"and key_id between {lo} and {lo + key_limit - 1}"
    )
    ctx.api("PUT", f"/v2/projects/{run.pid}/qa-settings/enabled", {"enabled": False})
    with run.measurement("qa_project_recheck") as m:
        before_id = max_job_id(ctx)
        _, ms = ctx.timed_api("PUT", f"/v2/projects/{run.pid}/qa-settings/enabled", {"enabled": True})
        m.result["http_start_ms"] = round(ms)
        jobs = ctx.sql_rows(
            f"select count(*), coalesce(sum(total_items), 0), coalesce(sum(total_chunks), 0) "
            f"from tolgee_batch_job where id > {before_id}"
        )[0]
        m.result.update({"jobs_created": int(jobs[0]), "items": int(jobs[1]), "chunks": int(jobs[2])})
        window = 120
        time.sleep(window)
        done = ctx.sql_rows(
            "select count(*), coalesce(sum(jsonb_array_length(success_targets)), 0) "
            "from tolgee_batch_job_chunk_execution e join tolgee_batch_job j on j.id = e.batch_job_id "
            f"where j.id > {before_id} and e.status = 'SUCCESS'"
        )[0]
        m.result.update({"window_s": window, "chunks_done": int(done[0]), "items_done": int(done[1]),
                         "chunks_per_s": round(int(done[0]) / window, 1),
                         "items_per_s": round(int(done[1]) / window, 1)})
    started = time.time()
    for (job_id,) in ctx.sql_rows(
        f"select id from tolgee_batch_job where id > {before_id} and status in ('PENDING', 'RUNNING')"
    ):
        try:
            ctx.api("PUT", f"/v2/projects/{run.pid}/batch-jobs/{job_id}/cancel")
        except ApiError as e:
            log(f"cancel {job_id}: {e}")
    m.result["cancel_all_s"] = round(time.time() - started, 1)
    wait_project_idle(run)
    with open(os.path.join(m.dir, "result.json"), "w") as f:
        json.dump(m.result, f, indent=2, default=str)
    return m.result


@scenario("idle_probe")
def sc_idle_probe(run: Run):
    """API latency without any batch job running (reference for the probe numbers of other scenarios)."""
    wait_project_idle(run)
    run.probe = True
    with run.measurement("idle_probe") as m:
        time.sleep(30)
    return m.result


@scenario("mixed", mutates_text=True)
def sc_mixed(run: Run):
    """Several concurrent jobs on the same project while the API probe edits translations."""
    enable_qa(run)
    ids = run.key_ids()
    langs = run.target_language_ids()
    small = ids[: max(1, len(ids) // 10)]
    starts = [
        ("machine-translate", {"keyIds": small, "targetLanguageIds": langs[:3]}, False),
        ("qa-check", {"keyIds": small, "languageIds": langs[:5]}, False),
        ("set-translation-state", {"keyIds": ids, "languageIds": langs, "state": "REVIEWED"}, False),
        ("tag-keys", {"keyIds": ids, "tags": ["bench-mixed"]}, False),
    ]
    run.probe = True
    try:
        return run_jobs(run, "mixed", starts)
    finally:
        restore_texts(run, langs[:3])


def enable_qa(run: Run):
    ctx = run.ctx
    ctx.api("PUT", "/internal/features/toggle?feature=QA_CHECKS&enabled=true")
    try:
        ctx.api("PUT", f"/v2/projects/{run.pid}/qa-settings/enabled", {"enabled": True})
    except Exception as e:  # noqa: BLE001
        log(f"qa enable: {e}")

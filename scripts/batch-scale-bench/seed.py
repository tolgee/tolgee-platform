"""Fast data generator for the batch scale benchmark.

The project and its languages are created through the REST API (so permissions, base language and
organization wiring are real); keys, key metas, tags, namespaces and translations are bulk inserted with
generate_series straight into the real schema.
"""

import time
from concurrent.futures import ThreadPoolExecutor

from common import Ctx, log

LANGUAGE_TAGS = [
    "en", "de", "fr", "es", "it", "pt", "nl", "pl", "cs", "sk", "sv", "da", "fi", "nb", "ru", "uk", "ja", "ko",
    "zh-Hans", "zh-Hant", "ar", "he", "tr", "el", "hu", "ro", "bg", "hr", "sl", "sr", "lt", "lv", "et", "vi",
    "th", "id", "ms", "hi", "bn", "ta", "te", "fa", "ur", "sw", "ca", "eu", "gl", "is", "ga", "mt",
]

WORDS = (
    "the quick brown fox jumps over lazy dog settings account profile save cancel delete confirm "
    "project language translation key export import upload download select all none search filter "
    "please enter your email address password forgot remember me sign in out create new edit view "
    "share invite member owner admin billing plan upgrade subscription invoice payment method card "
    "notification message error warning success info loading retry back next previous finish close "
    "open file folder name description value status pending running failed done today yesterday "
    "week month year hour minute second ago items selected total count page of results found no "
    "{name} {count} {date} <b>bold</b> %s %d available unavailable welcome dashboard report chart "
    "table column row cell sort ascending descending apply reset clear change update version history "
    "comment reply mention label tag namespace screenshot context machine memory suggestion review"
).split()

TEXT_POOL_SIZE = 100_000

# Non-unique, non-PK indexes that make bulk loading slow. They are dropped before the load and rebuilt
# afterwards from their saved definitions, so the measured schema is identical to production.
HEAVY_INDEX_TABLES = ("translation", "key", "key_meta")


def ensure_text_pool(ctx: Ctx):
    exists = ctx.sql_value("select count(*) from pg_tables where tablename = 'bench_text_pool'")
    if exists == "1" and ctx.sql_value("select count(*) from bench_text_pool") == str(TEXT_POOL_SIZE):
        return
    words = ",".join("'" + w.replace("'", "''") + "'" for w in WORDS)
    ctx.sql(
        f"""
        drop table if exists bench_text_pool;
        create table bench_text_pool(id int primary key, text text not null, words int not null, chars int not null);
        insert into bench_text_pool
        select i, t, array_length(regexp_split_to_array(t, ' '), 1), length(t)
        from (
          select i, (
            select string_agg(w[1 + ((i * 7 + j * 13 + (i / 7) * j) % array_length(w, 1))], ' ')
            from generate_series(1, 2 + (i * 31 % 30)) j
          ) t
          from generate_series(0, {TEXT_POOL_SIZE - 1}) i, (select array[{words}] w) ws
        ) x;
        """
    )


def create_project(ctx: Ctx, name: str, lang_count: int) -> int:
    tags = LANGUAGE_TAGS[:lang_count]
    body = {
        "name": name,
        "organizationId": 1,
        "baseLanguageTag": "en",
        "languages": [{"name": t, "originalName": t, "tag": t} for t in tags],
        "icuPlaceholders": True,
    }
    project = ctx.api("POST", "/v2/projects", body)
    return project["id"]


def save_and_drop_heavy_indexes(ctx: Ctx) -> list:
    tables = ",".join(f"'{t}'" for t in HEAVY_INDEX_TABLES)
    rows = ctx.sql_rows(
        f"""
        select i.indexname, i.indexdef from pg_indexes i
        join pg_class c on c.relname = i.indexname
        join pg_index x on x.indexrelid = c.oid
        where i.tablename in ({tables}) and not x.indisunique and not x.indisprimary
        """
    )
    for name, _ in rows:
        ctx.sql(f'drop index if exists "{name}"')
    return [d for _, d in rows]


def recreate_indexes(ctx: Ctx, defs: list):
    """CREATE INDEX statements on the same table only take SHARE locks, so they are built in parallel."""

    def build(d):
        started = time.time()
        ctx.sql(f"set maintenance_work_mem = '1GB'; set max_parallel_maintenance_workers = 2; {d}")
        log(f"  index rebuilt in {time.time() - started:.0f}s: {d[:110]}")

    with ThreadPoolExecutor(max_workers=8) as pool:
        for f in [pool.submit(build, d) for d in defs]:
            f.result()


def reserve_ids(ctx: Ctx, count: int) -> int:
    """Reserves a contiguous id range in hibernate_sequence so the app never allocates the same ids."""
    start = int(ctx.sql_value("select nextval('hibernate_sequence')")) + 100_000
    ctx.sql(f"select setval('hibernate_sequence', {start + count + 100_000})")
    return start


def seed(ctx: Ctx, name: str, keys: int, langs: int, fill: float):
    t0 = time.time()
    ensure_text_pool(ctx)
    project_id = create_project(ctx, name, langs)
    lang_rows = ctx.sql_rows(
        f"""
        select l.id from language l join project p on p.id = l.project_id
        where l.project_id = {project_id} and l.deleted_at is null
        order by (l.id = p.base_language_id) desc, l.id
        """
    )
    lang_ids = [int(r[0]) for r in lang_rows]
    log(f"project {name} id={project_id}, {len(lang_ids)} languages (base {lang_ids[0]})")

    namespace_count, tag_count = 20, 50
    base = reserve_ids(ctx, namespace_count + tag_count + 2 * keys + keys * len(lang_ids) + 10)
    ns_base = base
    tag_base = ns_base + namespace_count
    key_base = tag_base + tag_count
    meta_base = key_base + keys
    tr_base = meta_base + keys

    ctx.sql(
        f"""
        insert into namespace(id, created_at, updated_at, name, project_id)
        select {ns_base} + i, now(), now(), 'ns-' || i, {project_id} from generate_series(0, {namespace_count - 1}) i;
        insert into tag(id, created_at, updated_at, name, project_id)
        select {tag_base} + i, now(), now(), 'tag-' || i, {project_id} from generate_series(0, {tag_count - 1}) i;
        """
    )

    index_defs = save_and_drop_heavy_indexes(ctx)
    log(f"dropped {len(index_defs)} heavy indexes for the load")
    try:
        ctx.sql(
            f"""
            insert into key(id, created_at, updated_at, name, project_id, namespace_id, is_plural)
            select {key_base} + i, now(), now(),
              'module' || (i % 500) || '.screen' || (i % 37) || '.item_' || i, {project_id},
              case when i % 10 < 3 then null else {ns_base} + (i % {namespace_count}) end, false
            from generate_series(0, {keys - 1}) i;

            insert into key_meta(id, created_at, updated_at, key_id, description)
            select {meta_base} + i, now(), now(), {key_base} + i,
              case when i % 4 = 0 then 'Shown on screen ' || (i % 37) || ' of module ' || (i % 500) end
            from generate_series(0, {keys - 1}) i;

            insert into key_meta_tags(key_metas_id, tags_id)
            select {meta_base} + i, {tag_base} + ((i * 7 + j * 11) % {tag_count})
            from generate_series(0, {keys - 1}) i, generate_series(1, 3) j
            where j <= i % 4;
            """
        )
        log(f"keys inserted ({time.time() - t0:.0f}s)")

        fill_pct = int(fill * 100)
        for ord_, lang_id in enumerate(lang_ids):
            started = time.time()
            where = "" if ord_ == 0 else f"where (i * 7 + {ord_} * 13) % 100 < {fill_pct}"
            ctx.sql(
                f"""
                insert into translation(id, created_at, updated_at, text, language_id, key_id, state, auto,
                  character_count, word_count, outdated, qa_checks_stale)
                select {tr_base} + {ord_}::bigint * {keys} + i, now(), now(),
                  p.text, {lang_id}, {key_base} + i,
                  case when (i + {ord_}) % 10 = 0 then 2 else 1 end, false, p.chars, p.words, false, false
                from generate_series(0, {keys - 1}) i
                join bench_text_pool p on p.id = (i * 13 + {ord_} * 101) % {TEXT_POOL_SIZE}
                {where};
                """
            )
            log(f"  language {ord_ + 1}/{len(lang_ids)} inserted in {time.time() - started:.1f}s")
    finally:
        recreate_indexes(ctx, index_defs)

    ctx.sql("vacuum analyze key; vacuum analyze key_meta; vacuum analyze key_meta_tags; vacuum analyze translation;")
    ctx.save_project(name, {"id": project_id, "keys": keys, "languages": lang_ids, "fill": fill,
                            "keyIdRange": [key_base, key_base + keys - 1],
                            "tagIds": [tag_base, tag_base + tag_count - 1],
                            "namespaceIds": [ns_base, ns_base + namespace_count - 1]})
    log(f"seeded {name} in {time.time() - t0:.0f}s")
    return project_id


def add_languages(ctx: Ctx, name: str, count: int):
    """Adds empty languages (no translations) to a seeded project, e.g. to measure jobs that create rows."""
    project = ctx.project(name)
    existing = int(ctx.sql_value(f"select count(*) from language where project_id = {project['id']}"))
    added = []
    for i in range(count):
        tag = f"x-bench-{existing + i}"
        lang = ctx.api("POST", f"/v2/projects/{project['id']}/languages",
                       {"name": tag, "originalName": tag, "tag": tag})
        added.append(lang["id"])
    project["languages"] = project["languages"] + added
    ctx.save_project(name, project)
    log(f"added languages {added} to {name}; they are target languages {len(project['languages']) - 1 - count}.."
        f"{len(project['languages']) - 2} (use --lang-offset)")

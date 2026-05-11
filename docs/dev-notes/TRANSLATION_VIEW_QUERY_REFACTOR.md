# Translation View Query Refactor

This document describes the refactor of the **Translation View query** — the query that
powers `GET /v2/projects/{projectId}/translations` and `GET /v2/projects/{projectId}/keys/trash`.

It is intended as a reference for anyone touching `backend/data/src/main/kotlin/io/tolgee/service/queryBuilders/translationViewBuilder/`
or trying to understand why the code looks the way it does today.

---

## 1. Overview

### 1.1 Why the refactor

The previous design built **one large SQL query** per page request. For each requested language
it added one `LEFT JOIN` on the `translation` table plus four correlated `count(...)` subqueries
(comments, unresolved comments, active suggestions, total suggestions). With `N` languages
selected this came out to `N` LEFT JOINs and `4N` correlated subqueries, plus a `GROUP BY` over
all of them. The cost grew linearly with language count and dominated the cold-cache page-load
time.

The most pathological case — `filterUntranslatedAny` across 20 languages on 10 000 keys — took
**~290 seconds** (almost five minutes) on a cold buffer cache, because PostgreSQL had to scan
20 outer-joined translation columns for every candidate row before evaluating the
`OR text IS NULL OR text=''` chain.

### 1.2 What changed at a glance

The query was restructured to:

1. **Drop the N translation joins** from the main query. The main query now returns only
   key-level columns.
2. **Express every translation-level filter as an `EXISTS` subquery** correlated to the parent
   key row. Each filter stands on its own — no shared join.
3. **Fetch the actual translation rows in a separate query** (`Translation t WHERE t.key_id IN
   (... page key ids ...) AND t.language_id IN (...)`), then attach them to the views in
   application code.
4. **Post-load comment, suggestion and QA issue counts** in batched queries instead of inlining
   `5N` correlated subqueries in the main `SELECT` (4 per language for comment/suggestion counts
   plus the QA issue count added by the QA Checks feature).
5. **Replace tag/task LEFT JOINs with `EXISTS`** so the main query no longer multiplies rows
   and the `GROUP BY` is gone entirely.
6. **Build sort/cursor expressions for `translations.{tag}.text`** as scalar correlated
   subqueries on demand, since per-language columns are no longer in the `SELECT` list.
7. **Use non-correlated set predicates for the "any-language" aggregate filters** —
   `filterUntranslatedAny`, `filterTranslatedAny`, and homogeneous `filterState=lang,STATE`
   across many languages — so PostgreSQL evaluates them once per query instead of once per
   outer key. See section 3.4.
8. **Use non-correlated `k.id IN (…)` subqueries for count queries** (vs correlated `EXISTS`
   for data queries). Data queries short-circuit under `LIMIT` and benefit from correlated
   `EXISTS`; count queries have no `LIMIT` and must evaluate every key, so a non-correlated
   semi-join (evaluated once, hash-joined) is dramatically cheaper. The same filter code emits
   different query shapes based on an `isCountQuery` flag. See section 3.5.
9. **Collapse multi-language per-language filters into a single `language_id IN (…)`
   subquery.** When the same per-language `EXISTS` predicate would apply to several languages
   (e.g. `filterOutdatedLanguage=en&filterOutdatedLanguage=de&…`), the N subqueries collapse
   into one keyed on `language IN (…)` — logically equivalent (OR of EXISTS with different
   `language_id` values = single EXISTS with `language_id IN`) and much cheaper. See
   section 3.6.

The result: the main query touches a constant number of tables regardless of language count,
the `GROUP BY` is gone, and one extra round trip to PostgreSQL replaces a quadratic blow-up of
joins and subqueries.

### 1.3 Performance results

End-to-end HTTP benchmark hitting `/v2/projects/{id}/translations` against a local instance
backed by a 39 319-key × 14-language project (`tmp/bench-translations.sh`). Each scenario does
1 warmup run and the median of 5 measured runs. Total time covers **count + data + post-load**
queries. All scenarios request all 14 languages and list-valued filters span all 14 langs.

| Scenario                                    | Main     | Refactored | Change              |
|---------------------------------------------|----------|------------|---------------------|
| page load (no filter)                       | 65 ms    | 45 ms      | **1.4× faster**     |
| **filterState=UNTRANSLATED × 14 (homogeneous)** | 494 ms | 144 ms | **3.4× faster**     |
| **filterState × 14 (heterogeneous mix)**    | 190 ms   | 185 ms     | ≈                   |
| filterUntranslatedAny                       | 488 ms   | 170 ms     | **2.9× faster**     |
| filterUntranslatedInLang (1 lang)           | 95 ms    | 40 ms      | **2.4× faster**     |
| filterTranslatedAny                         | 222 ms   | 53 ms      | **4.2× faster**     |
| **filterTag**                               | 278 ms   | 17 ms      | **16× faster**      |
| filterNoTag                                 | 45 ms    | 33 ms      | ≈                   |
| **filterOutdatedLanguage × 14**             | 406 ms   | 61 ms      | **6.7× faster**     |
| **filterHasSuggestionsInLang × 14**         | 3526 ms  | 15 ms      | **~235× faster** 🎉 |
| full-text search                            | 378 ms   | 193 ms     | **2× faster**       |

The biggest wins:

- `filterHasSuggestionsInLang × 14 langs` goes from 3.5 s to ~15 ms. On main each language
  added one LEFT JOIN + one OR'd `t.text IS NOT NULL` clause; the combination pathologically
  interacts with PostgreSQL's join planner. On the refactor, the 14 per-language EXISTS
  predicates collapse into a single `EXISTS (… language_id IN (…))` subquery.
- `filterTag` goes from 278 ms to ~17 ms because we replaced a LEFT JOIN on `key_meta → tags`
  (which multiplied rows and forced a `GROUP BY`) with a single EXISTS subquery and no group.
- The hot UI query (`filterState=UNTRANSLATED` across every selected language) is 3.3×
  faster thanks to the homogeneous-filter optimization (section 3.4) and the count query
  using non-correlated `k.id NOT IN (…)` (section 3.5).

The reproducible benchmark script is `e2e/scripts/baseQuery/perf-test.sh`. Usage:
`./e2e/scripts/baseQuery/perf-test.sh <base-url> <project-id> <api-token>`.

A narrower in-JVM perf suite (disabled in CI; run manually) also exists under
`backend/app/src/test/kotlin/io/tolgee/api/v2/controllers/translations/v2TranslationsController/`
— one test class per filter family, each isolated from the others so a regression is easy to
pinpoint.

### 1.4 Where CTEs are and aren't used

A CTE-based design where the `translation` table is pre-aggregated into one row per key with
one column per `(language, field)` combination was rejected early — for 100 languages that is
~1 000 output columns per key, and the Criteria API code building hundreds of `selectCase()`
expressions dynamically is unwieldy. The **two-query pattern** (slim key-only main query +
batched translation fetch) scales better and stays constant-width regardless of language count.

However, two specific filters — `filterUntranslatedAny` and the homogeneous
`filterState=lang,UNTRANSLATED` across many languages — benefit from **materialized CTEs**
for their aggregate subqueries. These filters must identify keys that are *not* fully
translated (or *not* in a disallowed state) across all selected languages, which requires a
`GROUP BY key_id HAVING COUNT(*) = N` aggregation. Without materialization, PostgreSQL may
choose to inline the subquery and re-evaluate the aggregation per outer key; with
`MATERIALIZED`, the aggregation is computed once and hash-joined.

These CTEs use Hibernate 6.6's `JpaCteCriteria` API:

```kotlin
val cteQuery = cb.createTupleQuery()
// ... build GROUP BY + HAVING query ...
val cte = (queryBase.query as JpaCteContainer).with(cteQuery)
cte.setMaterialization(CteMaterialization.MATERIALIZED)

// Reference from a subquery:
val subquery = queryBase.query.subquery(Long::class.java)
val cteRoot = (subquery as AbstractSqmSelectQuery<Long>).from(cte)
subquery.select(cteRoot.get<Long>("keyId"))
whereConditions.add(cb.not(root.get(Key_.id).`in`(subquery)))
```

The generated SQL:

```sql
WITH fullyTranslatedKeys AS MATERIALIZED (
  SELECT key_id FROM translation
  WHERE language_id IN (1, ..., 20) AND text IS NOT NULL AND text <> ''
  GROUP BY key_id HAVING COUNT(*) = 20
)
SELECT ... FROM key k
WHERE k.id NOT IN (SELECT keyId FROM fullyTranslatedKeys)
```

All other filters use plain JPA Criteria primitives (`EXISTS` / `NOT EXISTS` subqueries,
scalar correlated subqueries for sort/cursor, batched JPQL follow-up queries).

---

## 2. Before vs After — what the SQL looks like

These examples assume a project with **20 languages** selected (`langA … langT`), `pageSize=20`,
no filters, default sort.

### 2.1 BEFORE: one giant query

```sql
SELECT
  k.id, k.created_at, k.name, k.is_plural, k.plural_arg_name, k.max_char_limit,
  branch.name,
  ns.id, ns.name,
  km.description,
  -- screenshot count subquery
  (SELECT count(*) FROM screenshot s ...) AS screenshot_count,
  -- context exists subquery
  EXISTS (SELECT ... FROM keys_distance ...) AS context_present,
  -- 10 columns × 20 languages = 200 columns:
  t1.id, t1.text, t1.state, t1.outdated, t1.auto, t1.mt_provider,
    (SELECT count(*) FROM translation_comment WHERE translation_id = t1.id),
    (SELECT count(*) FROM translation_comment WHERE translation_id = t1.id AND state = 'NEEDS_RESOLUTION'),
    (SELECT count(*) FROM translation_suggestion WHERE key_id = k.id AND language_id = 1 AND state = 'ACTIVE'),
    (SELECT count(*) FROM translation_suggestion WHERE key_id = k.id AND language_id = 1),
  t2.id, t2.text, t2.state, t2.outdated, t2.auto, t2.mt_provider,
    -- ... same 4 correlated subqueries for t2
  -- ... repeated for t3 .. t20
FROM key k
  LEFT JOIN branch       ON branch.id = k.branch_id
  LEFT JOIN namespace ns ON ns.id = k.namespace_id
  LEFT JOIN key_meta  km ON km.key_id = k.id
  LEFT JOIN translation t1  ON t1.key_id  = k.id AND t1.language_id  = 1   -- langA
  LEFT JOIN translation t2  ON t2.key_id  = k.id AND t2.language_id  = 2   -- langB
  -- ... 18 more LEFT JOINs
  LEFT JOIN translation t20 ON t20.key_id = k.id AND t20.language_id = 20  -- langT
WHERE k.project_id = ?
  AND k.deleted_at IS NULL
  AND <branch + global filter conditions>
GROUP BY
  k.id, branch.name, ns.id, ns.name, km.description,
  t1.id, t2.id, /* ... */ t20.id
ORDER BY k.name
LIMIT 20
```

For 20 languages this is **20 LEFT JOINs**, **80 correlated subqueries** in the SELECT list,
and a 24-column `GROUP BY`. The query planner spends so long on join order that the codebase
sets `join_collapse_limit = 1` per request to short-circuit it.

### 2.2 AFTER: a slim main query + a few batched follow-ups

**Query 1 — main query (key-level only)**

```sql
SELECT
  k.id, k.created_at, k.name, k.is_plural, k.plural_arg_name, k.max_char_limit,
  branch.name,
  ns.id, ns.name,
  km.description,
  (SELECT count(*) FROM screenshot s ...) AS screenshot_count,
  EXISTS (SELECT ... FROM keys_distance ...) AS context_present
FROM key k
  LEFT JOIN branch       ON branch.id    = k.branch_id
  LEFT JOIN namespace ns ON ns.id        = k.namespace_id
  LEFT JOIN key_meta  km ON km.key_id    = k.id
WHERE k.project_id = ?
  AND k.deleted_at IS NULL
  AND <branch + global filter conditions>
  -- Translation-level filters become EXISTS subqueries here, e.g.:
  -- AND EXISTS (SELECT 1 FROM translation t
  --             WHERE t.key_id = k.id AND t.language_id = 1 AND t.state = 'TRANSLATED')
ORDER BY k.name
LIMIT 20
```

Constant 4 joins regardless of language count. No `GROUP BY`. No correlated `count()`
subqueries in the `SELECT` list.

**Query 2 — translations + all per-translation counts (single query)**

```sql
SELECT
  t.id, t.text, t.state, t.auto, t.mt_provider, t.outdated, t.qa_checks_stale,
  t.key_id, t.language_id,
  -- inline count subqueries — cheap because they run on ≤500 rows (page only)
  (SELECT count(*) FROM translation_comment tc WHERE tc.translation_id = t.id),
  (SELECT sum(CASE WHEN tc.state = 'NEEDS_RESOLUTION' THEN 1 ELSE 0 END)
   FROM translation_comment tc WHERE tc.translation_id = t.id),
  (SELECT count(*) FROM translation_suggestion ts
   WHERE ts.key_id = t.key_id AND ts.language_id = t.language_id),
  (SELECT sum(CASE WHEN ts.state = 'ACTIVE' THEN 1 ELSE 0 END)
   FROM translation_suggestion ts
   WHERE ts.key_id = t.key_id AND ts.language_id = t.language_id),
  (SELECT count(*) FROM translation_qa_issue qa
   WHERE qa.translation_id = t.id AND qa.state = 'OPEN')
FROM translation t
WHERE t.key_id IN (?, ?, ..., ? /* 20 ids from page */)
  AND t.language_id IN (?, ?, ..., ? /* 20 lang ids */)
```

Returns one row per existing translation, with comment/suggestion/QA counts inline. The inline
count subqueries are correlated to `t`, but because the outer set is small (at most
`pageSize × langCount` ≈ 400 rows), each subquery runs only ~400 times — trivially fast.
Application code groups results by key and attaches to the views. Missing `(key, language)`
pairs are filled in with an `UNTRANSLATED` placeholder so every returned view has an entry for
every requested language.

Plus the pre-existing batched fetches for tags (`Query 3`) and labels (`Query 4`) that were
already there before the refactor.

**Total**: 6 queries instead of 1 — but each is small and constant in shape, none of them
has a join count proportional to the language count, and the overall round-trip cost is far
lower than the cost of the single huge query in the BEFORE version.

### 2.3 IN-clause chunking

The `IN (...)` list in Query 2 is **chunked at 10 000 elements per batch** in
`TranslationViewDataProvider`. PostgreSQL's wire protocol has a hard limit of 65 535 bind
parameters per prepared statement, and Hibernate's JPQL `in :list` expands to one bind
parameter per element. The chunk size leaves comfortable headroom for other parameters in the
same query and lets the post-load method handle arbitrarily large input lists. Common page
sizes (20–100 keys) result in a single chunk, so there is no overhead in the typical case.

---

## 3. Where each filter moved

Two filter classes hold the per-condition logic:

- `QueryGlobalFiltering` — filters that apply to keys as a whole (key id/name, namespace,
  search, screenshots, branch, …)
- `QueryTranslationFiltering` — filters that apply to translations within a particular
  language (state, has-comments, has-suggestions, label, …)

The table below is a complete catalog. "Before" describes how the condition was implemented
on top of the N-join structure. "After" describes the current implementation.

### 3.1 Global filters (`QueryGlobalFiltering`)

| Filter                                              | Before                                                                                | After                                                                              |
|-----------------------------------------------------|---------------------------------------------------------------------------------------|------------------------------------------------------------------------------------|
| `filterKeyId`                                       | `WHERE key.id IN (...)`                                                               | unchanged                                                                          |
| `filterKeyName`                                     | `WHERE key.name IN (...)`                                                             | unchanged                                                                          |
| `filterNamespace`                                   | `WHERE namespace.name IN (...)`                                                       | unchanged                                                                          |
| `filterNoNamespace`                                 | `WHERE NOT EXISTS (subquery on namespace)`                                            | unchanged                                                                          |
| `filterTag`                                         | `LEFT JOIN keyMeta → tags`, `WHERE tag.name IN (...)`, **required `GROUP BY` outside**| `WHERE EXISTS (SELECT 1 FROM keyMeta km JOIN km.tags t WHERE km.key = k AND t.name IN (...))` |
| `filterNoTag`                                       | `LEFT JOIN keyMeta → tags` + `NOT EXISTS` subquery                                    | `WHERE NOT EXISTS (SELECT 1 FROM keyMeta km JOIN km.tags t WHERE km.key = k AND t.name IN (...))` |
| `filterTask` (incl. done / notDone variants)        | `LEFT JOIN tasks → task`, plus extra predicates, **required `GROUP BY` outside**      | `WHERE EXISTS (SELECT 1 FROM TaskKey tk WHERE tk.key = k AND tk.task.number IN (...) [AND tk.done = ...])` |
| `filterHasScreenshot` / `filterHasNoScreenshot`     | comparison against the screenshot count subquery                                      | unchanged                                                                          |
| `filterRevisionId`                                  | `WHERE key.id IN (subquery on activity)`                                              | unchanged                                                                          |
| `filterFailedKeysOfJob`                             | `WHERE key.id IN (temp table populated from batch job)`                               | unchanged                                                                          |
| `filterBranch`                                      | branch join + WHERE on branch name / default flag                                     | unchanged                                                                          |
| `filterDeletedByUserId`                             | `LEFT JOIN deletedBy user`, `WHERE deletedBy.id IN (...)`                             | unchanged                                                                          |
| `filterTranslatedAny`                               | `OR (t1.text IS NOT NULL AND t1.text<>'') OR ... OR (tN.text ...)`                    | `WHERE k.id IN (SELECT t.key_id FROM translation t WHERE t.language IN (...) AND t.text IS NOT NULL AND t.text<>'')` — non-correlated semi-join, evaluated once |
| `filterUntranslatedAny`                             | `OR (t1.text IS NULL OR t1.text='') OR ... OR (tN.text ...)`                          | `WHERE k.id NOT IN (SELECT t.key_id FROM translation t WHERE t.language IN (...) AND t.text IS NOT NULL AND t.text<>'' GROUP BY t.key_id HAVING count(*) = :langCount)` — non-correlated anti-join, evaluated once |
| `filterSearch`                                      | `OR upper(...) LIKE '%X%'` over key name + namespace + description **+ N translation text columns** | `OR upper(key.name) LIKE ... OR upper(namespace.name) LIKE ... OR upper(km.description) LIKE ... OR k.id IN (SELECT t.key_id FROM translation t WHERE t.language IN (...) AND upper(t.text) LIKE ...)` — the translation-text part is a **non-correlated** semi-join so PostgreSQL scans the translation table once |

### 3.2 Translation-level filters (`QueryTranslationFiltering`)

All filters here used to reference the per-language `translation` LEFT JOIN. Now each builds
its own `EXISTS` subquery against `Translation`.

The translation-level filters are still **OR-ed together** at query build time in
`TranslationsViewQueryBuilder.getWhereConditions` — combining `filterState=en,TRANSLATED` with
`filterHasUnresolvedCommentsInLang=de` returns the union of the two, not the intersection.
This OR semantics is preserved exactly as it was before the refactor.

| Filter                            | Before (per-language `t` join)                   | After (EXISTS subquery)                                                                          |
|-----------------------------------|--------------------------------------------------|--------------------------------------------------------------------------------------------------|
| `filterState=tag,STATE`           | `t.state = STATE` (with extra `OR t.state IS NULL` for `UNTRANSLATED`) | Homogeneous case: materialized CTE anti-join for UNTRANSLATED-containing sets, single EXISTS for others (section 3.4) — both count and data queries. Heterogeneous: single combined `EXISTS` with OR-ed `(lang, state)` pairs + per-lang `NOT EXISTS` for UNTRANSLATED-containing sets (section 3.4.1). Single-language: one `EXISTS` / `NOT EXISTS`. |
| `filterAutoTranslatedInLang=tag`  | `t.auto = true`                                  | `EXISTS (... t.language=tag AND t.auto=true)`                                                    |
| `filterTranslatedInLang=tag`      | `t.text IS NOT NULL AND t.text<>''`              | `EXISTS (... t.language=tag AND t.text IS NOT NULL AND t.text<>'')`                              |
| `filterUntranslatedInLang=tag`    | `t.text IS NULL OR t.text=''` (matches missing rows because of LEFT JOIN) | `NOT EXISTS (... t.language=tag AND t.text IS NOT NULL AND t.text<>'')` |
| `filterOutdatedLanguage=tag`      | `t.outdated = true`                              | `EXISTS (... t.language=tag AND t.outdated=true)`                                                |
| `filterNotOutdatedLanguage=tag`   | `t.outdated = false`                             | `EXISTS (... t.language=tag AND t.outdated=false)`                                               |
| `filterHasCommentsInLang=tag`     | `(SELECT count(*) FROM translation_comment WHERE translation_id=t.id) > 0` (correlated subquery already present in SELECT) | `EXISTS (SELECT 1 FROM TranslationComment tc WHERE tc.translation.key=k AND tc.translation.language=tag)` |
| `filterHasUnresolvedCommentsInLang=tag` | same correlated subquery, with extra `state='NEEDS_RESOLUTION'` filter   | `EXISTS (SELECT 1 FROM TranslationComment tc WHERE tc.translation.key=k AND tc.translation.language=tag AND tc.state='NEEDS_RESOLUTION')` |
| `filterHasSuggestionsInLang=tag`  | `(SELECT count(*) FROM translation_suggestion ...) > 0` (correlated)     | `EXISTS (SELECT 1 FROM TranslationSuggestion ts WHERE ts.key=k AND ts.language=tag AND ts.state='ACTIVE')` |
| `filterHasNoSuggestionsInLang=tag`| `... = 0`                                        | `NOT EXISTS (SELECT 1 FROM TranslationSuggestion ts WHERE ts.key=k AND ts.language=tag AND ts.state='ACTIVE')` |
| `filterLabel=tag,labelId`         | `EXISTS` subquery referencing the per-language join's `t.id` | `EXISTS (SELECT 1 FROM Translation t JOIN t.labels l WHERE t.key=k AND t.language=tag AND l.id IN (...))` (no longer needs the parent join) |
| `filterHasQaIssuesInLang=tag`     | `(SELECT count(*) FROM TranslationQaIssue ...) > 0` (correlated subquery in SELECT) | `EXISTS (SELECT 1 FROM TranslationQaIssue qa WHERE qa.translation.key=k AND qa.translation.language=tag AND qa.state='OPEN')`. Only emitted when the project has the `QA_CHECKS` feature enabled. |
| `filterQaCheckType=type`          | correlated `EXISTS` on `TranslationQaIssue` filtered by type | `EXISTS (SELECT 1 FROM TranslationQaIssue qa WHERE qa.translation.key=k AND qa.translation.language=tag AND qa.state='OPEN' AND qa.type IN (...))`. Same feature-gate as `filterHasQaIssuesInLang`; multiple `type` values are OR-ed within the `IN`. |

### 3.3 Special handling for missing translation rows

The pre-refactor implementation relied on `LEFT JOIN translation t ON t.key=k AND t.language=L`
producing a row with all `NULL`s when no translation exists for `(k, L)`. Two filters depend on
that behavior — they must match keys that have **no row** for the given language as well as
keys whose row is empty:

- `filterUntranslatedInLang` — semantically "no non-empty translation exists in this language",
  expressed in the new code as `NOT EXISTS` of a translated row. Both the missing-row case and
  the empty-text case fall through correctly.
- `filterState=tag,UNTRANSLATED` — expressed as a single `NOT EXISTS (state NOT IN allowed)` per
  language. The semantics is "either no row exists, or the existing row is in the allowed
  state set"; encoding the negation as `NOT EXISTS` collapses what would otherwise be two OR-ed
  subqueries (`EXISTS (state = UNTRANSLATED) OR NOT EXISTS (any row)`) into one.

Tests in `TranslationsControllerFilterTest`, `TranslationsControllerSnapshotTest`, and the new
`TranslationsControllerLabelFilterTest` (EE) cover these edge cases.

### 3.4 Homogeneous-filter optimization for `filterState`

The Tolgee UI lets the user pick a single state from a dropdown ("show me all UNTRANSLATED
keys") which the frontend then sends as `filterState=lang,STATE` once **per selected language**
— typically 5-20 identical filter clauses on the same state across N languages.

A naive implementation OR-es N per-language `EXISTS` subqueries together, which forces
PostgreSQL to evaluate `N * outer_key_count` correlated subqueries — pathological for projects
with tens of thousands of keys.

[StateFilterBuilder] detects this **homogeneous** case (every selected language has the same
state set) and emits an optimized predicate:

- If the allowed state set contains `UNTRANSLATED`:
  A **materialized CTE** computes the set of key IDs where every selected language has a
  "disallowed" state (`GROUP BY key_id HAVING COUNT(*) = :langCount`), then the main query
  anti-joins: `k.id NOT IN (SELECT keyId FROM fullyDisallowedStateKeys)`. The `MATERIALIZED`
  hint forces PostgreSQL to compute the aggregation once and hash it — without it, the planner
  may inline the subquery and re-evaluate per outer key. See section 1.4 for the Hibernate API.
- Otherwise (state set excludes `UNTRANSLATED`):
  `EXISTS (... language IN (...selectedLangs) AND state IN (...allowedStates))` — a single
  correlated existence check, far cheaper than N OR-ed subqueries.

This applies to **both count and data queries** — the materialized CTE benefits both equally
since the aggregation cost is the same regardless of LIMIT.

Single-language filters fall back to `singleLanguageStatePredicate` (one `EXISTS` or `NOT
EXISTS` subquery). Heterogeneous filters use `collapsedMultiStatePredicate` (section 3.4.1)
which collapses N per-language subqueries into one combined subquery plus missing-row
predicates.

`filterUntranslatedAny` uses the same materialized CTE approach (computing fully-translated
keys and anti-joining). `filterTranslatedAny` uses a correlated `EXISTS` (short-circuits on
the first matching translation row).

### 3.4.1 Heterogeneous `filterState` — multi-language collapse

When `filterState` targets different state sets per language
(`filterState=en,TRANSLATED&filterState=de,REVIEWED&…`) the homogeneous shortcut does not
apply. The naive implementation is N per-language `EXISTS` / `NOT EXISTS` subqueries OR-ed
together, which is what the refactor originally emitted. On a 40k-key project with 14
languages this regressed to ~540 ms (vs main's ~190 ms) because the count query has to
evaluate N separate non-correlated IN subqueries.

[StateFilterBuilder] now collapses the heterogeneous case into at most two kinds of
subqueries via [collapsedMultiStatePredicate]:

1. **One combined positive subquery.** A single `EXISTS (… (t.lang=L1 AND t.state IN S1) OR
   (t.lang=L2 AND t.state IN S2) OR …)` (data) or `k.id IN (SELECT t.key_id … WHERE …)`
   (count) matches keys with an explicit row in any of the requested `(lang, state)` tuples.
2. **One missing-row subquery per language whose set contains UNTRANSLATED.** Because a
   missing row counts as UNTRANSLATED in the sparse-data model (main does the same — see
   its `cb.or(condition, cb.isNull(translationStateField))` branch), we need an extra
   `NOT EXISTS (row in Li)` (data) or `k.id NOT IN (SELECT t.key_id … WHERE t.lang=Li)`
   (count). These cannot be collapsed with `IN` — "missing in at least one language" is OR,
   but `NOT IN (lang IN …)` expresses "missing in all languages".

All of those are OR-ed together.

For a pure positive heterogeneous filter (no UNTRANSLATED in any set) the count drops from N
subqueries to **1**. When UNTRANSLATED is mixed in for M of the N languages, the count is
`1 + M ≤ N`. On the same benchmark this fixed the regression: **539 ms → 185 ms**, on par
with main.

[collapsedMultiStatePredicate] is only used for heterogeneous filters.
[homogeneousStateCountPredicate] fires for both data and count queries — the materialized CTE
approach benefits both equally (the aggregation cost does not depend on LIMIT).

### 3.5 `isCountQuery` — correlated EXISTS for data, non-correlated IN for counts

The data query and count query have very different performance characteristics:

- **Data query**: has `LIMIT 20`, so PostgreSQL can short-circuit as soon as 20 matching rows
  are found. Correlated `EXISTS (… WHERE t.key_id = k.id)` is ideal here — per-key evaluation
  with a cheap index seek, stopping early under LIMIT.
- **Count query**: has no LIMIT and must evaluate every candidate key. Correlated `EXISTS`
  then runs the subquery ~40k times. A non-correlated `k.id IN (SELECT t.key_id …)` is
  dramatically cheaper — the inner query is evaluated once (single index scan) and the outer
  becomes a hash semi-join.

`TranslationsViewQueryBuilder` builds **separate `QueryBase` instances** for data and count,
passing `isCountQuery = false` / `true` respectively. The filter classes
(`QueryTranslationFiltering`, `QueryGlobalFiltering`) read the flag and emit:

```kotlin
// Data query (isCountQuery = false):
EXISTS (SELECT 1 FROM translation t WHERE t.key_id = k.id AND t.language_id = :lang AND …)

// Count query (isCountQuery = true):
k.id IN (SELECT t.key_id FROM translation t WHERE t.language_id = :lang AND …)
```

Same filter logic, single source of truth, different SQL shape. The flag is also applied to
`keyIdsQuery` (the "select all keys" endpoint) because it too runs without LIMIT.

`NOT EXISTS` in the data path becomes `NOT IN` in the count path. This is safe because the
inner query selects `translation.key_id`, which is a `NOT NULL` foreign key — so `NOT IN` has
no three-valued-logic pitfalls.

### 3.6 Multi-language collapse

When the same per-language filter is applied to several languages — a very common pattern
because the UI sends one filter param per selected language — the naive implementation emits
N per-language `EXISTS` subqueries OR-ed together. For count queries (no LIMIT), this means N
separate scans of the target table, each producing a hash that gets OR'd across. On a 39k-key
project with 14 languages, `filterOutdatedLanguage × 14 langs` regressed to ~750 ms.

The fix: since `exists(lang=L1, P) OR exists(lang=L2, P) OR …` is logically equivalent to
`exists(lang IN (L1, L2, …), P)`, we emit a **single** subquery with `language_id IN (…)`.

Applies to every translation-level filter where the predicate body is language-agnostic:

- `filterAutoTranslatedInLang` — `auto = true`
- `filterOutdatedLanguage` — `outdated = true`
- `filterNotOutdatedLanguage` — `outdated = false`
- `filterHasCommentsInLang` — `EXISTS (TranslationComment …)`
- `filterHasUnresolvedCommentsInLang` — `EXISTS (… state = NEEDS_RESOLUTION)`
- `filterHasSuggestionsInLang` — `EXISTS (TranslationSuggestion … state = ACTIVE)`
- `filterHasQaIssuesInLang` — `EXISTS (TranslationQaIssue … state = OPEN)`
- `filterQaCheckType` — single subquery across all selected languages

`filterHasNoSuggestionsInLang` is **not** collapsed: `NOT EXISTS (lang=L1) OR NOT EXISTS
(lang=L2)` ("missing in at least one language") is **not** the same as
`NOT EXISTS (lang IN (L1, L2))` ("missing in all languages"). It keeps its per-language
form and is OR'd across languages as before.

`filterLabel` is also not collapsed this way because different label id sets may be requested
per language — the predicate body is not language-agnostic.

`filterState` is a distinct case — the state set can differ per language, so the predicate
body is not uniform. See section 3.4.1 for its dedicated collapse (one combined subquery with
OR-ed `(lang=L AND state IN S)` pairs, plus missing-row predicates for UNT-containing langs).

After collapsing, `filterHasSuggestionsInLang × 14 langs` on the 39k-key project dropped
from 3526 ms (main) to 15 ms (refactored) — a ~235× improvement, because the 14 separate
scans of `translation_suggestion` became one.

---

## 4. File-by-file changes

All paths are relative to `backend/data/src/main/kotlin/io/tolgee/`.

### 4.1 `service/queryBuilders/translationViewBuilder/QueryBase.kt`

Foundation for the main query. Now returns only key-level columns.

- Removed `addLanguageSpecificFields()`, `addTranslationId()`, `addTranslationText()`,
  `addTranslationStateField()`, `addTranslationOutdatedField()`, `addAutoTranslatedField()`,
  and the correlated-subquery builders for comments / suggestions / QA issues. The associated
  state (`translationsTextFields`, `groupByExpressions`) is gone.
- Added `applyTranslationFilters()` which calls each filter method in
  `QueryTranslationFiltering` **once** (not per language) — the filter methods handle
  multi-language input internally. See section 4.2.
- Added `scalarTranslationText(languageId)` — builds a scalar correlated subquery
  `(SELECT t.text FROM translation t WHERE t.key=k AND t.language=:languageId)`. Used by sort
  and cursor pagination on `translations.{tag}.text`.
- `languages` is exposed (was `private`) so the filter classes can resolve language tags to ids.
- Constructor takes `qaEnabled: Boolean` threaded from `TranslationViewDataProvider`. When
  `false`, QA-specific translation filters (`filterHasQaIssuesInLang`, `filterQaCheckType`)
  are skipped. (The legacy `authenticationFacade` constructor parameter was removed.)
- Constructor also takes `isCountQuery: Boolean`. Threaded through to
  `QueryTranslationFiltering` and `QueryGlobalFiltering` so they can emit non-correlated
  `k.id IN (…)` predicates for count queries instead of correlated `EXISTS`. See section 3.5.

### 4.2 `service/queryBuilders/translationViewBuilder/QueryTranslationFiltering.kt`

Rewritten so every translation-level filter is an `EXISTS` / `NOT EXISTS` subquery (data
queries) or `k.id IN (…)` / `NOT IN (…)` (count queries, via the `isCountQuery` flag on
the constructor — see section 3.5). Each filter method is called **once** per query build
(not per language) and internally collapses its multi-language input into a single
`language_id IN (…)` subquery wherever the semantics allow (see section 3.6).

Entry points (all called from `QueryBase.applyTranslationFilters()`):

- `applyStateFilters()` — three-branch dispatch:
  1. Single language → `singleLanguageStatePredicate(language, states)`.
  2. Multiple languages, same state set → `homogeneousStateCountPredicate` — materialized CTE
     for UNTRANSLATED-containing sets, single EXISTS for others (section 3.4). Applies to both
     count and data queries.
  3. Heterogeneous → `collapsedMultiStatePredicate` — one combined positive subquery plus one
     missing-row predicate per UNT-containing language (section 3.4.1).
- `applyAutoTranslatedFilter()` — `filterAutoTranslatedInLang`, collapsed to one subquery.
- `applyUntranslatedInLangFilter()` — single-language `NOT EXISTS (non-empty)`.
- `applyTranslatedInLangFilter()` — single-language `EXISTS (non-empty)`.
- `applyHasCommentsFilter()` — `filterHasCommentsInLang`, collapsed.
- `applyHasUnresolvedCommentsFilter()` — `filterHasUnresolvedCommentsInLang`, collapsed.
- `applyHasSuggestionsFilter()` — `filterHasSuggestionsInLang`, collapsed.
- `applyHasNoSuggestionsFilter()` — `filterHasNoSuggestionsInLang`, **not** collapsed
  (intersection vs union mismatch under `NOT EXISTS`; see section 3.6).
- `applyLabelFilter()` — not collapsed because different label id sets can be requested per
  language.
- `applyQaFilters()` — `filterHasQaIssuesInLang` + `filterQaCheckType`, both collapsed. Only
  invoked by `QueryBase` when `qaEnabled` is true; on projects without the QA Checks feature
  the filter parameters are silently ignored.
- `applyOutdatedFilters()` — `filterOutdatedLanguage` / `filterNotOutdatedLanguage`, both
  collapsed.

Helpers — generic subquery building:

- `buildExistsOrIn<E>(entityClass, keyIdInSub, languageIdInSub, languageIds, extraConditions)`
  — **one generic helper** that replaces the four near-identical `buildXxxExistsMultiLang`
  methods from the previous version. Takes the entity class, path functions to reach `key_id`
  and `language_id` from the subquery root, the language id list, and a condition builder.
  Emits correlated `EXISTS` for data queries or non-correlated `k.id IN (…)` for count queries
  via the shared `wrapExistsOrIn(subquery)` method.
- `wrapExistsOrIn(subquery)` — the canonical EXISTS-vs-IN switch: `if (isCountQuery) IN else
  EXISTS`.
- `buildTranslationExists`, `buildCommentExists`, `buildActiveSuggestionExists`,
  `buildQaIssueExists` — thin entity-specific wrappers calling `buildExistsOrIn` with the
  right paths.
- `buildLabelExists(language, labelIds)` — per-language label subquery (needs a join on
  `Translation.labels`, so doesn't fit the generic helper). Uses `wrapExistsOrIn` for the
  final predicate.
- `languagePredicate(pathExpr, ids)` — emits `= :id` for a single id, `IN (…)` otherwise.
- `languageIdsForTags(tags)` — resolves tags to IDs, returns `null` for empty/missing (so
  callers can `?: return`).

State-filter logic is delegated to [StateFilterBuilder] — see section 4.2.1.

### 4.2.1 `service/queryBuilders/translationViewBuilder/StateFilterBuilder.kt`

Internal class encapsulating all `filterState` predicate logic. Extracted from
`QueryTranslationFiltering` because the state filter is the most complex translation-level
filter — three dispatch branches (section 3.4), heterogeneous collapse (section 3.4.1), and
the UNTRANSLATED missing-row equivalence (section 3.3). Isolating it keeps
`QueryTranslationFiltering` focused on orchestration.

Key method:

- `build(perLanguage: List<Pair<LanguageDto, Set<TranslationState>>>): Predicate?` — the
  single entry point. Returns `null` when the input resolves to nothing (unknown tags dropped).
  Dispatches to one of three strategies:
  1. Single language → `singleLanguageStatePredicate`
  2. Homogeneous (all languages same state set) → `homogeneousStateCountPredicate` — uses a
     materialized CTE for UNTRANSLATED-containing sets, single EXISTS for others. Applies to
     both count and data queries.
  3. Heterogeneous → `collapsedMultiStatePredicate` (combined positive subquery + per-UNT-lang
     missing-row predicates)

Self-contained: has its own `buildTranslationExists(language, extra)` helper (same structure
as the one in `QueryTranslationFiltering`) so it doesn't depend on the parent class for
subquery building.

### 4.3 `service/queryBuilders/translationViewBuilder/QueryGlobalFiltering.kt`

- `filterTag` and `filterNoTag` rewritten to use `EXISTS` / `NOT EXISTS` over `KeyMeta → tags`
  instead of joining the tag table on the main query.
- `filterTask` rewritten similarly: `EXISTS` over `TaskKey` instead of joining tasks.
- `filterTranslatedAny` is a **non-correlated** semi-join: `k.id IN (SELECT key_id FROM
  translation WHERE language IN (...) AND text IS NOT NULL AND text<>'')`. The inner subquery is
  computed once across the project's translations and the outer predicate becomes a hash semi-
  join — no correlation cost per outer key.
- `filterUntranslatedAny` uses a **materialized CTE** to compute the set of fully-translated
  key IDs (`GROUP BY key_id HAVING COUNT(*) = :langCount`), then anti-joins:
  `k.id NOT IN (SELECT keyId FROM fullyTranslatedKeys)`. The `MATERIALIZED` hint forces
  PostgreSQL to compute the aggregation once and hash it. Handles missing-row keys correctly:
  a key with zero rows is never in the CTE result and so is correctly classified as
  "untranslated". See section 1.4 for the Hibernate CTE API usage.
- `filterSearch` splits into key-level `LIKE`s on the (small) `fullTextFields` collection plus
  a **non-correlated** `k.id IN (SELECT t.key_id FROM Translation t WHERE t.language IN (...)
  AND upper(t.text) LIKE ...)` for the per-language text search. The non-correlated form lets
  PostgreSQL scan the translation table once and semi-join with the outer key set, rather than
  running a correlated EXISTS per outer key.

### 4.4 `service/queryBuilders/translationViewBuilder/TranslationsViewQueryBuilder.kt`

- `dataQuery` no longer applies a `GROUP BY`. With no row-multiplying joins on the main query,
  one is no longer needed.
- `countQuery` uses plain `cb.count(keyId)` instead of `countDistinct(root)` — same reason.
- `getOrderList` delegates to a new `resolveSortExpression` that:
  - Looks up key-level columns in the existing `querySelection` map.
  - Falls back to `QueryBase.scalarTranslationText` for `translations.{tag}.text` properties.
- Passes the `QueryBase` and `languages` to `CursorPredicateProvider` so it can do the same
  scalar-subquery resolution for cursor predicates.

### 4.5 `service/queryBuilders/translationViewBuilder/CursorPredicateProvider.kt`

`resolveExpression(property)`:

1. If `property` is a key-level column, look it up in the selection map.
2. If `property` matches `translations.{tag}.text`, build the same scalar correlated subquery
   used for sorting and treat its column type as `String`.
3. Otherwise throw `BadRequestException(CANNOT_SORT_BY_THIS_COLUMN)`.

### 4.6 `service/queryBuilders/translationViewBuilder/QuerySelection.kt`

Trivial change: building the `translations.{tag}.{field}` map key now goes through the
`KeyWithTranslationsView.translationProperty(...)` helper instead of inline string
concatenation. Single source of truth for the public sort/cursor wire format.

### 4.7 `service/queryBuilders/translationViewBuilder/TranslationViewDataProvider.kt`

Orchestrates the new multi-query flow. Both the count and data queries are built via
`TranslationsViewQueryBuilder`, which creates separate `QueryBase` instances for each with
`isCountQuery = true` / `false` (see section 3.5). The provider also resolves the project's
QA Checks feature flag and threads `qaEnabled` through to the query builder.

The legacy `SET join_collapse_limit TO 1` / `SET join_collapse_limit TO DEFAULT` native SQL
commands were removed — they were needed for the old N-join query (20+ joins made the planner
spend too long on join orderings) but the refactored query only has 3–4 small 1:0..1 joins,
well within PostgreSQL's default `join_collapse_limit = 8`. Removing them saves two round trips
per request.

After the main query runs:

1. **`populateTranslationsWithCounts(keyIds, languages, views, qaEnabled)`** — a single Tuple
   query that fetches translations AND all per-translation counts (comment count, unresolved
   comment count, total suggestion count, active suggestion count, QA issue count) using inline
   correlated count subqueries. These subqueries are cheap because the outer result set is small
   (at most `pageSize × langCount` ≈ 400 rows). `qaChecksStale` is a field on the `Translation`
   entity itself, so it is read alongside the other columns — no extra query needed. Pre-fills
   every requested `(key, language)` slot with an `UNTRANSLATED` placeholder so missing rows
   stay visible to consumers. Chunked at `IN_CLAUSE_CHUNK_SIZE`.
2. **`tagService.getTagsForKeyIds(keyIds)`** — already existed, attaches tags.
3. **`labelService.getByTranslationIdsIndexed(translationIds)`** — already existed, attaches
   labels.
4. **`populateQaIssues(translationIds, views)`** (only when `qaEnabled` **and** the caller
   opts in with `includeQaIssues = true`) — fetches the actual `TranslationQaIssue` objects
   via `qaIssueRepository.findByTranslationIds` and attaches them to each `TranslationView`'s
   `qaIssues` list. Independent of the count in step 1: counts are always loaded, but the full
   issue list is optional.

A `companion object` constant `IN_CLAUSE_CHUNK_SIZE = 10_000` controls the chunk size; see
section 2.3 above.

`TranslationViewDataProvider` gains three new constructor dependencies for the QA integration:
`qaIssueRepository: TranslationQaIssueRepository`, `projectFeatureGuard: ProjectFeatureGuard`,
and `projectService: ProjectService`.

### 4.8 `model/views/KeyWithTranslationsView.kt`

- `of(...)` no longer parses per-language data from the result row. It only consumes the 12
  key-level columns (+ 6 trashed-only columns when applicable). The translations map is empty
  at construction and is populated by `TranslationViewDataProvider.populateTranslations`.
- New companion helpers for the public sort/cursor wire format:
  - `translationProperty(languageTag, field)` builds the string `translations.{tag}.{field}`.
  - `parseTranslationProperty(property)` parses the same format back to `(tag, field)` or
    returns `null` if the input does not match.
- `toCursorValue(property)` uses `parseTranslationProperty` instead of inline `split`
  + index access. The four call sites in the codebase that touch the format
  (`QuerySelection`, `KeyWithTranslationsView.toCursorValue`, `TranslationsViewQueryBuilder`,
  `CursorPredicateProvider`) all go through these helpers — single source of truth.

### 4.9 `model/views/TranslationView.kt`

Six count/flag fields (`commentCount`, `unresolvedCommentCount`, `activeSuggestionCount`,
`totalSuggestionCount`, `qaIssueCount`, `qaChecksStale`) are now `var` instead of `val`. The
post-load methods overwrite the initial 0 / false values. `qaIssues: List<TranslationQaIssue>`
(existing `var`) is populated by `populateQaIssues` when the caller opts in with
`includeQaIssues = true`.

### 4.10 `service/queryBuilders/CursorUtil.kt`

Small unrelated improvement: `jacksonObjectMapper()` was being instantiated on every
`getCursor` / `parseCursor` call. Replaced with a single lazy companion-object instance
(`ObjectMapper` is documented as thread-safe once constructed, and Kotlin's `by lazy` is
thread-safe by default).

### 4.11 Test additions

| File                                                                                                 | What it covers                                                                                       |
|------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------|
| `backend/app/.../v2TranslationsController/TranslationsControllerCursorMatrixTest.kt`                 | Exhaustive cursor + sort matrix across String / Long / Timestamp columns and ASC/DESC, focused on translation-text sort columns since they migrated from direct column references to scalar subqueries. |
| `backend/app/.../v2TranslationsController/TranslationsControllerSnapshotTest.kt`                     | Comprehensive snapshot of `TranslationViewDataProvider` output for a broad fixture (multiple languages, tags, comments, states, outdated/auto flags). Asserts exact field values; acts as a safety net against regressions. |
| `backend/app/.../v2TranslationsController/AbstractPartialFixturePerfTestBase.kt`                     | Abstract base for the filter-specific perf tests. Sets up 10 000 keys × 20 languages with only the first 2 translated (the typical early-stage i18n shape). Subclasses implement `setupFilterSpecificFixture()` to add only the data their filter needs — each class runs with a fresh DB and an isolated dataset so regressions are easy to pin on one filter. |
| `backend/app/.../v2TranslationsController/TranslationViewQueryPerfTest.kt`                           | `@Disabled` diagnostic perf test (10 000 keys × 20 languages, **all fully translated**). Run manually to benchmark the baseline page-load case. |
| `backend/app/.../v2TranslationsController/TranslationViewQueryPartialFixturePerfTest.kt`             | `@Disabled` — extends the abstract base. Covers state / text-only filters: homogeneous `filterState=lang,UNTRANSLATED` across all 20 langs, `filterUntranslatedAny`, `filterUntranslatedInLang`, default page load. |
| `backend/app/.../v2TranslationsController/TranslationViewTagFilterPerfTest.kt`                       | `@Disabled` — isolated fixture with tagged keys only. Covers `filterTag` and `filterNoTag`. |
| `backend/app/.../v2TranslationsController/TranslationViewOutdatedFilterPerfTest.kt`                  | `@Disabled` — isolated fixture with outdated flags only. Covers `filterOutdatedLanguage`. |
| `backend/app/.../v2TranslationsController/TranslationViewQaCheckFilterPerfTest.kt`                   | `@Disabled` — isolated fixture with `useQaChecks=true` and seeded `EMPTY_TRANSLATION` QA issues. Covers `filterQaCheckType`. |
| `backend/app/.../v2TranslationsController/TranslationViewSuggestionsFilterPerfTest.kt`               | `@Disabled` — isolated fixture with active translation suggestions only. Covers `filterHasSuggestionsInLang`. |
| `backend/app/.../v2KeyController/KeyTrashFilterTest.kt`                                              | Coverage for the `keys/trash` endpoint: `filterDeletedByUserId`, trashed + tag/keyName/search combinations, deletedAt/deletedBy fields. Previously almost untested. |
| `ee/backend/tests/.../ee/api/v2/controllers/TranslationsControllerLabelFilterTest.kt`                | EE-only `filterLabel` coverage: language/project isolation, unassigned labels, OR semantics. |
| `tmp/bench-translations.sh`                                                                          | HTTP-level benchmark script. Runs a fixed set of filter scenarios against a local instance and prints median / min / max timings. Used to produce the numbers in section 1.3. |

A small helper change in `LabelsTestData.kt` exposes the second key's English/Czech
translations so the new label tests can mutate them.

---

## 5. Reference

### 5.1 The `translations.{tag}.{field}` wire format

This is the public sort/cursor property name shape used by the controller, the cursor
encoder/decoder, and the query builder.

| Component  | Source                                           |
|------------|--------------------------------------------------|
| `translations` literal | `KeyWithTranslationsView::translations.name`            |
| `{tag}`              | a language tag from the project (BCP-47, never contains `.`)          |
| `{field}`            | one of: `text`, `id`, `state` (see `TranslationView` for the full list) |

Building and parsing the format goes through two helpers on
`KeyWithTranslationsView.Companion`:

```kotlin
fun translationProperty(languageTag: String, field: String): String =
  "${KeyWithTranslationsView::translations.name}.$languageTag.$field"

fun parseTranslationProperty(property: String): Pair<String, String>? {
  val parts = property.split(".")
  if (parts.size != 3) return null
  if (parts[0] != KeyWithTranslationsView::translations.name) return null
  return parts[1] to parts[2]
}
```

The four call sites in the codebase that touch the format
(`QuerySelection`, `KeyWithTranslationsView.toCursorValue`, `TranslationsViewQueryBuilder`,
`CursorPredicateProvider`) all go through these helpers — single source of truth.

### 5.2 `TranslationViewDataProvider.IN_CLAUSE_CHUNK_SIZE`

Constant in the `TranslationViewDataProvider` companion object. Controls how many IDs are
bound per `IN (...)` clause in the post-load batched queries. PostgreSQL's wire protocol
maxes out at 65 535 bind parameters per statement, and Hibernate's JPQL `in :list` expands
to one bind parameter per element.

The current value is `10_000`. For the typical UI page size of 20–100 keys, a single chunk
covers everything; the chunking only kicks in for the bulk-key paths (e.g. very large
"select all" results piped through a follow-up).

### 5.3 Module map

| File                                                  | Role                                                                                            |
|-------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| `QueryBase.kt`                                        | Builds the main query: SELECT, FROM, key-level joins, where conditions container, scalar-text helper. Carries `isCountQuery` / `qaEnabled` flags. |
| `QueryGlobalFiltering.kt`                             | Adds key-level WHERE conditions (`whereConditions`). Emits `IN (…)` vs `EXISTS` based on `isCountQuery`. |
| `QueryTranslationFiltering.kt`                        | Adds translation-level WHERE conditions (`translationConditions`, OR-ed at build time). Collapses multi-language predicates into single `language_id IN (…)` subqueries. Generic `buildExistsOrIn<E>` helper. |
| `StateFilterBuilder.kt`                               | `filterState` predicate builder — homogeneous / heterogeneous / single-lang dispatch, UNTRANSLATED missing-row handling. |
| `QuerySelection.kt`                                   | Ordered map from cursor/sort property name to JPA `Selection`                                    |
| `TranslationsViewQueryBuilder.kt`                     | Assembles `dataQuery`, `countQuery`, `keyIdsQuery` from the above; passes `isCountQuery=true` for the no-LIMIT queries, `false` for the data query. |
| `CursorPredicateProvider.kt`                          | Builds the cursor WHERE predicate from a parsed cursor + the selection / scalar-text helper      |
| `CursorUtil.kt`                                       | Encodes/decodes the base64-JSON cursor token                                                     |
| `TranslationViewDataProvider.kt`                      | Orchestrates count query + data query + post-load queries                                        |
| `model/views/KeyWithTranslationsView.kt`              | Data class for a row in the page; companion holds the public sort/cursor wire format helpers     |
| `model/views/TranslationView.kt`                      | Data class for one translation in a row's `translations` map                                     |

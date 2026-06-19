# Type-fair batch job scheduling

**Date:** 2026-06-19
**Branch:** `dkrizan/batch-type-fair-scheduling`
**Area:** `backend/data` — `io.tolgee.batch`

## Problem

Batch chunk scheduling shares one global worker pool (per pod, `tolgee.batch.concurrency`,
production = 20) across every job type. Chunks are dequeued by `pollRoundRobin()` in
`BatchJobChunkExecutionQueue`, which is fair **per `jobId`** (the unit of fairness is one
batch job). PR #3428 added this to stop a single large job from monopolizing workers.

It does not cover the next level up: **many jobs of one type starving a few jobs of another
type.** Each batch job is a distinct `jobId`, so a flood of same-type jobs each claims an
equal turn.

### Production incident (2026-06-18 → 19)

A single project enqueued QA checks ~87 times in a ~12-minute window. Combined with other
projects, the live queue held:

| Job type          | Incomplete jobs | PENDING chunks |
| ----------------- | --------------- | -------------- |
| QA_CHECK          | 104             | 4,314          |
| AUTO_TRANSLATE    | 13              | 1,990          |
| MACHINE_TRANSLATE | 3               | 19             |

With ~120 jobs in the rotation and round-robin fair per `jobId`, a MACHINE_TRANSLATE job
received ~1/120 of scheduling turns. Symptom: user-triggered machine translation and auto
translation were effectively stuck (MACHINE_TRANSLATE completed ~20 chunks/h vs QA_CHECK
~1082 chunks/h), while the queue drained a 31k-chunk backlog over a full day.

The round-robin behaved correctly — it simply has no notion that 104 of those jobs are the
same low-value bulk type. **Fairness is per-job, but the unfairness is per-type.**

## Goal

Make scheduling fair **across job types** so that a large number of jobs of one type cannot
starve jobs of another type, **regardless of how many jobs of each type are queued.**

### Success criteria

- With N QA_CHECK jobs and 1 MACHINE_TRANSLATE job queued, the MACHINE_TRANSLATE chunk is
  served within the first few polls (a small constant), not after ~N polls.
- When only one type is present, behavior is unchanged from today (per-job round-robin).
- Per-job fairness from #3428 is preserved **within** each type.
- No regression in the existing batch concurrency / fairness / stress test suite.

## Non-goals (YAGNI)

- No priority weighting between types (QA_CHECK and MACHINE_TRANSLATE get an equal type
  share — sufficient to resolve the starvation).
- No per-type concurrency caps or reserved slots.
- No new configuration knobs.
- No change to project locking, debouncing, per-job concurrency, or the JobCharacter
  SLOW/FAST cap (see "Unchanged" below).
- Does not address what mass-enqueued the QA_CHECK jobs (tracked separately).

## Design

Add one grouping level **above** the existing per-job rotation, inside
`BatchJobChunkExecutionQueue` (placement A — keep the lock-free invariants where #3428's
already live). The rotation becomes two-level: **type → job → chunk.**

### Data structures

Replace the single job-level rotation with a nested one. Today:

```
jobQueues:       ConcurrentHashMap<Long, ConcurrentLinkedDeque<ExecutionQueueItem>>   // jobId -> chunks
roundRobinOrder: ConcurrentOrderedSet<Long>                                            // jobId rotation
```

After:

```
jobQueues:   ConcurrentHashMap<Long, ConcurrentLinkedDeque<ExecutionQueueItem>>        // unchanged: jobId -> chunks
jobsByType:  ConcurrentHashMap<BatchJobType, ConcurrentOrderedSet<Long>>               // type -> jobId rotation
typeOrder:   ConcurrentOrderedSet<BatchJobType>                                         // type rotation
```

`queuedExecutionIds`, `totalSize`, `jobCharacterCounts` are unchanged.

`ConcurrentOrderedSet` (already used for `roundRobinOrder`) gives idempotent `addLast()`,
which keeps the rotations duplicate-free under concurrent poll/add races.

### `pollRoundRobin()` algorithm

```
if empty: return null
loop (bounded by typeOrder.size + 1 to avoid spinning on concurrent drain):
  type  = typeOrder.pollFirst()        ?: return null
  jobId = jobsByType[type].pollFirst() ?: continue        // type drained concurrently
  item  = jobQueues[jobId].removeFirst()

  // re-queue tails for fairness, atomically (see invariants):
  if jobQueues[jobId] still non-empty: jobsByType[type].addLast(jobId)
  if jobsByType[type]  still non-empty: typeOrder.addLast(type)

  return item
```

One chunk is served per `(type, job)` visit; the type goes to the back of `typeOrder`, so
every type present gets a turn before any type gets a second. Within a type, jobs rotate
exactly as #3428 specifies.

### Atomicity / invariants

Maintained the same way as today — all side effects performed inside
`ConcurrentHashMap.compute()` blocks so they are atomic with the deque mutation:

- **I1:** `jobId ∈ jobsByType[type]` ⟺ `jobQueues[jobId]` is non-empty.
- **I2:** `type ∈ typeOrder` ⟺ `jobsByType[type]` is non-empty.
- **I3:** `totalSize`, `jobCharacterCounts`, `queuedExecutionIds` change exactly once per
  item, atomically with the deque mutation.

`addSingleItem`, the `REMOVE` event handler, and `removeJobExecutions(jobId)` must all keep
both levels (`jobsByType`, `typeOrder`) consistent — e.g. when a job's deque empties, remove
its `jobId` from `jobsByType[type]`, and if that type's set empties, remove the type from
`typeOrder`. `clear()` clears all five structures.

The bounded retry loop already present in `pollRoundRobin` is extended to tolerate a type
whose job-set drained concurrently (skip to the next type).

### Supporting changes

1. **`ExecutionQueueItem`** gains `val jobType: BatchJobType` — the grouping key. The item
   is JSON-serialized to Redis inside `JobQueueItemsEvent`; adding an enum field is
   backward-safe for in-flight messages (absent field tolerated, or handled via a default —
   verify the Jackson config used for these events).
2. **`populateQueue()` query** selects `bk.type` and passes it to
   `BatchJobChunkExecutionDto`; both `toItem(...)` helpers populate `jobType`. The owning
   `BatchJob` is already joined on every path that builds an item, so the type is available
   without extra queries.
3. **`BatchJobConcurrentLauncher`** is unchanged — it still calls `pollRoundRobin()`.

### Unchanged (and why)

- **JobCharacter SLOW/FAST cap** (`canRunJobWithCharacter`, `ceil(ratio × concurrency)`):
  orthogonal — it prevents slow chunks from occupying all 20 slots. With type-fair feeding,
  MACHINE_TRANSLATE now reaches the launcher often enough to actually claim its SLOW slots.
- **Project locking** (`BatchJobProjectLockingManager`), **debouncing**,
  **`maxPerJobConcurrency`**: independent concerns, untouched.
- **Per-pod scope:** the rotation is per-pod in-memory (as today). Each pod independently
  type-fair-rotates its local queue; no cross-pod coordination needed for fairness.

## Testing

Extend the suite #3428 added (`BatchJobRoundRobinTest.kt`, plus queue-level unit tests):

- **Type starvation (the fix):** enqueue 100 QA_CHECK jobs (1 chunk each) + 1
  MACHINE_TRANSLATE job; assert the MACHINE_TRANSLATE chunk is polled within the first few
  polls (≤ small constant, e.g. ≤ 3), not after ~100.
- **Type interleaving:** with 3 types present, assert poll order cycles types
  (QA, MT, AT, QA, MT, AT, …).
- **Per-job fairness within a type preserved:** two QA_CHECK jobs of many chunks alternate
  (the #3428 guarantee, now scoped under a type).
- **Single type unchanged:** one type with several jobs behaves exactly as current
  per-job round-robin.
- **Invariants under concurrency:** concurrent add/poll/remove leaves `typeOrder`,
  `jobsByType`, `jobQueues`, `totalSize`, and `queuedExecutionIds` consistent (no orphaned
  type/job entries, `totalSize` matches actual items). Reuse the stress-test harness.
- Full existing batch suite stays green.

Run per project convention:
`./gradlew :data:test --tests "io.tolgee.batch.*" --console=plain` and the relevant
`:server-app:test` batch tests.

## Risks

- **Lock-free hot path.** The change touches carefully-tuned concurrent code. Mitigation:
  reuse the exact `compute()` discipline and invariant style already documented in the
  class; cover with the concurrency/stress tests above before merge.
- **Redis item compatibility.** New `jobType` field on a Redis-serialized DTO. Mitigation:
  confirm Jackson tolerates the added field for messages in flight during deploy; provide a
  safe default if needed.
- **Many distinct types reduce each type's share to 1/T.** With the real, bounded set of
  active types (~5–6) this is a non-issue and still far better than 1/120. Out of scope to
  add weighting now.

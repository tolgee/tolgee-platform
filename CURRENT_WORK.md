# Current Work Context

This file tracks ongoing work for context in AI coding sessions.

## Active PRs (as of 2026-01-19)

### Batch Job Performance Testing & Optimization

| PR | Title | Base | Status | URL |
|----|-------|------|--------|-----|
| #3389 | test: Batch job performance test script improvements | main | OPEN | https://github.com/tolgee/tolgee-platform/pull/3389 |
| #3392 | refactor: optimize batch job state management | #3389 | OPEN | https://github.com/tolgee/tolgee-platform/pull/3392 |

**Merge order**: #3389 first, then #3392 (optimization depends on script improvements)

#### PR #3389 - Script Improvements
Branch: `jancizmar/batch-job-test-script-improvements`

Contains:
- Batch job performance test script (`e2e/scripts/batch/run-batch-mt-job-with-2-pods-and-redis.sh`)
- NO_OP batch job type for performance testing
- Fix: Staggered instance startup to prevent Liquibase race condition (first instance starts 3 seconds before others)
- Configurable batch concurrency parameter (`-C`, default: 20 to match production)
- Production-like test config documented: 10000 items, 3 instances, concurrency 20

#### PR #3392 - Optimization
Branch: `jancizmar/batch-job-optimization`

Contains:
- Lock-free O(1) updates for batch job state handling
- Atomic counters for batch job completion tracking
- Enhanced batch job state management with optimized data structures
- Removed pessimistic locking from chunk execution queue
- **O(n) bottleneck elimination** (2026-01-19):
  - `BatchJobChunkExecutionQueue`: Added `jobCharacterCounts` counter map for O(1) character count lookups (was iterating entire queue on every chunk)
  - `BatchJobConcurrentLauncher`: Added `runningJobCharacterCounts` counter map for O(1) running job character lookups (was filtering runningJobs on every chunk)
  - Pass `BatchJobDto` through call chain to reduce redundant `getJobDto` calls
  - **Result**: Throughput now stable at ~160-220 chunks/s regardless of queue size (was degrading from ~500/s to ~30/s with large queues due to O(n²) total complexity)
- Files modified:
  - `backend/data/src/main/kotlin/io/tolgee/batch/BatchJobActionService.kt`
  - `backend/data/src/main/kotlin/io/tolgee/batch/BatchJobConcurrentLauncher.kt`
  - `backend/data/src/main/kotlin/io/tolgee/batch/ProgressManager.kt`
  - `backend/data/src/main/kotlin/io/tolgee/batch/state/BatchJobStateProvider.kt`
  - `backend/data/src/main/kotlin/io/tolgee/batch/BatchJobChunkExecutionQueue.kt`
  - `backend/app/src/test/kotlin/io/tolgee/batch/AbstractBatchJobsGeneralTest.kt`

### Bug Fixes

| PR | Title | Base | Status | URL |
|----|-------|------|--------|-----|
| #3393 | fix: prevent lock loss in SimpleLockingProvider | main | OPEN | https://github.com/tolgee/tolgee-platform/pull/3393 |
| #3394 | fix: count per-job executions for maxPerJobConcurrency check | main | OPEN | https://github.com/tolgee/tolgee-platform/pull/3394 |

#### PR #3393 - SimpleLockingProvider Fix
Branch: `jancizmar/fix-simple-locking-provider`

Problem: `WeakHashMap` could cause locks to be GC'd between calls when using string concatenation for lock names, breaking synchronization.

Fix:
- Replace `WeakHashMap` with `ConcurrentHashMap`
- Add scheduled cleanup (every 60s) using atomic `compute()` to remove unused locks
- Check both `isLocked` and `hasQueuedThreads()` before removing

#### PR #3394 - maxPerJobConcurrency Fix
Branch: `jancizmar/fix-max-per-job-concurrency`

Problem: `maxPerJobConcurrency` was checking global `runningJobs.size` instead of counting executions for the specific job, causing flaky test `mt job respects maxPerJobConcurrency`.

Fix: Count only executions for the specific job:
```kotlin
runningJobs.values.count { it.first.id == this.jobId } < maxPerJobConcurrency
```

### Documentation

| PR | Title | Base | Status | URL |
|----|-------|------|--------|-----|
| #3391 | docs: document multi-repository structure in AGENTS.md | main | OPEN | https://github.com/tolgee/tolgee-platform/pull/3391 |

Added repository structure documentation explaining the multi-repo setup (platform-dev-start wrapper, tolgee-platform, billing).

---

*Last updated: 2026-01-19*

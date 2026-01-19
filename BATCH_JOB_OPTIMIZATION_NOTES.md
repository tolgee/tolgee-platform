# Batch Job Optimization Notes

## Branch: `jancizmar/batch-job-optimization`

## Problem Statement

Batch job orchestration overhead was too high, causing poor throughput. With 5000 NO_OP chunks, the system was achieving only ~100-110 chunks/second due to expensive state management operations.

## Key Bottlenecks Identified

Using the timing aspect (`BatchJobTimingAspect`), we identified these bottlenecks:

1. **`STATE_GET` (~5ms per call)** - `batchJobStateProvider.get(batchJobId)` retrieves the entire state map. Called in `trySetExecutionRunning` but the result wasn't even used by the `canRunFn` lambda.

2. **`PROGRESS_HANDLE` (~7ms per call)** - Multiple Redis round-trips for counter operations inside `handleProgress`.

3. **Multiple Redis calls per chunk** - Each counter operation (get/increment) is a separate Redis call averaging 0.5-0.7ms.

## Optimizations Implemented (Committed)

### 1. Lock-free O(1) State Updates
- Replaced locked `updateState` with atomic counter-based operations
- Added counters: `completedChunksCount`, `failedCount`, `cancelledCount`, `progressCount`, `committedCount`, `runningCount`
- `handleProgress` now uses counters instead of iterating the state map

### 2. Single Execution Operations
- Added `getSingleExecution`, `updateSingleExecution`, `removeSingleExecution` for O(1) operations on individual executions
- Avoids loading/saving the entire state map for single execution changes

### 3. Counter-based Job Completion Detection
- `handleProgress` tracks status changes with `wasCountableAsCompleted`/`isNowCountableAsCompleted`
- Only increments counters when status actually changes to prevent double-counting
- `handleChunkCompletedCommitted` uses atomic `incrementCommittedCountAndGet` to detect job completion

## Current Uncommitted Changes

### Optimization: Remove expensive `STATE_GET` from `trySetExecutionRunning`

**Files modified:**
- `backend/data/src/main/kotlin/io/tolgee/batch/ProgressManager.kt`
- `backend/data/src/main/kotlin/io/tolgee/batch/BatchJobConcurrentLauncher.kt`

**Changes:**

1. **`ProgressManager.trySetExecutionRunning`** - Removed the `canRunFn` parameter and the expensive `batchJobStateProvider.get(batchJobId)` call:
   ```kotlin
   // BEFORE: Called get() which loaded entire state map (~5ms)
   fun trySetExecutionRunning(
     executionId: Long,
     batchJobId: Long,
     canRunFn: (Map<Long, ExecutionState>) -> Boolean,  // Lambda never used the state!
   ): Boolean {
     val state = batchJobStateProvider.get(batchJobId)  // EXPENSIVE!
     if (!canRunFn(state)) { return false }
     // ...
   }

   // AFTER: No more expensive get() call
   fun trySetExecutionRunning(
     executionId: Long,
     batchJobId: Long,
   ): Boolean {
     batchJobStateProvider.ensureInitialized(batchJobId)
     batchJobStateProvider.incrementRunningCount(batchJobId)
     // ...
   }
   ```

2. **`BatchJobConcurrentLauncher.trySetRunningState`** - Moved `maxPerJobConcurrency` check before calling `trySetExecutionRunning`:
   ```kotlin
   // BEFORE: Check was inside lambda passed to trySetExecutionRunning
   private fun ExecutionQueueItem.trySetRunningState(): Boolean {
     return progressManager.trySetExecutionRunning(this.chunkExecutionId, this.jobId) {
       val maxPerJobConcurrency = batchJobService.getJobDto(this.jobId).maxPerJobConcurrency
       // ... check using runningJobs.values.count (NOT the state map!)
     }
   }

   // AFTER: Check done before calling trySetExecutionRunning
   private fun ExecutionQueueItem.trySetRunningState(): Boolean {
     val maxPerJobConcurrency = batchJobService.getJobDto(this.jobId).maxPerJobConcurrency
     if (maxPerJobConcurrency != -1) {
       val runningForThisJob = runningJobs.values.count { it.first.id == this.jobId }
       if (runningForThisJob >= maxPerJobConcurrency) {
         return false
       }
     }
     return progressManager.trySetExecutionRunning(this.chunkExecutionId, this.jobId)
   }
   ```

3. **Added rollback call** when `canLockJobForProject` fails after `trySetRunningState` succeeds:
   ```kotlin
   if (!batchJobProjectLockingManager.canLockJobForProject(executionItem.jobId)) {
     // Rollback the state change made in trySetRunningState
     progressManager.rollbackSetToRunning(executionItem.chunkExecutionId, executionItem.jobId)
     // ...
   }
   ```

## Expected Performance Improvement

Removing the `STATE_GET` call should eliminate ~25 seconds of overhead for 5000 chunks (5ms × 5000 = 25,000ms).

## Test Status

- **51/52 tests passing** in initial run
- **`cancels a job` test** is flaky - sometimes fails with `batch_job_cancellation_timeout`
  - This appears to be a pre-existing race condition, not caused by the optimization
  - Test passed on subsequent runs

## Enhanced Timing Instrumentation

Added more granular timing aspects in `BatchJobTimingAspect.kt`:
- All `BatchJobStateProvider` counter operations (STATE_GET_*, STATE_INC_*, STATE_ADD_*)
- Event publishing (EVENT_PROGRESS, EVENT_SUCCEEDED, etc.)
- `handleJobStatus` in ProgressManager

## Files Changed (Uncommitted)

```
backend/data/src/main/kotlin/io/tolgee/batch/ProgressManager.kt
backend/data/src/main/kotlin/io/tolgee/batch/BatchJobConcurrentLauncher.kt
backend/app/src/test/kotlin/io/tolgee/batch/BatchJobTimingAspect.kt
```

## Next Steps

1. Run full batch job test suite to confirm all tests pass
2. Run performance test to measure improvement from STATE_GET removal
3. Consider batching multiple Redis operations into pipelines for further optimization
4. Commit and push the changes

## Running the Performance Test

```bash
./gradlew :server-app:test --tests "io.tolgee.batch.BatchJobNoOpPerformanceWithRedisTest"
```

## Running All Batch Job Tests

```bash
./gradlew :server-app:test --tests "io.tolgee.batch.*" --tests "io.tolgee.api.v2.controllers.batch.*"
```

## Run Configuration

An IDEA run configuration for batch job tests is available at:
`billing/.run/All Batch Job Tests.run.xml`

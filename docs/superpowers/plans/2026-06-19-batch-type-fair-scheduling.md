# Type-fair Batch Job Scheduling Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make batch chunk scheduling fair across job *types* so a flood of one type (e.g. 100+ QA_CHECK jobs) can no longer starve other types (MACHINE_TRANSLATE, AUTO_TRANSLATE).

**Architecture:** Extend the existing per-job round-robin in `BatchJobChunkExecutionQueue` (PR #3428) with a second rotation level. Today: rotate across jobs (`roundRobinOrder`). After: rotate across **types** (`typeOrder`), then across **jobs within a type** (`jobsByType`), then serve one chunk from the chosen job's deque (`jobQueues`, unchanged). Each queued chunk carries its `jobType` so the queue can group by it.

**Tech Stack:** Kotlin, Spring Boot, Jackson (Redis pub/sub queue events), JUnit 5 + AssertJ + mockito-kotlin. Lock-free concurrency via `ConcurrentHashMap.compute()` + `ConcurrentOrderedSet` (a `ReentrantLock`-guarded `LinkedHashSet`).

## Global Constraints

- The queue's shared structures live in the class **companion object** (process-global, per pod). Tests call `queue.clear()` in `@BeforeEach` — keep all structures cleared there.
- **All side effects** (rotation membership, `totalSize`, `jobCharacterCounts`, `queuedExecutionIds`) MUST be performed inside the relevant `ConcurrentHashMap.compute()` block so they are atomic with the deque mutation. This is the existing invariant discipline — preserve it exactly.
- **Lock order is fixed:** `jobQueues`/`jobsByType` CHM key-lock → `ConcurrentOrderedSet` lock. NEVER call `jobQueues.compute(...)` or `jobsByType.compute(...)` while holding a `ConcurrentOrderedSet` lock. (`ConcurrentOrderedSet.pollFirst/addLast/remove` only take their own lock and never re-enter compute — keep it that way.)
- `ConcurrentOrderedSet.addLast` is **idempotent** (no-op if present). Rely on this so concurrent producers/consumers can both re-add a type/job without creating duplicates.
- Run tests per repo rule: `./gradlew :<module>:test --tests "<FQN>" --console=plain`. Queue + receiver unit tests are module `:data`; Spring batch integration tests are module `:server-app`. Gradle needs the sandbox bypass (`dangerouslyDisableSandbox: true`).
- Conventional commits; **no AI attribution** in any git content (repo rule).
- Default value `BatchJobType.NO_OP` on `ExecutionQueueItem.jobType` exists ONLY as a rolling-deploy deserialization fallback for queue events produced by an older instance. Every in-process construction path sets it explicitly.

---

## File Structure

- `backend/data/.../batch/data/ExecutionQueueItem.kt` — add `jobType` field (grouping key).
- `backend/data/.../batch/data/BatchJobChunkExecutionDto.kt` — add `jobType` field.
- `backend/data/.../batch/BatchJobChunkExecutionQueue.kt` — two-level rotation; propagate `jobType`; populate-query change. **Core of the change.**
- `backend/data/.../pubSub/RedisPubSubReceiver.kt` — tolerate cross-version queue-event payloads.
- `backend/data/src/test/.../batch/BatchJobChunkExecutionQueueTest.kt` — extend: jobType round-trip + type-fairness behavior (primary coverage).
- `backend/data/src/test/.../batch/BatchJobChunkExecutionQueuePerformanceTest.kt` — update item factory signature.
- `backend/data/src/test/.../pubSub/RedisPubSubReceiverTest.kt` — **new**: cross-version deserialization.

---

## Task 1: Thread `jobType` through the queue item and DTO

Additive plumbing — no scheduling-behavior change yet. The grouping key rides along on every queued chunk so Task 2 can use it.

**Files:**
- Modify: `backend/data/src/main/kotlin/io/tolgee/batch/data/ExecutionQueueItem.kt`
- Modify: `backend/data/src/main/kotlin/io/tolgee/batch/data/BatchJobChunkExecutionDto.kt`
- Modify: `backend/data/src/main/kotlin/io/tolgee/batch/BatchJobChunkExecutionQueue.kt:146,236-244`
- Test: `backend/data/src/test/kotlin/io/tolgee/batch/BatchJobChunkExecutionQueueTest.kt`
- Test: `backend/data/src/test/kotlin/io/tolgee/batch/BatchJobChunkExecutionQueuePerformanceTest.kt`

**Interfaces:**
- Produces: `ExecutionQueueItem.jobType: BatchJobType` (default `BatchJobType.NO_OP`); `BatchJobChunkExecutionDto.jobType: BatchJobType` (no default); test helper `item(id, jobId, character = FAST, jobType = BatchJobType.QA_CHECK)`.

- [ ] **Step 1: Update the unit-test `item()` helper to accept a `jobType`, and add a round-trip test (RED)**

In `BatchJobChunkExecutionQueueTest.kt`, add the import and replace the `item(...)` helper, then add a test.

Add import near the top:
```kotlin
import io.tolgee.batch.data.BatchJobType
```

Replace the helper (currently at lines 31-35):
```kotlin
  private fun item(
    id: Long,
    jobId: Long,
    character: JobCharacter = JobCharacter.FAST,
    jobType: BatchJobType = BatchJobType.QA_CHECK,
  ) = ExecutionQueueItem(
    chunkExecutionId = id,
    jobId = jobId,
    executeAfter = null,
    jobCharacter = character,
    jobType = jobType,
  )
```

Add this test (anywhere in the class):
```kotlin
  @Test
  fun `pollRoundRobin preserves jobType on items`() {
    queue.addItemsToLocalQueue(listOf(item(1, jobId = 1, jobType = BatchJobType.MACHINE_TRANSLATE)))
    assertThat(queue.pollRoundRobin()?.jobType).isEqualTo(BatchJobType.MACHINE_TRANSLATE)
  }
```

- [ ] **Step 2: Run the test — verify it FAILS to compile**

Run: `./gradlew :data:test --tests "io.tolgee.batch.BatchJobChunkExecutionQueueTest" --console=plain`
Expected: compilation error — `ExecutionQueueItem` has no parameter `jobType` / `item` call mismatch.

- [ ] **Step 3: Add `jobType` to `ExecutionQueueItem`**

Replace the whole body of `ExecutionQueueItem.kt`:
```kotlin
package io.tolgee.batch.data

import io.tolgee.batch.JobCharacter

data class ExecutionQueueItem(
  val chunkExecutionId: Long,
  val jobId: Long,
  var executeAfter: Long?,
  val jobCharacter: JobCharacter,
  var managementErrorRetrials: Int = 0,
  // Grouping key for type-fair round-robin (see BatchJobChunkExecutionQueue.pollRoundRobin).
  // Defaulted ONLY so a queue event serialized by an older instance (without this field)
  // still deserializes during a rolling deploy; the 60s populateQueue() re-read then corrects
  // the grouping from the DB. Every in-process construction path sets it explicitly.
  val jobType: BatchJobType = BatchJobType.NO_OP,
)
```

- [ ] **Step 4: Add `jobType` to `BatchJobChunkExecutionDto`**

Replace the constructor in `BatchJobChunkExecutionDto.kt` (keep the existing KDoc/package/imports, add the `BatchJobType` import):
```kotlin
package io.tolgee.batch.data

import io.tolgee.batch.JobCharacter
import java.util.Date

/**
 * DTO object for the BatchJobChunkExecution. Contains the bare minimum needed for the
 * BatchJobChunckExecutionQueue.
 *
 * @author Geert Zondervan <zondervan@serviceplanet.nl>
 */
class BatchJobChunkExecutionDto(
  val id: Long,
  val batchJobId: Long,
  var executeAfter: Date?,
  val jobCharacter: JobCharacter,
  val jobType: BatchJobType,
)
```

- [ ] **Step 5: Select `bk.type` in the populate query and propagate in both `toItem()` helpers**

In `BatchJobChunkExecutionQueue.kt`, change the JPQL constructor expression (line ~146) to add `bk.type`:
```kotlin
          select new io.tolgee.batch.data.BatchJobChunkExecutionDto(bjce.id, bk.id, bjce.executeAfter, bk.jobCharacter, bk.type)
```

Replace the two `toItem()` helpers (lines ~236-244):
```kotlin
  private fun BatchJobChunkExecution.toItem(
    // Yes. jobCharacter is part of the batchJob entity.
    // However, we don't want to fetch it here, because it would be a waste of resources.
    // So we can provide the jobCharacter here.
    jobCharacter: JobCharacter? = null,
  ) = ExecutionQueueItem(
    id,
    batchJob.id,
    executeAfter?.time,
    jobCharacter ?: batchJob.jobCharacter,
    jobType = batchJob.type,
  )

  private fun BatchJobChunkExecutionDto.toItem(providedJobCharacter: JobCharacter? = null) =
    ExecutionQueueItem(
      id,
      batchJobId,
      executeAfter?.time,
      providedJobCharacter ?: jobCharacter,
      jobType = jobType,
    )
```

Note: passing `jobType` by name leaves `managementErrorRetrials` at its default `0` (unchanged behavior).

- [ ] **Step 6: Update the performance-test item factory**

In `BatchJobChunkExecutionQueuePerformanceTest.kt`, add the import `import io.tolgee.batch.data.BatchJobType` and replace `makeItem` (lines ~46-54):
```kotlin
  private fun makeItem(
    id: Long,
    jobId: Long,
    jobType: BatchJobType = BatchJobType.QA_CHECK,
  ) = ExecutionQueueItem(
    chunkExecutionId = id,
    jobId = jobId,
    executeAfter = null,
    jobCharacter = JobCharacter.FAST,
    jobType = jobType,
  )
```

- [ ] **Step 7: Run unit + performance tests — verify they PASS**

Run: `./gradlew :data:test --tests "io.tolgee.batch.BatchJobChunkExecutionQueueTest" --tests "io.tolgee.batch.BatchJobChunkExecutionQueuePerformanceTest" --console=plain`
Expected: PASS (new `preserves jobType` test passes; all existing queue + perf tests still pass — they all use a single jobType so per-job behavior is unchanged).

- [ ] **Step 8: Commit**

```bash
git add backend/data/src/main/kotlin/io/tolgee/batch/data/ExecutionQueueItem.kt \
        backend/data/src/main/kotlin/io/tolgee/batch/data/BatchJobChunkExecutionDto.kt \
        backend/data/src/main/kotlin/io/tolgee/batch/BatchJobChunkExecutionQueue.kt \
        backend/data/src/test/kotlin/io/tolgee/batch/BatchJobChunkExecutionQueueTest.kt \
        backend/data/src/test/kotlin/io/tolgee/batch/BatchJobChunkExecutionQueuePerformanceTest.kt
git commit -m "refactor: carry jobType on batch queue items and DTO"
```

---

## Task 2: Two-level (type → job) round-robin in the queue

Replace the single job-level rotation with a type rotation above the job rotation. This is the behavioral fix.

**Files:**
- Modify: `backend/data/src/main/kotlin/io/tolgee/batch/BatchJobChunkExecutionQueue.kt` (companion fields, `addSingleItem`, `onJobItemEvent` REMOVE, `removeJobExecutions`, `pollRoundRobin`, `peek`, `clear`)
- Test: `backend/data/src/test/kotlin/io/tolgee/batch/BatchJobChunkExecutionQueueTest.kt`

**Interfaces:**
- Consumes: `ExecutionQueueItem.jobType` (Task 1).
- Produces: unchanged public method signatures (`pollRoundRobin`, `peek`, `removeJobExecutions`, `clear`, `addItemsToLocalQueue`, etc.) — only internal behavior changes.

- [ ] **Step 1: Add failing type-fairness tests (RED)**

In `BatchJobChunkExecutionQueueTest.kt` add:
```kotlin
  @Test
  fun `pollRoundRobin does not let many jobs of one type starve another type`() {
    // 100 QA_CHECK jobs, 1 chunk each
    val qaItems = (1L..100L).map { item(it, jobId = it, jobType = BatchJobType.QA_CHECK) }
    // 1 MACHINE_TRANSLATE job
    val mtItem = item(1000L, jobId = 1000L, jobType = BatchJobType.MACHINE_TRANSLATE)
    queue.addItemsToLocalQueue(qaItems + mtItem)

    // With per-job round-robin the MT chunk would appear ~position 101.
    // With type fairness, MT (one of 2 types) must be served within the first few polls.
    val firstFew = (1..4).mapNotNull { queue.pollRoundRobin()?.jobType }
    assertThat(firstFew).contains(BatchJobType.MACHINE_TRANSLATE)
  }

  @Test
  fun `pollRoundRobin rotates across types before serving a type twice`() {
    queue.addItemsToLocalQueue(
      listOf(
        item(1, jobId = 1, jobType = BatchJobType.QA_CHECK),
        item(2, jobId = 1, jobType = BatchJobType.QA_CHECK),
        item(3, jobId = 2, jobType = BatchJobType.MACHINE_TRANSLATE),
        item(4, jobId = 2, jobType = BatchJobType.MACHINE_TRANSLATE),
        item(5, jobId = 3, jobType = BatchJobType.AUTO_TRANSLATE),
        item(6, jobId = 3, jobType = BatchJobType.AUTO_TRANSLATE),
      ),
    )

    val types = (1..3).mapNotNull { queue.pollRoundRobin()?.jobType }
    // first three polls must hit three distinct types (one full type cycle)
    assertThat(types).containsExactlyInAnyOrder(
      BatchJobType.QA_CHECK,
      BatchJobType.MACHINE_TRANSLATE,
      BatchJobType.AUTO_TRANSLATE,
    )
  }

  @Test
  fun `pollRoundRobin keeps job fairness within a single type`() {
    // two jobs of the SAME type interleave (the #3428 guarantee, now scoped under a type)
    queue.addItemsToLocalQueue(
      listOf(
        item(1, jobId = 1, jobType = BatchJobType.QA_CHECK),
        item(2, jobId = 1, jobType = BatchJobType.QA_CHECK),
        item(3, jobId = 1, jobType = BatchJobType.QA_CHECK),
        item(4, jobId = 2, jobType = BatchJobType.QA_CHECK),
        item(5, jobId = 2, jobType = BatchJobType.QA_CHECK),
      ),
    )
    val jobs = (1..5).mapNotNull { queue.pollRoundRobin()?.jobId }
    // job2 (2 chunks) must not wait behind all of job1's chunks
    assertThat(jobs.indexOf(2L)).isLessThan(3)
  }
```

- [ ] **Step 2: Run — verify the starvation test FAILS**

Run: `./gradlew :data:test --tests "io.tolgee.batch.BatchJobChunkExecutionQueueTest" --console=plain`
Expected: `does not let many jobs of one type starve another type` FAILS (MT not in first 4 polls); the other two may pass or fail depending on insertion order. All must pass after Step 3.

- [ ] **Step 3: Replace the rotation structures and the methods that touch them**

In `BatchJobChunkExecutionQueue.kt` companion object, **replace** the `roundRobinOrder` declaration (lines ~50-56) with the two-level structures:
```kotlin
    /**
     * Two-level round-robin order. Outer level rotates job *types*; inner level rotates
     * jobs within a type. This makes scheduling fair across types regardless of how many
     * jobs of each type are queued — a flood of one type cannot starve another.
     *
     * jobsByType: type -> rotation of jobIds that currently have queued chunks of that type.
     * typeOrder:  rotation of types that currently have queued jobs.
     *
     * Invariants (eventually-consistent, self-correcting on poll like the job level):
     *   I1: jobId in jobsByType[type]  ↔ jobQueues[jobId] is non-null and non-empty
     *   I2: type  in typeOrder         ↔ jobsByType[type] has at least one jobId
     * A type/job that lingers after a concurrent drain is skipped on the next poll.
     * Both sets are [ConcurrentOrderedSet] so addLast() is idempotent under races.
     * Empty per-type sets are left in jobsByType (≤ number of BatchJobTypes) rather than
     * removed, to avoid a remove/computeIfAbsent race; typeOrder membership is what gates
     * scheduling.
     */
    private val jobsByType = ConcurrentHashMap<io.tolgee.batch.data.BatchJobType, ConcurrentOrderedSet<Long>>()
    private val typeOrder = ConcurrentOrderedSet<io.tolgee.batch.data.BatchJobType>()
```

**Replace** `addSingleItem` (lines ~90-106):
```kotlin
  private fun addSingleItem(item: ExecutionQueueItem): Boolean {
    if (!queuedExecutionIds.add(item.chunkExecutionId)) return false

    jobQueues.compute(item.jobId) { jobId, existing ->
      val deque =
        existing ?: ConcurrentLinkedDeque<ExecutionQueueItem>().also {
          // New job for this jobId — register it in its type's rotation (and the type in
          // typeOrder). Called atomically inside compute — only once per new job.
          // addLast is idempotent, so a concurrent re-add is a safe no-op.
          jobsByType.computeIfAbsent(item.jobType) { ConcurrentOrderedSet() }.addLast(jobId)
          typeOrder.addLast(item.jobType)
        }
      deque.addLast(item)
      incrementCharacterCount(item.jobCharacter)
      totalSize.incrementAndGet()
      deque
    }

    return true
  }
```

**Replace** the `QueueEventType.REMOVE` branch in `onJobItemEvent` (lines ~113-131):
```kotlin
      QueueEventType.REMOVE -> {
        event.items.forEach { item ->
          jobQueues.compute(item.jobId) { _, deque ->
            if (deque == null) return@compute null
            val removed = deque.removeIf { it.chunkExecutionId == item.chunkExecutionId }
            if (removed) {
              queuedExecutionIds.remove(item.chunkExecutionId)
              decrementCharacterCount(item.jobCharacter)
              totalSize.decrementAndGet()
            }
            if (deque.isEmpty()) {
              // Job drained — drop it from its type rotation. typeOrder self-corrects on poll.
              jobsByType[item.jobType]?.remove(item.jobId)
              null
            } else {
              deque
            }
          }
        }
      }
```

**Replace** `removeJobExecutions` (lines ~216-234):
```kotlin
  fun removeJobExecutions(jobId: Long) {
    logger.debug("Removing job $jobId from queue, queue size: ${totalSize.get()}")
    var removedCount = 0
    jobQueues.compute(jobId) { _, deque ->
      if (deque != null) {
        // All chunks of a job share its type; read it from any queued item.
        val type = deque.peek()?.jobType
        deque.forEach { item ->
          queuedExecutionIds.remove(item.chunkExecutionId)
          decrementCharacterCount(item.jobCharacter)
        }
        removedCount = deque.size
        totalSize.addAndGet(-removedCount)
        // Inside compute to prevent a concurrent addSingleItem from re-adding jobId between
        // the compute returning and the remove call. typeOrder self-corrects on next poll.
        if (type != null) jobsByType[type]?.remove(jobId)
      }
      null // remove entry from map
    }
    logger.debug("Removed job $jobId from queue ($removedCount items), queue size: ${totalSize.get()}")
  }
```

**Replace** `pollRoundRobin` (lines ~271-306) with the two-level version:
```kotlin
  /**
   * O(1) two-level round-robin poll: rotate types, then jobs within the chosen type, then
   * serve one chunk. Fair across types regardless of per-type job count (see #3428 for the
   * job level this builds on).
   *
   * Thread-safe: the chunk removal and all counters run inside jobQueues.compute(); the
   * rotation sets are ConcurrentOrderedSets with idempotent addLast(). A type or job that
   * drained concurrently is skipped; maxAttempts bounds the skip loop.
   */
  fun pollRoundRobin(): ExecutionQueueItem? {
    if (isEmpty()) return null

    // Each iteration removes one type from typeOrder. We may skip types whose bucket drained
    // concurrently (≤ number of types) and jobs that drained concurrently (≤ number of jobs).
    val maxAttempts = jobQueues.size + jobsByType.size + 1
    var attempts = 0
    while (attempts++ <= maxAttempts) {
      val type = typeOrder.pollFirst() ?: return null
      val jobsForType = jobsByType[type]
      val jobId = jobsForType?.pollFirst()
      if (jobId == null) {
        // Type bucket drained concurrently; type already removed from typeOrder. Try next.
        continue
      }

      var item: ExecutionQueueItem? = null
      jobQueues.compute(jobId) { _, deque ->
        if (deque.isNullOrEmpty()) return@compute null
        item = deque.removeFirst()
        val capturedItem = item!!
        queuedExecutionIds.remove(capturedItem.chunkExecutionId)
        decrementCharacterCount(capturedItem.jobCharacter)
        totalSize.decrementAndGet()
        if (deque.isNotEmpty()) {
          // Job still has chunks → keep it in its type's rotation (idempotent).
          jobsForType.addLast(jobId)
          deque
        } else {
          null
        }
      }

      // Re-queue the type if it still has any jobs waiting (idempotent; mirrors job level).
      if (jobsForType.peekFirst() != null) {
        typeOrder.addLast(type)
      }

      if (item != null) return item
      // else: job deque drained concurrently — try next type/job
    }
    return null
  }
```

**Replace** `clear` (lines ~308-315):
```kotlin
  fun clear() {
    logger.debug("Clearing queue")
    jobQueues.clear()
    jobsByType.clear()
    typeOrder.clear()
    queuedExecutionIds.clear()
    totalSize.set(0)
    jobCharacterCounts.clear()
  }
```

**Replace** `peek` (lines ~319-322):
```kotlin
  fun peek(): ExecutionQueueItem {
    val type = typeOrder.peekFirst() ?: throw NoSuchElementException("Queue is empty")
    val jobId = jobsByType[type]?.peekFirst() ?: throw NoSuchElementException("Queue is empty")
    return jobQueues[jobId]?.peekFirst() ?: throw NoSuchElementException("Queue is empty")
  }
```

Finally, update the now-stale KDoc on `jobQueues` (lines ~40-48) and `addSingleItem` (lines ~82-89) that still say "roundRobinOrder" — change those mentions to "jobsByType / typeOrder" so the comments don't rot. (No code change; comment text only.)

- [ ] **Step 4: Run the queue unit + performance tests — verify all PASS**

Run: `./gradlew :data:test --tests "io.tolgee.batch.BatchJobChunkExecutionQueueTest" --tests "io.tolgee.batch.BatchJobChunkExecutionQueuePerformanceTest" --console=plain`
Expected: PASS — the three new type-fairness tests pass; all pre-existing tests (single-type job round-robin, size/contains/remove/clear, perf O(1)) still pass.

- [ ] **Step 5: Commit**

```bash
git add backend/data/src/main/kotlin/io/tolgee/batch/BatchJobChunkExecutionQueue.kt \
        backend/data/src/test/kotlin/io/tolgee/batch/BatchJobChunkExecutionQueueTest.kt
git commit -m "fix: schedule batch chunks fairly across job types"
```

---

## Task 3: Tolerate cross-version queue-event payloads on Redis

During a rolling deploy, instances run mixed versions and exchange `JobQueueItemsEvent` over Redis. The new `jobType` field must not break deserialization. New→old is covered by the 60s `populateQueue()` re-read; this task hardens the new code so future field additions degrade gracefully too, and verifies the `NO_OP` default path.

**Files:**
- Modify: `backend/data/src/main/kotlin/io/tolgee/pubSub/RedisPubSubReceiver.kt`
- Test: `backend/data/src/test/kotlin/io/tolgee/pubSub/RedisPubSubReceiverTest.kt` (new)

**Interfaces:**
- Consumes: `ExecutionQueueItem.jobType` default `NO_OP` (Task 1); `JobQueueItemsEvent` (existing).

- [ ] **Step 1: Write the failing test (RED)**

Create `backend/data/src/test/kotlin/io/tolgee/pubSub/RedisPubSubReceiverTest.kt`:
```kotlin
package io.tolgee.pubSub

import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.events.JobQueueItemsEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.context.ApplicationEventPublisher
import org.springframework.messaging.simp.SimpMessagingTemplate

class RedisPubSubReceiverTest {
  @Test
  fun `deserializes a queue event missing jobType and ignores unknown fields`() {
    val publisher = mock<ApplicationEventPublisher>()
    val receiver = RedisPubSubReceiver(mock<SimpMessagingTemplate>(), publisher)

    // Payload as produced by an OLDER instance: no jobType, plus a hypothetical unknown field.
    val json =
      """{"items":[{"chunkExecutionId":1,"jobId":2,"executeAfter":null,""" +
        """"jobCharacter":"FAST","unknownFutureField":"x"}],"type":"ADD"}"""

    receiver.receiveJobQueueMessage(json)

    val captor = argumentCaptor<JobQueueItemsEvent>()
    verify(publisher).publishEvent(captor.capture())
    val item = captor.firstValue.items.single()
    assertThat(item.chunkExecutionId).isEqualTo(1)
    assertThat(item.jobType).isEqualTo(BatchJobType.NO_OP)
  }
}
```

- [ ] **Step 2: Run — verify it FAILS**

Run: `./gradlew :data:test --tests "io.tolgee.pubSub.RedisPubSubReceiverTest" --console=plain`
Expected: FAIL — `UnrecognizedPropertyException` on `unknownFutureField` (default mapper has `FAIL_ON_UNKNOWN_PROPERTIES=true`).

- [ ] **Step 3: Configure a lenient mapper for queue events**

In `RedisPubSubReceiver.kt`, add the import and a shared mapper, and use it in `receiveJobQueueMessage`:

Add import:
```kotlin
import com.fasterxml.jackson.databind.DeserializationFeature
```

Add a companion object and switch the read call:
```kotlin
  companion object {
    // Queue events are exchanged between mixed-version instances during rolling deploys.
    // Ignore unknown fields so an added field on a newer instance doesn't break an older
    // reader; missing fields fall back to ExecutionQueueItem defaults. The 60s
    // populateQueue() re-read corrects any transient grouping drift.
    private val jobQueueMapper =
      jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  }

  fun receiveJobQueueMessage(message: String) {
    val data = jobQueueMapper.readValue(message, JobQueueItemsEvent::class.java)
    applicationEventPublisher.publishEvent(data)
  }
```

(Leave `receiveWebsocketMessage` and `receiveJobCancel` untouched.)

- [ ] **Step 4: Run — verify it PASSES**

Run: `./gradlew :data:test --tests "io.tolgee.pubSub.RedisPubSubReceiverTest" --console=plain`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/data/src/main/kotlin/io/tolgee/pubSub/RedisPubSubReceiver.kt \
        backend/data/src/test/kotlin/io/tolgee/pubSub/RedisPubSubReceiverTest.kt
git commit -m "fix: tolerate cross-version batch queue events during rolling deploy"
```

---

## Task 4: Regression-verify the batch integration suite

The fix lives in the queue's poll logic, fully covered by the `:data` unit tests above. The Spring integration suite uses `runNoOpJob` (single `NO_OP` type), so it cannot cheaply exercise multi-type fairness — but it MUST still pass (single-type behavior is unchanged, and the queue API is unchanged).

**Files:** none (verification only).

- [ ] **Step 1: Run the round-robin + concurrency integration tests**

Run: `./gradlew :server-app:test --tests "io.tolgee.batch.BatchJobRoundRobinTest" --tests "io.tolgee.batch.BatchJobConcurrencyTest" --tests "io.tolgee.batch.BatchJobStressTest" --console=plain`
Expected: PASS. (If the test Postgres container shows schema errors, remove it with `docker rm -fv <container>` per the running-tests rule and re-run.)

- [ ] **Step 2: Run the broader batch suite**

Run: `./gradlew :server-app:test --tests "io.tolgee.batch.*" --console=plain`
Expected: PASS.

- [ ] **Step 3: No commit needed** (verification only). If a regression appears, return to systematic-debugging — do not patch blindly.

---

## Self-Review

**Spec coverage:**
- Two-level type→job rotation → Task 2 (structures, `pollRoundRobin`, `addSingleItem`, `removeJobExecutions`, REMOVE handler, `peek`, `clear`). ✓
- `ExecutionQueueItem.jobType` grouping key + populate-query `bk.type` → Task 1. ✓
- Atomicity invariants I1/I2 maintained inside `compute()` → Task 2 Step 3 (documented in code + plan constraints). ✓
- JobCharacter cap, project locking, debouncing unchanged → not touched by any task. ✓
- Redis `jobType` deploy-compatibility risk → Task 3 (default `NO_OP` + lenient mapper) and the 60s populateQueue recovery (documented). ✓
- Tests: type-starvation, type interleaving, per-job fairness within a type, single-type unchanged, perf O(1), cross-version deserialization → Tasks 1-3; integration regression → Task 4. ✓

**Placeholder scan:** No TBD/TODO/"handle edge cases"/"similar to". Every code step shows full code; every run step shows command + expected result. ✓

**Type consistency:** `jobType: BatchJobType` used identically across `ExecutionQueueItem`, `BatchJobChunkExecutionDto`, JPQL `bk.type`, both `toItem()`, `jobsByType` key, and all tests. Test helpers `item(...)`/`makeItem(...)` gain a `jobType` param with a default. `jobQueueMapper` name consistent in Task 3. ✓

**Known transient (documented, acceptable):** During a deploy, a queue event from an older instance deserializes its items with `jobType = NO_OP`; such a job is briefly grouped under `NO_OP` (and could momentarily occupy two type buckets if a correctly-typed item for the same job also arrives). No chunk is lost or duplicated (`queuedExecutionIds` dedupes), and `populateQueue()` re-reads correct types within 60s. This is strictly a sub-60s fairness approximation, not a correctness issue.

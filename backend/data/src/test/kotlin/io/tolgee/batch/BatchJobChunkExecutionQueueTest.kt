package io.tolgee.batch

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.tolgee.Metrics
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.ExecutionQueueItem
import io.tolgee.batch.data.QueueEventType
import io.tolgee.batch.events.JobQueueItemsEvent
import io.tolgee.component.UsingRedisProvider
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.data.redis.core.StringRedisTemplate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class BatchJobChunkExecutionQueueTest {
  private lateinit var queue: BatchJobChunkExecutionQueue

  @BeforeEach
  fun setup() {
    queue =
      BatchJobChunkExecutionQueue(
        entityManager = mock<EntityManager>(),
        usingRedisProvider = mock<UsingRedisProvider>(),
        redisTemplate = mock<StringRedisTemplate>(),
        metrics = Metrics(SimpleMeterRegistry()),
      )
    queue.clear()
  }

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

  // ── size / isEmpty ────────────────────────────────────────────────────────

  @Test
  fun `size and isEmpty reflect queue state`() {
    assertThat(queue.isEmpty()).isTrue()
    assertThat(queue.size).isEqualTo(0)

    queue.addItemsToLocalQueue(listOf(item(1, jobId = 1), item(2, jobId = 1)))

    assertThat(queue.isEmpty()).isFalse()
    assertThat(queue.size).isEqualTo(2)
  }

  // ── add / duplicate prevention ────────────────────────────────────────────

  @Test
  fun `duplicate items are not added twice`() {
    queue.addItemsToLocalQueue(listOf(item(1, jobId = 1), item(1, jobId = 1)))
    assertThat(queue.size).isEqualTo(1)
  }

  @Test
  fun `second addItemsToLocalQueue call does not re-add already queued items`() {
    queue.addItemsToLocalQueue(listOf(item(1, jobId = 1), item(2, jobId = 1)))
    queue.addItemsToLocalQueue(listOf(item(1, jobId = 1), item(3, jobId = 1)))

    assertThat(queue.size).isEqualTo(3)
    assertThat(queue.getAllQueueItems().map { it.chunkExecutionId }).containsExactlyInAnyOrder(1, 2, 3)
  }

  // ── pollRoundRobin ────────────────────────────────────────────────────────

  @Test
  fun `pollRoundRobin returns null on empty queue`() {
    assertThat(queue.pollRoundRobin()).isNull()
  }

  @Test
  fun `pollRoundRobin returns all items and empties the queue`() {
    queue.addItemsToLocalQueue(listOf(item(1, jobId = 1), item(2, jobId = 1), item(3, jobId = 1)))

    val polled = mutableListOf<Long>()
    repeat(3) { queue.pollRoundRobin()?.let { polled.add(it.chunkExecutionId) } }

    assertThat(polled).containsExactlyInAnyOrder(1, 2, 3)
    assertThat(queue.isEmpty()).isTrue()
    assertThat(queue.pollRoundRobin()).isNull()
  }

  @Test
  fun `pollRoundRobin alternates between jobs`() {
    // job1 has 3 items, job2 has 2 items — round-robin should interleave them
    queue.addItemsToLocalQueue(
      listOf(
        item(1, jobId = 1),
        item(2, jobId = 1),
        item(3, jobId = 1),
        item(4, jobId = 2),
        item(5, jobId = 2),
      ),
    )

    val polledJobs = (1..5).mapNotNull { queue.pollRoundRobin()?.jobId }

    // Each job must appear before it appears a second time (round-robin)
    val job1Positions = polledJobs.indices.filter { polledJobs[it] == 1L }
    val job2Positions = polledJobs.indices.filter { polledJobs[it] == 2L }

    // job1 and job2 should interleave: positions should alternate
    assertThat(job1Positions).hasSize(3)
    assertThat(job2Positions).hasSize(2)
    // No two consecutive items from the same job while the other still has items
    for (i in 0 until polledJobs.size - 1) {
      if (i < job2Positions.last()) {
        assertThat(polledJobs[i]).isNotEqualTo(polledJobs[i + 1])
      }
    }
  }

  @Test
  fun `pollRoundRobin preserves jobType on items`() {
    queue.addItemsToLocalQueue(listOf(item(1, jobId = 1, jobType = BatchJobType.MACHINE_TRANSLATE)))
    assertThat(queue.pollRoundRobin()?.jobType).isEqualTo(BatchJobType.MACHINE_TRANSLATE)
  }

  // ── size tracking after polls ─────────────────────────────────────────────

  @Test
  fun `size decrements correctly after each poll`() {
    queue.addItemsToLocalQueue(listOf(item(1, jobId = 1), item(2, jobId = 2), item(3, jobId = 3)))

    assertThat(queue.size).isEqualTo(3)
    queue.pollRoundRobin()
    assertThat(queue.size).isEqualTo(2)
    queue.pollRoundRobin()
    assertThat(queue.size).isEqualTo(1)
    queue.pollRoundRobin()
    assertThat(queue.size).isEqualTo(0)
  }

  // ── contains ─────────────────────────────────────────────────────────────

  @Test
  fun `contains returns true for queued item and false after it is polled`() {
    val it = item(42, jobId = 1)
    queue.addItemsToLocalQueue(listOf(it))

    assertThat(queue.contains(it)).isTrue()

    queue.pollRoundRobin()

    assertThat(queue.contains(it)).isFalse()
  }

  @Test
  fun `contains returns false for null`() {
    assertThat(queue.contains(null)).isFalse()
  }

  // ── removeJobExecutions ───────────────────────────────────────────────────

  @Test
  fun `removeJobExecutions removes all items for a job`() {
    queue.addItemsToLocalQueue(
      listOf(item(1, jobId = 1), item(2, jobId = 1), item(3, jobId = 2)),
    )

    queue.removeJobExecutions(jobId = 1)

    assertThat(queue.size).isEqualTo(1)
    assertThat(queue.getAllQueueItems().map { it.chunkExecutionId }).containsExactly(3)
  }

  @Test
  fun `removeJobExecutions on unknown jobId does nothing`() {
    queue.addItemsToLocalQueue(listOf(item(1, jobId = 1)))
    queue.removeJobExecutions(jobId = 99)
    assertThat(queue.size).isEqualTo(1)
  }

  @Test
  fun `removed job items can be re-added after removal`() {
    queue.addItemsToLocalQueue(listOf(item(1, jobId = 1)))
    queue.removeJobExecutions(jobId = 1)
    queue.addItemsToLocalQueue(listOf(item(1, jobId = 1)))

    assertThat(queue.size).isEqualTo(1)
    assertThat(queue.pollRoundRobin()?.chunkExecutionId).isEqualTo(1)
  }

  @Test
  fun `removeJobExecutions drops only the targeted job and leaves other types intact`() {
    queue.addItemsToLocalQueue(
      listOf(
        item(1, jobId = 1, jobType = BatchJobType.MACHINE_TRANSLATE),
        item(2, jobId = 1, jobType = BatchJobType.MACHINE_TRANSLATE),
        item(3, jobId = 2, jobType = BatchJobType.QA_CHECK),
        item(4, jobId = 3, jobType = BatchJobType.AUTO_TRANSLATE),
      ),
    )

    queue.removeJobExecutions(jobId = 1)

    assertThat(queue.size).isEqualTo(2)
    val polled = generateSequence { queue.pollRoundRobin() }.toList()
    assertThat(polled.map { it.chunkExecutionId }).containsExactlyInAnyOrder(3L, 4L)
    assertThat(polled.map { it.jobType }).containsExactlyInAnyOrder(
      BatchJobType.QA_CHECK,
      BatchJobType.AUTO_TRANSLATE,
    )
    assertThat(polled.map { it.jobType }).doesNotContain(BatchJobType.MACHINE_TRANSLATE)
    assertThat(queue.isEmpty()).isTrue()
  }

  // ── onJobItemEvent REMOVE ─────────────────────────────────────────────────

  @Test
  fun `REMOVE event removes specific items by chunkExecutionId`() {
    val i1 = item(1, jobId = 1)
    val i2 = item(2, jobId = 1)
    val i3 = item(3, jobId = 2)
    queue.addItemsToLocalQueue(listOf(i1, i2, i3))

    queue.onJobItemEvent(JobQueueItemsEvent(listOf(i1, i3), QueueEventType.REMOVE))

    assertThat(queue.size).isEqualTo(1)
    assertThat(queue.getAllQueueItems().map { it.chunkExecutionId }).containsExactly(2)
  }

  // ── clear ─────────────────────────────────────────────────────────────────

  @Test
  fun `clear empties the queue completely`() {
    queue.addItemsToLocalQueue(listOf(item(1, jobId = 1), item(2, jobId = 2)))
    queue.clear()

    assertThat(queue.isEmpty()).isTrue()
    assertThat(queue.size).isEqualTo(0)
    assertThat(queue.getAllQueueItems()).isEmpty()
  }

  @Test
  fun `items can be added again after clear`() {
    queue.addItemsToLocalQueue(listOf(item(1, jobId = 1)))
    queue.clear()
    queue.addItemsToLocalQueue(listOf(item(1, jobId = 1)))

    assertThat(queue.size).isEqualTo(1)
  }

  // ── getQueuedJobItems / getAllQueueItems ───────────────────────────────────

  @Test
  fun `getQueuedJobItems returns only items for that job`() {
    queue.addItemsToLocalQueue(
      listOf(item(1, jobId = 1), item(2, jobId = 1), item(3, jobId = 2)),
    )

    assertThat(queue.getQueuedJobItems(1).map { it.chunkExecutionId }).containsExactlyInAnyOrder(1, 2)
    assertThat(queue.getQueuedJobItems(2).map { it.chunkExecutionId }).containsExactly(3)
    assertThat(queue.getQueuedJobItems(99)).isEmpty()
  }

  @Test
  fun `getAllQueueItems returns all items across all jobs`() {
    queue.addItemsToLocalQueue(
      listOf(item(1, jobId = 1), item(2, jobId = 2), item(3, jobId = 3)),
    )

    assertThat(queue.getAllQueueItems().map { it.chunkExecutionId }).containsExactlyInAnyOrder(1, 2, 3)
  }

  // ── getJobCharacterCounts ─────────────────────────────────────────────────

  @Test
  fun `getJobCharacterCounts tracks counts correctly`() {
    queue.addItemsToLocalQueue(
      listOf(
        item(1, jobId = 1, character = JobCharacter.FAST),
        item(2, jobId = 1, character = JobCharacter.FAST),
        item(3, jobId = 2, character = JobCharacter.SLOW),
      ),
    )

    val counts = queue.getJobCharacterCounts()
    assertThat(counts[JobCharacter.FAST]).isEqualTo(2)
    assertThat(counts[JobCharacter.SLOW]).isEqualTo(1)

    queue.pollRoundRobin() // removes one FAST item
    assertThat(queue.getJobCharacterCounts()[JobCharacter.FAST]).isEqualTo(1)
  }

  // ── find ──────────────────────────────────────────────────────────────────

  @Test
  fun `find returns matching item or null`() {
    queue.addItemsToLocalQueue(listOf(item(1, jobId = 1), item(2, jobId = 2)))

    assertThat(queue.find { it.chunkExecutionId == 2L }?.chunkExecutionId).isEqualTo(2)
    assertThat(queue.find { it.chunkExecutionId == 99L }).isNull()
  }

  // ── type-level fairness ───────────────────────────────────────────────────

  @Test
  fun `pollRoundRobin does not let many jobs of one type starve another type`() {
    val qaItems = (1L..100L).map { item(it, jobId = it, jobType = BatchJobType.QA_CHECK) }
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

  // ── concurrency stress test ───────────────────────────────────────────────

  @Test
  fun `concurrent producers and consumers lose no items and serve each item exactly once`() {
    val types =
      listOf(
        BatchJobType.QA_CHECK,
        BatchJobType.MACHINE_TRANSLATE,
        BatchJobType.AUTO_TRANSLATE,
        BatchJobType.AUTOMATION,
      )
    val jobsPerType = 5
    val totalJobs = types.size * jobsPerType
    val totalItems = 20_000
    val itemsPerJob = totalItems / totalJobs

    // Build the full item list with globally unique chunkExecutionIds.
    data class Slot(
      val id: Long,
      val jobId: Long,
      val jobType: BatchJobType,
    )
    val allItems =
      (0 until totalJobs).flatMap { jobIndex ->
        val type = types[jobIndex / jobsPerType]
        val jobId = (jobIndex + 1).toLong()
        (0 until itemsPerJob).map { offset ->
          val chunkId = (jobIndex * itemsPerJob + offset + 1).toLong()
          Slot(chunkId, jobId, type)
        }
      }
    val allIds = allItems.map { it.id }.toSet()
    assertThat(allIds).hasSize(totalItems) // sanity: all ids unique

    // Partition items across producers; each partition is added by exactly one thread.
    val producerCount = 6
    val consumerCount = 6
    val batchSize = 50
    val partitions = allItems.chunked(allItems.size / producerCount + 1)

    val startGate = CountDownLatch(1)
    val producersDone = AtomicBoolean(false)
    val polledIds = ConcurrentHashMap.newKeySet<Long>()
    val polledCount = AtomicInteger(0)
    val deadlineMs = 20_000L

    val executor = Executors.newFixedThreadPool(producerCount + consumerCount)
    try {
      val producerFutures =
        partitions.mapIndexed { idx, partition ->
          executor.submit {
            startGate.await()
            partition.chunked(batchSize).forEach { batch ->
              queue.addItemsToLocalQueue(
                batch.map { s ->
                  item(s.id, s.jobId, jobType = s.jobType)
                },
              )
            }
          }
        }

      val consumerFutures =
        (0 until consumerCount).map {
          executor.submit {
            startGate.await()
            val deadline = System.currentTimeMillis() + deadlineMs
            while (true) {
              val polled = queue.pollRoundRobin()
              if (polled != null) {
                polledIds.add(polled.chunkExecutionId)
                polledCount.incrementAndGet()
              } else {
                // null from an eventually-consistent poll: only exit when producers are done
                // AND the queue is genuinely empty
                if (producersDone.get() && queue.isEmpty()) break
                if (System.currentTimeMillis() > deadline) {
                  error(
                    "Consumer deadline exceeded: polled=${ polledCount.get()}, expected=$totalItems, " +
                      "queueSize=${queue.size}, producersDone=${producersDone.get()}",
                  )
                }
                Thread.yield()
              }
            }
          }
        }

      startGate.countDown()

      producerFutures.forEach { it.get(30, TimeUnit.SECONDS) }
      producersDone.set(true)

      consumerFutures.forEach { it.get(30, TimeUnit.SECONDS) }
    } finally {
      executor.shutdownNow()
    }

    // ── assertions ────────────────────────────────────────────────────────────
    assertThat(polledCount.get())
      .withFailMessage("Expected $totalItems polled items but got ${polledCount.get()} (items lost or counted wrong)")
      .isEqualTo(totalItems)

    assertThat(polledIds.size)
      .withFailMessage("${polledIds.size} distinct ids polled — expected $totalItems (duplicate serves detected)")
      .isEqualTo(totalItems)

    assertThat(polledIds)
      .withFailMessage("Polled id set differs from added id set")
      .isEqualTo(allIds)

    assertThat(queue.size)
      .withFailMessage("Queue size is ${queue.size} after draining — expected 0")
      .isEqualTo(0)

    assertThat(queue.isEmpty())
      .withFailMessage("queue.isEmpty() returned false after draining all items")
      .isTrue()

    val positiveCharacterCounts =
      queue.getJobCharacterCounts().filter { (_, v) -> v > 0 }
    assertThat(positiveCharacterCounts)
      .withFailMessage("Non-zero character counts remain after full drain: $positiveCharacterCounts")
      .isEmpty()
  }
}

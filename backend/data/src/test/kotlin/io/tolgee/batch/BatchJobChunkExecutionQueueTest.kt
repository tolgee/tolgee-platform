package io.tolgee.batch

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.tolgee.Metrics
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
  ) = ExecutionQueueItem(chunkExecutionId = id, jobId = jobId, executeAfter = null, jobCharacter = character)

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
}

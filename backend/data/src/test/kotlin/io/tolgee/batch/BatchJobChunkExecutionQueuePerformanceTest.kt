package io.tolgee.batch

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.tolgee.Metrics
import io.tolgee.batch.data.ExecutionQueueItem
import io.tolgee.component.UsingRedisProvider
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD
import org.mockito.kotlin.mock
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import java.util.concurrent.TimeUnit
import kotlin.time.measureTime

/**
 * Performance regression tests for BatchJobChunkExecutionQueue.
 *
 * These tests prove that pollRoundRobin() is O(1) per call thanks to the
 * per-job indexed structure (Map<JobId, ArrayDeque> + ConcurrentLinkedDeque
 * for round-robin order). With 110k items, polls must remain fast.
 *
 * If these tests start failing (i.e. polling becomes slow again), it means
 * an O(n) scan was re-introduced in the hot path.
 */
class BatchJobChunkExecutionQueuePerformanceTest {
  private val logger = LoggerFactory.getLogger(BatchJobChunkExecutionQueuePerformanceTest::class.java)
  private lateinit var executionQueue: BatchJobChunkExecutionQueue

  @BeforeEach
  fun setup() {
    executionQueue =
      BatchJobChunkExecutionQueue(
        entityManager = mock<EntityManager>(),
        usingRedisProvider = mock<UsingRedisProvider>(),
        redisTemplate = mock<StringRedisTemplate>(),
        metrics = Metrics(SimpleMeterRegistry()),
      )
    // The internal structures live in the companion object (static), clear between tests
    executionQueue.clear()
  }

  private fun makeItem(
    id: Long,
    jobId: Long,
  ) = ExecutionQueueItem(
    chunkExecutionId = id,
    jobId = jobId,
    executeAfter = null,
    jobCharacter = JobCharacter.FAST,
  )

  /**
   * 1 large job with 110k chunks.
   * With the old O(n) implementation, 1000 polls took several seconds.
   * With the new O(1) structure, they must complete in under 500ms.
   */
  @Test
  fun `pollRoundRobin is fast with 1 large job and 110k items`() {
    val items = (1L..110_000L).map { makeItem(it, jobId = 1L) }
    executionQueue.addItemsToLocalQueue(items)

    val polls = 1_000
    val elapsed = measureTime { repeat(polls) { executionQueue.pollRoundRobin() } }
    val elapsedMs = elapsed.inWholeMilliseconds

    logger.info(
      "1 job × 110k items | $polls polls → ${elapsedMs}ms (~${"%.2f".format(elapsedMs.toDouble() / polls)}ms/poll)",
    )

    assertThat(elapsedMs)
      .withFailMessage("$polls polls with 110k items took ${elapsedMs}ms — O(n) scan may have been re-introduced")
      .isLessThan(500)
  }

  /**
   * 110k jobs with 1 chunk each.
   * With the old implementation each poll scanned 110k nodes.
   * With the new structure, each poll is O(1) regardless of job count.
   */
  @Test
  fun `pollRoundRobin is fast with 110k jobs of 1 item each`() {
    val items = (1L..110_000L).map { makeItem(it, jobId = it) }
    executionQueue.addItemsToLocalQueue(items)

    val polls = 1_000
    val elapsed = measureTime { repeat(polls) { executionQueue.pollRoundRobin() } }
    val elapsedMs = elapsed.inWholeMilliseconds

    logger.info(
      "110k jobs × 1 item | $polls polls → ${elapsedMs}ms (~${"%.2f".format(elapsedMs.toDouble() / polls)}ms/poll)",
    )

    assertThat(elapsedMs)
      .withFailMessage("$polls polls with 110k jobs took ${elapsedMs}ms — O(n) scan may have been re-introduced")
      .isLessThan(500)
  }

  // ──────────────────────────────────────────────────────────────────────────
  // removeJobExecutions
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * removeJobExecutions(jobId) should run in O(k) where k is the number of items
   * for that job, not O(total queue size).
   * We load 110k items across 100 jobs and call removeJobExecutions 1000 times
   * (the job is already empty after the first call, so subsequent calls are O(1)).
   */
  @Test
  fun `removeJobExecutions is fast with large queue`() {
    val items = (1L..110_000L).map { makeItem(it, jobId = it % 100 + 1) }
    executionQueue.addItemsToLocalQueue(items)

    val elapsed = measureTime { repeat(1_000) { executionQueue.removeJobExecutions(1L) } }
    val elapsedMs = elapsed.inWholeMilliseconds

    logger.info("removeJobExecutions × 1000 with 110k-item queue → ${elapsedMs}ms")

    assertThat(elapsedMs)
      .withFailMessage("removeJobExecutions × 1000 took ${elapsedMs}ms — O(total) scan may have been re-introduced")
      .isLessThan(500)
  }

  // ──────────────────────────────────────────────────────────────────────────
  // contains / isEmpty / size — O(1) accessors
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * contains() must be O(1) — backed by a ConcurrentHashMap set, not a linear scan.
   */
  @Test
  fun `contains is O(1) with large queue`() {
    val items = (1L..110_000L).map { makeItem(it, jobId = it % 100 + 1) }
    executionQueue.addItemsToLocalQueue(items)
    val target = items[55_000]

    val checks = 100_000
    val elapsed = measureTime { repeat(checks) { executionQueue.contains(target) } }
    val elapsedMs = elapsed.inWholeMilliseconds

    logger.info("contains × $checks with 110k-item queue → ${elapsedMs}ms")

    assertThat(elapsedMs)
      .withFailMessage("contains × $checks took ${elapsedMs}ms — O(n) scan may have been re-introduced")
      .isLessThan(200)
  }

  /**
   * isEmpty() and size must be O(1) — backed by an AtomicInteger counter.
   */
  @Test
  @Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = SEPARATE_THREAD)
  fun `isEmpty and size are O(1) with large queue`() {
    val items = (1L..110_000L).map { makeItem(it, jobId = it % 100 + 1) }
    executionQueue.addItemsToLocalQueue(items)

    val checks = 1_000_000
    val elapsed =
      measureTime {
        repeat(checks) {
          executionQueue.isEmpty()
          executionQueue.size
        }
      }
    val elapsedMs = elapsed.inWholeMilliseconds

    logger.info("isEmpty+size × $checks with 110k-item queue → ${elapsedMs}ms")

    assertThat(elapsedMs)
      .withFailMessage("isEmpty/size × $checks took ${elapsedMs}ms — O(n) traversal may have been re-introduced")
      .isLessThan(500)
  }

  // ──────────────────────────────────────────────────────────────────────────
  // pollRoundRobin — existing coverage kept below
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Verifies that poll time does NOT scale with queue size (O(1) behaviour).
   * The ratio between 110k and 1k should be small (< 3x), not ~110x as with O(n).
   */
  @Test
  fun `pollRoundRobin throughput does not degrade with queue size`() {
    val pollsPerSize = 200
    val results = linkedMapOf<Int, Long>()

    for (queueSize in listOf(1_000, 10_000, 110_000)) {
      executionQueue.clear()
      val items = (1L..queueSize.toLong()).map { makeItem(it, jobId = it % 50 + 1) }
      executionQueue.addItemsToLocalQueue(items)

      val elapsed = measureTime { repeat(pollsPerSize) { executionQueue.pollRoundRobin() } }
      results[queueSize] = elapsed.inWholeMilliseconds
    }

    val baseline = maxOf(results[1_000]!!, 1L)
    val ratio110k = results[110_000]!! / baseline

    logger.info("Queue 1k:   ${results[1_000]}ms for $pollsPerSize polls")
    logger.info("Queue 10k:  ${results[10_000]}ms for $pollsPerSize polls")
    logger.info("Queue 110k: ${results[110_000]}ms for $pollsPerSize polls (${ratio110k}x vs 1k)")

    // With O(1) behaviour, 110k should be at most 3x slower than 1k (JIT warm-up noise)
    // With the old O(n) scan, this ratio was ~110x
    assertThat(ratio110k)
      .withFailMessage("110k queue is ${ratio110k}x slower than 1k — O(n) scan may have been re-introduced")
      .isLessThan(3)
  }
}

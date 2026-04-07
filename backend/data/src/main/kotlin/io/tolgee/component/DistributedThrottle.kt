package io.tolgee.component

import org.redisson.api.RedissonClient
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Ensures a block of work runs at most once per [interval] across all application nodes.
 *
 * When Redis is available, the last-run timestamp is stored in a shared Redis key,
 * so multiple pods coordinate and avoid redundant work. When Redis is not configured
 * (single-node / local dev), an in-memory timestamp is used instead.
 *
 * Usage:
 * ```
 * @Scheduled(fixedRate = 30_000)  // Spring wakes us often…
 * fun cleanup() {
 *   distributedThrottle.runAtMostEvery("my-cleanup", Duration.ofMinutes(10)) {
 *     // …but the expensive work only runs once per 10 min across all pods
 *     doExpensiveCleanup()
 *   }
 * }
 * ```
 *
 * The Spring `@Scheduled` rate should be shorter than the throttle interval so that
 * every node gets a chance to check; the throttle guarantees only one execution happens.
 */
@Component
class DistributedThrottle(
  private val usingRedisProvider: UsingRedisProvider,
  @Lazy
  private val redissonClient: RedissonClient,
) {
  private val localTimestamps = ConcurrentHashMap<String, Long>()

  /**
   * Executes [block] only if at least [interval] has elapsed since the last execution
   * (across all nodes when Redis is available, locally otherwise).
   *
   * This is not locked — if two nodes check at the exact same millisecond, both may run.
   * This is acceptable for distributed coordination or background cleanup where occasional
   * double-execution is harmless but skipping for the full interval is the goal.
   */
  fun runAtMostEvery(
    name: String,
    interval: Duration,
    block: () -> Unit,
  ) {
    val now = System.currentTimeMillis()
    val intervalMs = interval.toMillis()

    if (usingRedisProvider.areWeUsingRedis) {
      runWithRedis(name, now, intervalMs, block)
    } else {
      runLocal(name, now, intervalMs, block)
    }
  }

  private fun runWithRedis(
    name: String,
    now: Long,
    intervalMs: Long,
    block: () -> Unit,
  ) {
    val key = "distributed_throttle:$name"
    val bucket = redissonClient.getAtomicLong(key)
    val lastRun = bucket.get()
    if (now - lastRun < intervalMs) {
      return
    }
    // CAS to avoid races: only proceed if nobody else updated it since we read
    if (lastRun == 0L) {
      // First run ever — set unconditionally (getAndSet returns old value)
      bucket.set(now)
    } else if (!bucket.compareAndSet(lastRun, now)) {
      // Another node won the race — skip this run
      return
    }
    block()
  }

  private fun runLocal(
    name: String,
    now: Long,
    intervalMs: Long,
    block: () -> Unit,
  ) {
    var shouldRun = false
    localTimestamps.compute(name) { _, previous ->
      val lastRun = previous ?: 0L
      if (now - lastRun >= intervalMs) {
        shouldRun = true
        now
      } else {
        lastRun
      }
    }
    if (shouldRun) {
      block()
    }
  }
}

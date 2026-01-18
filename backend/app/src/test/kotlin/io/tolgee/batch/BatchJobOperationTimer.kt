package io.tolgee.batch

import io.tolgee.util.Logging
import io.tolgee.util.logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Component to measure time spent in various batch job operations.
 * Used for profiling and identifying bottlenecks.
 */
class BatchJobOperationTimer : Logging {

  private val operationTimes = ConcurrentHashMap<String, OperationStats>()

  data class OperationStats(
    val totalTimeNanos: AtomicLong = AtomicLong(0),
    val count: AtomicLong = AtomicLong(0),
    val maxTimeNanos: AtomicLong = AtomicLong(0),
    val minTimeNanos: AtomicLong = AtomicLong(Long.MAX_VALUE),
  )

  fun <T> measure(operationName: String, block: () -> T): T {
    val startTime = System.nanoTime()
    try {
      return block()
    } finally {
      val elapsed = System.nanoTime() - startTime
      record(operationName, elapsed)
    }
  }

  suspend fun <T> measureSuspend(operationName: String, block: suspend () -> T): T {
    val startTime = System.nanoTime()
    try {
      return block()
    } finally {
      val elapsed = System.nanoTime() - startTime
      record(operationName, elapsed)
    }
  }

  private fun record(operationName: String, elapsedNanos: Long) {
    val stats = operationTimes.computeIfAbsent(operationName) { OperationStats() }
    stats.totalTimeNanos.addAndGet(elapsedNanos)
    stats.count.incrementAndGet()
    stats.maxTimeNanos.updateAndGet { maxOf(it, elapsedNanos) }
    stats.minTimeNanos.updateAndGet { minOf(it, elapsedNanos) }
  }

  fun reset() {
    operationTimes.clear()
  }

  fun printReport() {
    logger.info("=".repeat(100))
    logger.info("BATCH JOB OPERATION TIMING REPORT")
    logger.info("=".repeat(100))
    logger.info(
      String.format(
        "%-50s %10s %12s %12s %12s %12s",
        "Operation",
        "Count",
        "Total(ms)",
        "Avg(ms)",
        "Min(ms)",
        "Max(ms)",
      ),
    )
    logger.info("-".repeat(100))

    val sortedOperations =
      operationTimes.entries
        .sortedByDescending { it.value.totalTimeNanos.get() }

    for ((name, stats) in sortedOperations) {
      val count = stats.count.get()
      val totalMs = stats.totalTimeNanos.get() / 1_000_000.0
      val avgMs = if (count > 0) totalMs / count else 0.0
      val minMs = if (stats.minTimeNanos.get() == Long.MAX_VALUE) 0.0 else stats.minTimeNanos.get() / 1_000_000.0
      val maxMs = stats.maxTimeNanos.get() / 1_000_000.0

      logger.info(
        String.format(
          "%-50s %10d %12.2f %12.2f %12.2f %12.2f",
          name,
          count,
          totalMs,
          avgMs,
          minMs,
          maxMs,
        ),
      )
    }

    logger.info("=".repeat(100))

    // Summary
    val totalTimeMs = operationTimes.values.sumOf { it.totalTimeNanos.get() } / 1_000_000.0
    val totalOps = operationTimes.values.sumOf { it.count.get() }
    logger.info("Total operations: $totalOps")
    logger.info("Total measured time: ${"%.2f".format(totalTimeMs)}ms")
    logger.info("=".repeat(100))
  }

  fun getStats(): Map<String, OperationStats> = operationTimes.toMap()
}

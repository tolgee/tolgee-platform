package io.tolgee.component

import io.tolgee.util.Logging
import io.tolgee.util.logger
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture

/**
 * Manages scheduled tasks using manual scheduling instead of @Scheduled annotation.
 *
 * This approach is used due to Spring's context caching in tests, which would make
 * scheduled tasks run multiple times across test executions. By managing scheduling
 * manually, we can cancel tasks when needed and only run them in required tests.
 */
@Component
class SchedulingManager(
  private val taskScheduler: org.springframework.scheduling.TaskScheduler,
) : Logging {
  companion object {
    private val scheduledTasks = ConcurrentHashMap<String, ScheduledFuture<*>>()

    fun cancelAll() {
      // Create a thread-safe copy of the keys
      val idsToCancel = ArrayList(scheduledTasks.keys)
      logger().debug("Cancelling all ${idsToCancel.size} scheduled tasks")
      idsToCancel.forEach { cancelTask(it) }
    }

    fun cancelTask(id: String) {
      scheduledTasks[id]?.cancel(true)
      scheduledTasks.remove(id)
    }
  }

  fun scheduleWithFixedDelay(
    runnable: Runnable,
    period: Duration,
  ): String {
    val future = taskScheduler.scheduleWithFixedDelay(runnable, period)
    val id = UUID.randomUUID().toString()
    scheduledTasks[id] = future
    return id
  }

  @PreDestroy
  fun cancelAll() {
    Companion.cancelAll()
  }
}

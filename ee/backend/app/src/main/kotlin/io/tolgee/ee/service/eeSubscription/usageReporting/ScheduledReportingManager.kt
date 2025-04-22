package io.tolgee.ee.service.eeSubscription.usageReporting

import io.tolgee.ee.EeProperties
import io.tolgee.util.Logging
import io.tolgee.util.logger
import jakarta.annotation.PreDestroy
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture

/**
 * Manages scheduled reporting tasks using manual scheduling instead of @Scheduled annotation.
 *
 * This approach is used due to Spring's context caching in tests, which would make
 * scheduled tasks run multiple times across test executions. By managing scheduling
 * manually, we can cancel tasks when needed and only run them in required tests.
 */
@Component
class ScheduledReportingManager(
  private val taskScheduler: TaskScheduler,
  private val eeProperties: EeProperties,
  private val reportingService: UsageReportingService,
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

  // Store the ID of the task scheduled at startup for potential cancellation
  private var startupTaskId: String? = null

  @EventListener(ApplicationReadyEvent::class)
  fun scheduleReporting() {
    if (!eeProperties.scheduledReportingEnabled) {
      logger.info("Scheduled reporting is disabled, skipping scheduling")
      return
    }
    startupTaskId = scheduleTask()
    logger.debug("Scheduled reporting task with ID: $startupTaskId")
  }

  fun scheduleTask(): String {
    val period = Duration.ofMillis(eeProperties.reportUsageFixedDelayInMs)
    val runnable = {
      logger.debug("Reporting usage periodically")
      reportingService.reportIfNeeded()
    }
    val future = taskScheduler.scheduleAtFixedRate(runnable, period)
    val id = UUID.randomUUID().toString()
    scheduledTasks[id] = future
    return id
  }

  @PreDestroy
  fun cancelAll() {
    Companion.cancelAll()
  }
}

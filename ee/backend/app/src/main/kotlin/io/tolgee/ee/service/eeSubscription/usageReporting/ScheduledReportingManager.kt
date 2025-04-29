package io.tolgee.ee.service.eeSubscription.usageReporting

import io.tolgee.component.SchedulingManager
import io.tolgee.ee.EeProperties
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Manages scheduled reporting tasks using manual scheduling instead of @Scheduled annotation.
 *
 * This approach is used due to Spring's context caching in tests, which would make
 * scheduled tasks run multiple times across test executions. By managing scheduling
 * manually, we can cancel tasks when needed and only run them in required tests.
 */
@Component
class ScheduledReportingManager(
  private val eeProperties: EeProperties,
  private val reportingService: UsageReportingService,
  private val schedulingManager: SchedulingManager,
) : Logging {
  @EventListener(ApplicationReadyEvent::class)
  fun scheduleReporting() {
    if (!eeProperties.scheduledReportingEnabled) {
      logger.info("Scheduled reporting is disabled, skipping scheduling")
      return
    }
    scheduleTask()
    logger.debug("Scheduled reporting task")
  }

  fun scheduleTask() {
    val period = Duration.ofMillis(eeProperties.reportUsageFixedDelayInMs)
    val runnable = {
      logger.debug("Reporting usage periodically")
      reportingService.reportIfNeeded()
    }

    schedulingManager.scheduleWithFixedDelay(runnable, period)
  }
}

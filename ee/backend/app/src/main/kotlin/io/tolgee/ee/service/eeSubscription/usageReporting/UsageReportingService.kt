package io.tolgee.ee.service.eeSubscription.usageReporting

import io.tolgee.api.EeSubscriptionDto
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.LockingProvider
import io.tolgee.ee.data.usageReporting.UsageToReportDto
import io.tolgee.ee.service.eeSubscription.EeSubscriptionErrorCatchingService
import io.tolgee.ee.service.eeSubscription.EeSubscriptionServiceImpl
import io.tolgee.ee.service.eeSubscription.cloudClient.TolgeeCloudLicencingClient
import io.tolgee.util.Logging
import io.tolgee.util.addSeconds
import io.tolgee.util.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UsageReportingService(
  private val catchingService: EeSubscriptionErrorCatchingService,
  private val client: TolgeeCloudLicencingClient,
  private val usageToReportService: UsageToReportService,
  private val currentDateProvider: CurrentDateProvider,
  private val eeSubscriptionServiceImpl: EeSubscriptionServiceImpl,
  private val lockingProvider: LockingProvider,
) : Logging {
  /**
   * Reports usage to Tolgee Cloud with a 1-minute deferral mechanism.
   *
   * If a report was sent less than 1 minute ago, the usage data is stored locally
   * instead of being sent immediately. This prevents excessive API calls when
   * multiple actions occur in quick succession.
   *
   * @param subscription The current subscription information
   * @param keys The number of keys to report, or null if unchanged
   * @param seats The number of seats to report, or null if unchanged
   */
  fun reportUsage(
    subscription: EeSubscriptionDto?,
    keys: Long? = null,
    seats: Long? = null,
  ) {
    if (isReportingTooSoon()) {
      usageToReportService.storeCurrentUsage(keys = keys, seats = seats)
      return
    }

    reportAndStore(subscription, keys, seats)
  }

  /**
   * Periodically reports usage data if needed.
   *
   * This method is called by the ScheduledReportingManager's scheduled task.
   * It uses manual scheduling instead of @Scheduled to avoid issues with
   * Spring's context caching in tests, allowing for better control over
   * when reporting occurs during test execution.
   */
  @Transactional
  fun reportIfNeeded() {
    try {
      lockingProvider.withLocking("report_usage_periodically") {
        val subscription = eeSubscriptionServiceImpl.findSubscriptionDto() ?: return@withLocking
        val usageToReport = usageToReportService.findOrCreateUsageToReport()

        if (isReportingTooSoon(usageToReport)) {
          return@withLocking
        }

        if (
          usageToReport.keysToReport != usageToReport.lastReportedKeys ||
          usageToReport.seatsToReport != usageToReport.lastReportedSeats
        ) {
          reportAndStore(subscription, usageToReport.keysToReport, usageToReport.seatsToReport)
        }
      }
    } catch (e: Exception) {
      // Log the exception but don't rethrow to prevent task scheduling from being disrupted
      logger.error("Error while reporting usage", e)
    }
  }

  /**
   * Reports usage to Tolgee Cloud and updates the local storage.
   *
   * This method handles the actual reporting process when it's determined that
   * a report should be sent (i.e., when the 1-minute deferral period has passed).
   * It sends the report to Tolgee Cloud and then updates the local storage with
   * the reported data and current timestamp.
   *
   * @param subscription The current subscription information
   * @param keys The number of keys to report, or null if unchanged
   * @param seats The number of seats to report, or null if unchanged
   */
  private fun reportAndStore(
    subscription: EeSubscriptionDto?,
    keys: Long?,
    seats: Long?,
  ) {
    reportUsageRemote(subscription, keys, seats)
    usageToReportService.storeOnReport(keys = keys, seats = seats)
  }

  private fun reportUsageRemote(
    subscription: EeSubscriptionDto?,
    keys: Long?,
    seats: Long?,
  ) {
    if (subscription != null) {
      catchingService.catchingSpendingLimits {
        catchingService.catchingLicenseNotFound {
          client.reportUsageRemote(subscription = subscription, keys = keys, seats = seats)
        }
      }
    }
  }

  /**
   * Determines if a usage report was sent too recently (less than 1 minute ago).
   *
   * This method is a key part of the deferral mechanism. It checks if the last report
   * was sent less than 60 seconds ago, in which case we defer sending a new report
   * to avoid excessive API calls.
   *
   * @return true if the last report was sent less than 60 seconds ago, false otherwise
   */
  private fun isReportingTooSoon(): Boolean {
    val usageToReport = usageToReportService.findOrCreateUsageToReport()
    return isReportingTooSoon(usageToReport)
  }

  private fun isReportingTooSoon(usageToReport: UsageToReportDto): Boolean {
    val minDateToReport = usageToReport.reportedAt.addSeconds(60)

    return minDateToReport.after(currentDateProvider.date)
  }
}

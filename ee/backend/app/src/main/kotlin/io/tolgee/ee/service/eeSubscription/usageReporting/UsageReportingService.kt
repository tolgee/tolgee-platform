package io.tolgee.ee.service.eeSubscription.usageReporting

import io.tolgee.api.EeSubscriptionDto
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.LockingProvider
import io.tolgee.ee.EeProperties
import io.tolgee.ee.component.limitsAndReporting.SelfHostedLimitsProvider
import io.tolgee.ee.data.usageReporting.UsageToReportDto
import io.tolgee.ee.service.eeSubscription.EeSubscriptionErrorCatchingService
import io.tolgee.ee.service.eeSubscription.EeSubscriptionServiceImpl
import io.tolgee.ee.service.eeSubscription.cloudClient.TolgeeCloudLicencingClient
import io.tolgee.exceptions.BadRequestException
import io.tolgee.service.organization.OrganizationStatsService
import io.tolgee.util.Logging
import io.tolgee.util.addSeconds
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException

@Service
class UsageReportingService(
  private val catchingService: EeSubscriptionErrorCatchingService,
  private val client: TolgeeCloudLicencingClient,
  private val usageToReportService: UsageToReportService,
  private val currentDateProvider: CurrentDateProvider,
  private val eeSubscriptionServiceImpl: EeSubscriptionServiceImpl,
  private val lockingProvider: LockingProvider,
  private val organizationStatsService: OrganizationStatsService,
  private val selfHostedLimitsProvider: SelfHostedLimitsProvider,
  private val eeProperties: EeProperties,
  private val transactionManager: PlatformTransactionManager,
) : Logging {
  /**
   * The last word figure the licence server refused, so it is not re-sent (and re-refused) on every
   * tick. Deliberately in memory and not persisted: it is a back-off, and a restart re-testing the
   * figure once is the harmless outcome. Cleared as soon as any report is accepted.
   */
  @Volatile
  private var rejectedWordFigure: Long? = null

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

    reportAndStore(subscription, keys, seats, words = null)
  }

  fun markWordUsageChanged() {
    usageToReportService.markWordsDirty()
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

        val wordsToReport = refreshWordsToReport(usageToReport)
        val keysAndSeatsDue = !isReportingTooSoon(usageToReport)

        val keys = usageToReport.keysToReport.takeIf { keysAndSeatsDue && it != usageToReport.lastReportedKeys }
        val seats = usageToReport.seatsToReport.takeIf { keysAndSeatsDue && it != usageToReport.lastReportedSeats }
        val words =
          wordsToReport?.takeIf { it != usageToReport.lastReportedWords && it != rejectedWordFigure }

        if (keys != null || seats != null || words != null) {
          reportAndStore(subscription, keys, seats, words)
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
   * @param words The number of words to report, or null if unchanged
   */
  private fun reportAndStore(
    subscription: EeSubscriptionDto?,
    keys: Long?,
    seats: Long?,
    words: Long?,
  ) {
    try {
      reportUsageRemote(subscription, keys, seats, words)
      rejectedWordFigure = null
      usageToReportService.storeOnReport(keys = keys, seats = seats, words = words)
      return
    } catch (e: Exception) {
      if (words == null || !isPayloadRejection(e)) {
        throw e
      }
      // Keys and seats travel in the same POST but have nothing to do with a refused word figure,
      // and leaving them unstored would freeze their billing too, indefinitely, since the same
      // payload would then be re-sent and re-refused every cycle.
      rejectedWordFigure = words
      logger.error("Licence server refused the reported word count ($words); reporting keys and seats without it", e)
    }

    if (keys == null && seats == null) {
      return
    }
    reportUsageRemote(subscription, keys, seats, words = null)
    usageToReportService.storeOnReport(keys = keys, seats = seats, words = null)
  }

  private fun isPayloadRejection(e: Exception): Boolean =
    e is BadRequestException || e is HttpClientErrorException.BadRequest

  private fun reportUsageRemote(
    subscription: EeSubscriptionDto?,
    keys: Long?,
    seats: Long?,
    words: Long?,
  ) {
    if (subscription != null) {
      catchingService.catchingSpendingLimits {
        catchingService.catchingLicenseNotFound {
          client.reportUsageRemote(subscription = subscription, keys = keys, seats = seats, words = words)
        }
      }
    }
  }

  /**
   * The one place that pays for the word aggregation. Everything upstream only raises the flag.
   * Null when this licence does not meter words — the licence server reads a figure as a real
   * measurement and would record a zero the instance never claimed — or when the word window is
   * still closed.
   */
  private fun refreshWordsToReport(usageToReport: UsageToReportDto): Long? {
    if (!selfHostedLimitsProvider.getLimits().metersWords) {
      return null
    }
    // A figure already counted but not yet accepted by the licence server is retried every tick;
    // only the count itself is throttled.
    val recountAllowedAt = usageToReport.wordsCountedAt.time + eeProperties.reportWordsMinIntervalInMs
    if (recountAllowedAt > currentDateProvider.date.time) {
      return usageToReport.wordsToReport
    }
    return try {
      recountWords(usageToReport)
    } catch (e: Throwable) {
      // Throwable, not Exception: takeWordsDirty has already committed the flag clear by the time
      // the count runs, so an Error escaping it (this path produced a StackOverflowError once
      // during development) would otherwise lose the recount until the next content change.
      // Keys and seats have to be reported anyway: an aggregation that keeps failing would
      // otherwise halt reporting for two metrics that have nothing to do with words.
      logger.error("Failed to refresh the instance word count, reporting the last stored figure", e)
      // Without putting the flag back, a failure after it was taken loses both the flag and the
      // fresh figure, and nothing recounts until the next content change.
      runCatching { usageToReportService.markWordsDirtyInNewTransaction() }
        .onFailure { logger.error("Failed to restore the words-dirty flag", it) }
      usageToReport.wordsToReport
    }
  }

  private fun recountWords(usageToReport: UsageToReportDto): Long {
    if (!usageToReportService.takeWordsDirty()) {
      return usageToReport.wordsToReport
    }
    // In its own transaction: a failed aggregation must not leave the reporting transaction
    // rollback-only, or the keys and seats figures in the same run go down with it.
    val words =
      executeInNewTransaction(transactionManager) {
        organizationStatsService.countAllWordsOnInstance()
      }
    usageToReportService.storeRecountedWords(words)
    return words
  }

  private fun isReportingTooSoon(): Boolean {
    val usageToReport = usageToReportService.findOrCreateUsageToReport()
    return isReportingTooSoon(usageToReport)
  }

  private fun isReportingTooSoon(usageToReport: UsageToReportDto): Boolean {
    val minDateToReport = usageToReport.reportedAt.addSeconds(60)

    return minDateToReport.after(currentDateProvider.date)
  }
}

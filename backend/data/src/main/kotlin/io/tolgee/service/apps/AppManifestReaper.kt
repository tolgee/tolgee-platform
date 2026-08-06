package io.tolgee.service.apps

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.LockingProvider
import io.tolgee.component.SchedulingManager
import io.tolgee.configuration.tolgee.AppsProperties
import io.tolgee.constants.Message
import io.tolgee.exceptions.ExceptionWithMessage
import io.tolgee.model.apps.App
import io.tolgee.model.apps.AppManifestFailureKind
import io.tolgee.repository.apps.AppRepository
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import io.tolgee.util.runSentryCatching
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.time.Duration
import java.util.Date

/**
 * Re-fetches every registered app's manifest and, when one stays unreachable for long enough, first
 * marks the app unhealthy and tells its owner, then — much later, and only if
 * [AppsProperties.reapUnreachableApps] is on — removes it from every organization.
 *
 * The thresholds exist because this destroys other people's installs on a timer. A single failure,
 * or a burst of them inside a few minutes, must never count: the failure has to survive both a
 * minimum number of consecutive checks and a wall-clock window.
 */
@Component
class AppManifestReaper(
  private val appRepository: AppRepository,
  private val appManifestFetcher: AppManifestFetcher,
  private val appOwnerRemovalService: AppOwnerRemovalService,
  private val appManifestHealthNotifier: AppManifestHealthNotifier,
  private val currentDateProvider: CurrentDateProvider,
  private val appsProperties: AppsProperties,
  private val lockingProvider: LockingProvider,
  private val transactionManager: PlatformTransactionManager,
  private val schedulingManager: SchedulingManager,
) : Logging {
  @EventListener(ApplicationReadyEvent::class)
  fun scheduleSweep() {
    if (!appsProperties.enabled) return
    val period = Duration.ofMinutes(appsProperties.manifestHealthCheckPeriodMinutes)
    schedulingManager.scheduleWithFixedDelay(::sweep, period)
    logger.debug("Scheduled app manifest health sweep with period: {}", period)
  }

  fun sweep() {
    lockingProvider.withLockingIfFree(SWEEP_LOCK_NAME, SWEEP_LOCK_LEASE_TIME) {
      runSentryCatching {
        sweepAllApps()
      }
    }
  }

  /**
   * Ids are read in batches and each app is then checked on its own, outside any transaction: the
   * fetch reaches an app-controlled host, and one host that is merely slow must not hold a pooled
   * connection or delay the apps after it. `appsRestTemplate` bounds each fetch to a few seconds.
   */
  private fun sweepAllApps() {
    var afterId = 0L
    while (true) {
      val batch =
        executeInNewTransaction(transactionManager, readOnly = true) {
          appRepository.findIdsAfter(afterId, PageRequest.of(0, BATCH_SIZE))
        }
      if (batch.isEmpty()) return
      afterId = batch.last()

      batch.forEach { runSentryCatching { check(it) } }

      if (batch.size < BATCH_SIZE) return
    }
  }

  /** Checks one app and applies whatever the result implies. Public so tests can drive it directly. */
  fun check(appEntityId: Long) {
    val manifestUrl =
      executeInNewTransaction(transactionManager, readOnly = true) {
        appRepository.findById(appEntityId).orElse(null)?.manifestUrl
      } ?: return

    val failure = fetchFailure(manifestUrl)

    val outcome =
      executeInNewTransaction(transactionManager) {
        val app = appRepository.findById(appEntityId).orElse(null) ?: return@executeInNewTransaction null
        record(app, failure)
      } ?: return

    if (outcome.notify) notifyOwner(appEntityId)
    if (outcome.reap) {
      logger.info("Removing app {} — its manifest has been unreachable past the grace period", appEntityId)
      appOwnerRemovalService.removeEverywhere(appEntityId)
    }
  }

  /**
   * Stamped only once the notification has actually been sent, so an app left unhealthy but never
   * announced is visible as such — and gets another attempt on the next sweep.
   */
  private fun notifyOwner(appEntityId: Long) {
    executeInNewTransaction(transactionManager) {
      val app = appRepository.findById(appEntityId).orElse(null) ?: return@executeInNewTransaction
      appManifestHealthNotifier.notifyUnhealthy(app)
      app.unhealthyNotifiedAt = currentDateProvider.date
      appRepository.save(app)
    }
  }

  private data class Outcome(
    val notify: Boolean = false,
    val reap: Boolean = false,
  )

  private data class Failure(
    val kind: AppManifestFailureKind,
    val message: String,
  )

  /**
   * The manifest URL the **app** is registered under, never an install's. An install self-registered
   * against a development tunnel keeps whatever URL it last saw, and letting one stale install's URL
   * decide the health of an app every other organization uses would reap a working app.
   */
  private fun fetchFailure(manifestUrl: String): Failure? {
    try {
      appManifestFetcher.fetch(manifestUrl)
    } catch (e: Exception) {
      return Failure(kind = failureKindOf(e), message = e.message ?: e.javaClass.simpleName)
    }
    return null
  }

  /**
   * Only "nothing answered" ever leads to removal. A manifest that is served but no longer valid
   * means its author is still there, so it is surfaced to the owner and left alone.
   *
   * `URL_NOT_VALID` counts as unreachable because that is what a host whose DNS record is gone
   * produces — the very case reaping exists for.
   */
  private fun failureKindOf(e: Exception): AppManifestFailureKind {
    val message = (e as? ExceptionWithMessage)?.tolgeeMessage ?: return AppManifestFailureKind.UNREACHABLE
    return when (message) {
      Message.APP_MANIFEST_FETCH_FAILED, Message.URL_NOT_VALID -> AppManifestFailureKind.UNREACHABLE
      else -> AppManifestFailureKind.INVALID
    }
  }

  private fun record(
    app: App,
    failure: Failure?,
  ): Outcome {
    val now = currentDateProvider.date
    app.manifestLastCheckedAt = now

    if (failure == null) {
      clearFailureState(app)
      appRepository.save(app)
      return Outcome()
    }

    app.manifestFailureCount++
    app.manifestLastError = failure.message.take(MAX_ERROR_LENGTH)
    app.manifestLastFailureKind = failure.kind
    if (app.manifestFirstFailedAt == null) app.manifestFirstFailedAt = now

    val outcome = escalate(app, now)
    appRepository.save(app)
    return outcome
  }

  private fun clearFailureState(app: App) {
    app.manifestFailureCount = 0
    app.manifestFirstFailedAt = null
    app.manifestLastError = null
    app.manifestLastFailureKind = null
    app.unhealthySince = null
    app.unhealthyNotifiedAt = null
  }

  private fun escalate(
    app: App,
    now: Date,
  ): Outcome {
    // A server-owned app has no owner to warn and no owner to consent to its removal.
    if (app.organization == null) return Outcome()

    val unhealthySince = app.unhealthySince
    if (unhealthySince == null) {
      if (!hasFailedLongEnough(app, now)) return Outcome()
      app.unhealthySince = now
      logger.info("App {} marked unhealthy: {}", app.id, app.manifestLastError)
      return Outcome(notify = true)
    }

    if (app.unhealthyNotifiedAt == null) return Outcome(notify = true)
    if (!appsProperties.reapUnreachableApps) return Outcome()
    if (app.manifestLastFailureKind != AppManifestFailureKind.UNREACHABLE) return Outcome()
    val graceMs = Duration.ofDays(appsProperties.manifestReapAfterUnhealthyDays).toMillis()
    if (now.time - unhealthySince.time < graceMs) return Outcome()
    return Outcome(reap = true)
  }

  private fun hasFailedLongEnough(
    app: App,
    now: Date,
  ): Boolean {
    if (app.manifestFailureCount < appsProperties.manifestUnhealthyMinFailures) return false
    val firstFailedAt = app.manifestFirstFailedAt ?: return false
    val windowMs = Duration.ofHours(appsProperties.manifestUnhealthyAfterHours).toMillis()
    return now.time - firstFailedAt.time >= windowMs
  }

  companion object {
    private const val SWEEP_LOCK_NAME = "app_manifest_health_lock"
    private const val BATCH_SIZE = 100
    private const val MAX_ERROR_LENGTH = 500
    private val SWEEP_LOCK_LEASE_TIME = Duration.ofMinutes(30)
  }
}

package io.tolgee.service.apps.lifecycle

import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.AppsProperties
import io.tolgee.model.apps.AppDelivery
import io.tolgee.repository.apps.AppDeliveryRepository
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import io.tolgee.util.runSentryCatching
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

/**
 * Sends lifecycle deliveries and retries them with exponential backoff, off the thread of whatever
 * triggered them. Nothing here may propagate to the caller: an app whose server is down must still
 * be installable.
 */
@Component
class AppLifecycleDeliveryDispatcher(
  private val appLifecycleHttpClient: AppLifecycleHttpClient,
  private val appDeliveryRepository: AppDeliveryRepository,
  private val currentDateProvider: CurrentDateProvider,
  private val appsProperties: AppsProperties,
  private val transactionManager: PlatformTransactionManager,
) : Logging {
  private val pending = ConcurrentHashMap<Long, PendingAppDelivery>()

  /**
   * Called from another bean so Spring's async proxy applies — the first attempt reaches an
   * app-controlled host and must not sit in the caller's request thread.
   */
  @Async
  fun submit(delivery: PendingAppDelivery) {
    pending[delivery.deliveryId] = delivery
    runSentryCatching { attempt(delivery) }
  }

  /** Retries every delivery whose backoff has elapsed. Driven by [AppLifecycleDeliveryScheduler]. */
  fun retryDue() {
    val now = currentDateProvider.date
    pending.values
      .filter { it.nextAttemptAt?.after(now) != true }
      .forEach { runSentryCatching { attempt(it) } }
  }

  /**
   * Deliveries whose sending process is gone. Only rows older than the whole retry window are
   * touched, so a delivery another replica is still retrying is left alone.
   */
  fun abandonStale() {
    val cutoff = Date(currentDateProvider.date.time - totalRetryWindowMs())
    executeInNewTransaction(transactionManager) {
      appDeliveryRepository
        .findUnfinishedCreatedBefore(cutoff)
        .filter { !pending.containsKey(it.id) }
        .forEach {
          it.abandonedAt = currentDateProvider.date
          it.lastError = ABANDONED_ERROR
          appDeliveryRepository.save(it)
        }
    }
  }

  private fun attempt(delivery: PendingAppDelivery) {
    if (!pending.containsKey(delivery.deliveryId)) return
    delivery.attempts++

    try {
      appLifecycleHttpClient.post(delivery.targetUrl, delivery.payload, delivery.signingSecret)
    } catch (e: Exception) {
      recordFailure(delivery, e.message ?: e.javaClass.simpleName)
      return
    }

    pending.remove(delivery.deliveryId)
    updateRecord(delivery) {
      it.deliveredAt = currentDateProvider.date
      it.lastError = null
    }
  }

  private fun recordFailure(
    delivery: PendingAppDelivery,
    error: String,
  ) {
    val exhausted = delivery.attempts >= appsProperties.lifecycleDeliveryMaxAttempts
    if (exhausted) {
      pending.remove(delivery.deliveryId)
    }
    delivery.nextAttemptAt = Date(currentDateProvider.date.time + backoffMs(delivery.attempts))

    updateRecord(delivery) {
      it.lastError = error.take(MAX_ERROR_LENGTH)
      if (exhausted) it.abandonedAt = currentDateProvider.date
    }
    logger.info(
      "App lifecycle delivery {} to {} failed on attempt {}: {}",
      delivery.deliveryId,
      delivery.targetUrl,
      delivery.attempts,
      error,
    )
  }

  private fun updateRecord(
    delivery: PendingAppDelivery,
    update: (AppDelivery) -> Unit,
  ) {
    runSentryCatching {
      executeInNewTransaction(transactionManager) {
        val record = appDeliveryRepository.findById(delivery.deliveryId).orElse(null) ?: return@executeInNewTransaction
        record.attempts = delivery.attempts
        record.lastAttemptAt = currentDateProvider.date
        update(record)
        appDeliveryRepository.save(record)
      }
    }
  }

  private fun backoffMs(attempts: Int): Long {
    val exponent = (attempts - 1).coerceIn(0, MAX_BACKOFF_EXPONENT)
    val backoff = appsProperties.lifecycleDeliveryInitialBackoffSeconds * (1L shl exponent)
    return backoff.coerceAtMost(appsProperties.lifecycleDeliveryMaxBackoffSeconds) * 1000
  }

  private fun totalRetryWindowMs(): Long {
    return (1..appsProperties.lifecycleDeliveryMaxAttempts).sumOf { backoffMs(it) }
  }

  companion object {
    const val ABANDONED_ERROR = "not delivered before the sending server stopped"
    private const val MAX_ERROR_LENGTH = 500

    /** 2^30 seconds is already far beyond any sane max backoff; this only stops the shift overflowing. */
    private const val MAX_BACKOFF_EXPONENT = 30
  }
}

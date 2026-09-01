package io.tolgee.service.apps

import io.tolgee.dtos.apps.AppLifecycleAppCredentials
import io.tolgee.dtos.apps.AppLifecycleDeliveryOutcome
import io.tolgee.model.apps.App
import io.tolgee.model.apps.AppLifecycleEventType
import io.tolgee.service.apps.lifecycle.AppLifecycleDeliveryService
import io.tolgee.util.executeInNewTransaction
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import java.util.Date

/**
 * Rolls an app-level client secret and hands the replacement to the app over the lifecycle channel.
 * A new secret is minted and every other active one put on a deadline, then the new secret is
 * delivered. A dead app host makes the delivery a reported failure, never undoing the rotation. The
 * old secrets are never revoked here — a landed delivery only proves the app received the webhook,
 * not that it adopted the secret, so cutting anything off is left to the grace window or to the
 * operator's explicit revoke.
 */
@Service
class AppSecretRotationService(
  private val appSecretService: AppSecretService,
  private val appLifecycleDeliveryService: AppLifecycleDeliveryService,
  private val transactionManager: PlatformTransactionManager,
  private val appActivityRecorder: AppActivityRecorder,
) {
  data class RotationResult(
    val issued: AppSecretService.IssueResult,
    val delivery: AppLifecycleDeliveryOutcome?,
    /** When the outgoing secrets lapse, or null when there was nothing to retire. */
    val previousExpiresAt: Date?,
  )

  fun rotate(
    app: App,
    graceSeconds: Long,
  ): RotationResult {
    // Committed before delivery so an app that tests the new secret immediately isn't rejected.
    val (issued, previousExpiresAt) = issueAndExpireOthersInNewTransaction(app, graceSeconds)
    val delivery =
      appLifecycleDeliveryService.deliverNow(
        appEntityId = app.id,
        eventType = AppLifecycleEventType.APP_SECRET_ROTATED,
        appCredentials =
          AppLifecycleAppCredentials(
            clientId = app.clientId,
            clientSecret = issued.plaintextSecret,
          ),
      )
    return RotationResult(
      issued = issued,
      delivery = delivery,
      previousExpiresAt = previousExpiresAt,
    )
  }

  private fun issueAndExpireOthersInNewTransaction(
    app: App,
    graceSeconds: Long,
  ): Pair<AppSecretService.IssueResult, Date?> =
    executeInNewTransaction(transactionManager) {
      appActivityRecorder.record(app)
      val issued = appSecretService.issue(app)
      issued to appSecretService.expireOthers(app.id, issued.secret.id, graceSeconds)
    }
}

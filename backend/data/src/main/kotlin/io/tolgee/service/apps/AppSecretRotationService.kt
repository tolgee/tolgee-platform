package io.tolgee.service.apps

import io.tolgee.dtos.apps.AppLifecycleAppCredentials
import io.tolgee.dtos.apps.AppLifecycleDeliveryOutcome
import io.tolgee.model.apps.App
import io.tolgee.model.apps.AppLifecycleEventType
import io.tolgee.service.apps.lifecycle.AppLifecycleDeliveryService
import org.springframework.stereotype.Service
import java.util.Date

/**
 * Rolls an app-level client secret and hands the replacement to the app over the lifecycle channel.
 * The secret is minted and the old ones put on a deadline in [AppSecretService.rotate]'s own
 * transaction; the delivery runs after that commits, so a dead app host reports a failed delivery
 * without undoing the rotation. The old secrets are never revoked here — a landed delivery only
 * proves the app received the webhook, not that it adopted the secret, so cutting anything off is
 * left to the grace window or to the operator's explicit revoke.
 */
@Service
class AppSecretRotationService(
  private val appSecretService: AppSecretService,
  private val appLifecycleDeliveryService: AppLifecycleDeliveryService,
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
    val rotated = appSecretService.rotate(app, graceSeconds)
    val delivery =
      appLifecycleDeliveryService.deliverNow(
        appEntityId = app.id,
        eventType = AppLifecycleEventType.APP_SECRET_ROTATED,
        appCredentials =
          AppLifecycleAppCredentials(
            clientId = app.clientId,
            clientSecret = rotated.issued.plaintextSecret,
          ),
      )
    return RotationResult(
      issued = rotated.issued,
      delivery = delivery,
      previousExpiresAt = rotated.previousExpiresAt,
    )
  }
}

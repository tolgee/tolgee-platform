package io.tolgee.service.apps

import io.tolgee.dtos.apps.AppLifecycleAppCredentials
import io.tolgee.dtos.apps.AppLifecycleDeliveryOutcome
import io.tolgee.model.apps.App
import io.tolgee.model.apps.AppLifecycleEventType
import io.tolgee.service.apps.lifecycle.AppLifecycleDeliveryService
import org.springframework.stereotype.Service
import java.util.Date

/**
 * Rolls an app-level client secret: mints a replacement, hands it to the app over the lifecycle
 * channel, and retires the outgoing one. If the app took the new secret over that channel the old
 * one is cut off at once; otherwise it keeps working through a grace window so an app configured by
 * hand can be switched over first.
 *
 * Kept out of [AppSecretService] so the delivery happens after that service's transaction commits,
 * and out of [AppService] so the delivery service can keep depending on it.
 */
@Service
class AppSecretRotationService(
  private val appService: AppService,
  private val appSecretService: AppSecretService,
  private val appLifecycleDeliveryService: AppLifecycleDeliveryService,
) {
  data class RotationResult(
    val issued: AppSecretService.IssueResult,
    /** Present only on the operator path — the app-initiated path returns the secret in its response. */
    val delivery: AppLifecycleDeliveryOutcome?,
    /** When the outgoing secrets lapse, or null when there was nothing to retire. */
    val previousExpiresAt: Date? = null,
  )

  /**
   * The operator path: issue a new secret, hand it to the app, and put every other active secret on
   * a [graceSeconds] deadline. The old ones are never revoked here — a landed delivery only proves
   * the app received the webhook, not that it adopted the secret, so cutting anything off is left to
   * the window or to the operator's explicit revoke.
   */
  fun rotate(
    app: App,
    graceSeconds: Long,
  ): RotationResult {
    val result = issueAndDeliver(app)
    val previousExpiresAt =
      appSecretService.expireOthers(
        appId = app.id,
        keepSecretId = result.issued.secret.id,
        graceSeconds = graceSeconds,
      )
    return result.copy(previousExpiresAt = previousExpiresAt)
  }

  /**
   * Issues a new secret and hands it to the app synchronously, so the caller can tell whether the
   * app took it or the operator still has to copy it. Does not touch the old secret.
   */
  fun issueAndDeliver(app: App): RotationResult {
    val issued = appSecretService.issue(app)
    val clientId = appService.resolveClientId(app.id)
    val delivery =
      appLifecycleDeliveryService.deliverNow(
        appEntityId = app.id,
        eventType = AppLifecycleEventType.APP_SECRET_ROTATED,
        appCredentials = AppLifecycleAppCredentials(clientId = clientId, clientSecret = issued.plaintextSecret),
      )
    return RotationResult(issued = issued, delivery = delivery)
  }

  /** The app-initiated path: issue only. The caller is the app and reads the secret from the response. */
  fun issue(app: App): AppSecretService.IssueResult {
    return appSecretService.issue(app)
  }
}

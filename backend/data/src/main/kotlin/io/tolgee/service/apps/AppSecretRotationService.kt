package io.tolgee.service.apps

import io.tolgee.dtos.apps.AppLifecycleAppCredentials
import io.tolgee.dtos.apps.AppLifecycleDeliveryOutcome
import io.tolgee.model.apps.App
import io.tolgee.model.apps.AppLifecycleEventType
import io.tolgee.service.apps.lifecycle.AppLifecycleDeliveryService
import org.springframework.stereotype.Service

/**
 * Phase one of an app-level rotation. The new secret leaves Tolgee in the response and, on the
 * operator path, over the lifecycle channel too, because the two callers need different things: an
 * app rotating itself reads the response, but an operator rotating by hand has no response to hand
 * to the app.
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
  )

  /**
   * The operator path: issue a new secret and hand it to the app synchronously, so the rotation
   * dialog can say whether the app took it or the operator still has to copy it.
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

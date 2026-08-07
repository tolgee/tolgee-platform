package io.tolgee.service.apps

import io.tolgee.dtos.apps.AppLifecycleAppCredentials
import io.tolgee.model.apps.App
import io.tolgee.model.apps.AppLifecycleEventType
import io.tolgee.service.apps.lifecycle.AppLifecycleDeliveryService
import org.springframework.stereotype.Service

/**
 * Phase one of an app-level rotation, plus the push that goes with it. The new secret leaves Tolgee
 * twice — in the response and over the lifecycle channel — because the two callers need different
 * things: an operator rotating by hand cannot read a delivery, and an app rotating unattended has no
 * response to paste anywhere.
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
  fun issue(app: App): AppSecretService.IssueResult {
    val issued = appSecretService.issue(app)
    val clientId = appService.resolveClientId(app.id)

    appLifecycleDeliveryService.deliver(
      appEntityId = app.id,
      eventType = AppLifecycleEventType.APP_SECRET_ROTATED,
      appCredentials = AppLifecycleAppCredentials(clientId = clientId, clientSecret = issued.plaintextSecret),
    )
    return issued
  }
}

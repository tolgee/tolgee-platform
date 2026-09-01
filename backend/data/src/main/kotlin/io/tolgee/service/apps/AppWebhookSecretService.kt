package io.tolgee.service.apps

import io.tolgee.component.KeyGenerator
import io.tolgee.constants.Message
import io.tolgee.dtos.apps.AppLifecycleAppCredentials
import io.tolgee.dtos.apps.AppLifecycleDeliveryOutcome
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.apps.AppLifecycleEventType
import io.tolgee.repository.apps.AppRepository
import io.tolgee.service.apps.lifecycle.AppLifecycleDeliveryService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Rotates the secret Tolgee signs an app's lifecycle deliveries with. The new secret is delivered to
 * the app **signed with the old one** — the only secret the app is guaranteed to already hold — so a
 * running app can verify that delivery and adopt the new secret without being locked out. Tolgee
 * signs every later delivery with the new secret.
 *
 * A running app keeps accepting deliveries signed with the previous secret during the overlap and
 * drops it on its own next rotation — that overlap is the app's concern, not Tolgee's, so there is
 * nothing here to "revoke".
 */
@Service
class AppWebhookSecretService(
  private val appRepository: AppRepository,
  private val appService: AppService,
  private val keyGenerator: KeyGenerator,
  private val appLifecycleDeliveryService: AppLifecycleDeliveryService,
) {
  data class RotationResult(
    val newSecret: String,
    val delivery: AppLifecycleDeliveryOutcome?,
  )

  @Transactional
  fun rotate(appEntityId: Long): RotationResult {
    val previous = appService.resolveWebhookSecret(appEntityId)
    val clientId = appService.resolveClientId(appEntityId)
    val app =
      appRepository.findById(appEntityId).orElse(null)
        ?: throw NotFoundException(Message.APP_NOT_FOUND)

    val newSecret = keyGenerator.generate(256)
    app.webhookSecret = newSecret
    appRepository.save(app)

    val target =
      AppLifecycleDeliveryService.AppTarget(
        appEntityId = app.id,
        appIdentifier = app.appId,
        baseUrl = app.baseUrl,
        // Signed with the old secret so a running app can verify the delivery that carries the new one.
        signingSecret = previous,
      )
    val delivery =
      appLifecycleDeliveryService.deliverNow(
        target = target,
        eventType = AppLifecycleEventType.APP_SECRET_ROTATED,
        appCredentials =
          AppLifecycleAppCredentials(
            clientId = clientId,
            clientSecret = null,
            webhookSecret = newSecret,
          ),
      )
    return RotationResult(newSecret, delivery)
  }
}

package io.tolgee.service.apps

import io.tolgee.component.KeyGenerator
import io.tolgee.constants.Message
import io.tolgee.dtos.apps.AppLifecycleAppCredentials
import io.tolgee.dtos.apps.AppLifecycleDeliveryOutcome
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.apps.AppLifecycleEventType
import io.tolgee.repository.apps.AppRepository
import io.tolgee.service.apps.lifecycle.AppLifecycleDeliveryService
import io.tolgee.util.executeInNewTransaction
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager

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
  private val keyGenerator: KeyGenerator,
  private val appLifecycleDeliveryService: AppLifecycleDeliveryService,
  private val transactionManager: PlatformTransactionManager,
) {
  data class RotationResult(
    val newSecret: String,
    val delivery: AppLifecycleDeliveryOutcome?,
  )

  private data class NewWebhookSecret(
    val target: AppLifecycleDeliveryService.AppTarget,
    val clientId: String,
    val newSecret: String,
  )

  fun rotate(appEntityId: Long): RotationResult {
    // Persist and commit the new secret before delivering it, so Tolgee never signs a later delivery
    // with a secret the app already switched to but this transaction hadn't yet committed.
    val rotated =
      executeInNewTransaction(transactionManager) {
        val app =
          appRepository.findById(appEntityId).orElse(null)
            ?: throw NotFoundException(Message.APP_NOT_FOUND)

        val previous = app.webhookSecret
        val newSecret = keyGenerator.generate(256)
        app.webhookSecret = newSecret
        appRepository.save(app)

        NewWebhookSecret(
          target =
            AppLifecycleDeliveryService.AppTarget(
              appEntityId = app.id,
              appIdentifier = app.appId,
              baseUrl = app.baseUrl,
              // Signed with the old secret so a running app can verify the delivery that carries the new one.
              signingSecret = previous,
            ),
          clientId = app.clientId,
          newSecret = newSecret,
        )
      }

    val delivery =
      appLifecycleDeliveryService.deliverNow(
        target = rotated.target,
        eventType = AppLifecycleEventType.APP_SECRET_ROTATED,
        appCredentials =
          AppLifecycleAppCredentials(
            clientId = rotated.clientId,
            clientSecret = null,
            webhookSecret = rotated.newSecret,
          ),
      )
    return RotationResult(rotated.newSecret, delivery)
  }
}

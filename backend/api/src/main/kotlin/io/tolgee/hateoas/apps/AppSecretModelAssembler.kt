package io.tolgee.hateoas.apps

import io.tolgee.dtos.apps.AppLifecycleDeliveryOutcome
import io.tolgee.model.apps.AppSecret
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class AppSecretModelAssembler : RepresentationModelAssembler<AppSecret, AppSecretModel> {
  override fun toModel(entity: AppSecret): AppSecretModel {
    return build(entity, plaintextSecret = null, delivery = null)
  }

  /** Builds the model including the plaintext — used once, in the response to issuing the secret. */
  fun toModelWithSecret(
    entity: AppSecret,
    plaintextSecret: String,
    delivery: AppLifecycleDeliveryOutcome? = null,
  ): AppSecretModel {
    return build(entity, plaintextSecret, delivery)
  }

  private fun build(
    entity: AppSecret,
    plaintextSecret: String?,
    delivery: AppLifecycleDeliveryOutcome?,
  ): AppSecretModel {
    return AppSecretModel(
      id = entity.id,
      prefix = entity.secretPrefix,
      createdAt = entity.createdAt?.time ?: 0,
      lastUsedAt = entity.lastUsedAt?.time,
      revokedAt = entity.revokedAt?.time,
      secret = plaintextSecret,
      delivery = delivery?.toModel(),
    )
  }
}

fun AppLifecycleDeliveryOutcome.toModel(): AppDeliveryOutcomeModel =
  AppDeliveryOutcomeModel(attempted = attempted, delivered = delivered, error = error)

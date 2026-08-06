package io.tolgee.hateoas.apps

import io.tolgee.model.apps.AppInstallSecret
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class AppInstallSecretModelAssembler : RepresentationModelAssembler<AppInstallSecret, AppInstallSecretModel> {
  override fun toModel(entity: AppInstallSecret): AppInstallSecretModel {
    return build(entity, plaintextSecret = null)
  }

  /** Builds the model including the plaintext — used once, in the response to issuing the secret. */
  fun toModelWithSecret(
    entity: AppInstallSecret,
    plaintextSecret: String,
  ): AppInstallSecretModel {
    return build(entity, plaintextSecret)
  }

  private fun build(
    entity: AppInstallSecret,
    plaintextSecret: String?,
  ): AppInstallSecretModel {
    return AppInstallSecretModel(
      id = entity.id,
      prefix = entity.secretPrefix,
      createdAt = entity.createdAt?.time ?: 0,
      lastUsedAt = entity.lastUsedAt?.time,
      revokedAt = entity.revokedAt?.time,
      secret = plaintextSecret,
    )
  }
}

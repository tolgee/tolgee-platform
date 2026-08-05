package io.tolgee.hateoas.organization.apps

import io.tolgee.dtos.apps.AppManifest
import io.tolgee.model.apps.AppInstall
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

@Component
class AppInstallModelAssembler(
  private val objectMapper: ObjectMapper,
) : RepresentationModelAssembler<AppInstall, AppInstallModel> {
  override fun toModel(entity: AppInstall): AppInstallModel {
    return build(entity, plaintextClientSecret = null)
  }

  /**
   * Builds the model including the plaintext client secret — used once, in the response to
   * registration. The secret is never persisted, so it can only be surfaced here.
   */
  fun toModelWithSecret(
    entity: AppInstall,
    plaintextClientSecret: String,
  ): AppInstallModel {
    return build(entity, plaintextClientSecret)
  }

  private fun build(
    entity: AppInstall,
    plaintextClientSecret: String?,
  ): AppInstallModel {
    val manifest = objectMapper.readValue<AppManifest>(entity.manifestJson)
    return AppInstallModel(
      id = entity.id,
      manifestUrl = entity.manifestUrl,
      appId = entity.appId,
      name = entity.name,
      version = entity.version,
      baseUrl = entity.baseUrl,
      modules = manifest.modules,
      scopes = entity.grantedScopes.map { it.value },
      clientId = entity.clientId,
      clientSecretPrefix = entity.clientSecretPrefix,
      availableToAllOrganizations = entity.availableToAllOrganizations,
      clientSecret = plaintextClientSecret,
    )
  }
}

package io.tolgee.hateoas.organization.apps

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.dtos.apps.AppManifest
import io.tolgee.model.apps.AppInstall
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class AppInstallModelAssembler(
  private val objectMapper: ObjectMapper,
) : RepresentationModelAssembler<AppInstall, AppInstallModel> {
  override fun toModel(entity: AppInstall): AppInstallModel {
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
      webhookEvents = entity.webhookSubscriptions.toList(),
      webhookUrl = entity.webhookUrl,
      clientId = entity.clientId,
      clientSecretPrefix = entity.clientSecretPrefix,
      webhookSecret = entity.webhookSecret,
      decoratorsUrl = manifest.decoratorsUrl,
    )
  }
}

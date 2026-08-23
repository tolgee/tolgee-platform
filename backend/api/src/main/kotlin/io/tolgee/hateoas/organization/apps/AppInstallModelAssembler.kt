package io.tolgee.hateoas.organization.apps

import io.tolgee.dtos.apps.AppManifestDto
import io.tolgee.model.apps.AppInstall
import io.tolgee.service.apps.AppEnablementService
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

@Component
class AppInstallModelAssembler(
  private val objectMapper: ObjectMapper,
  private val appEnablementService: AppEnablementService,
) : RepresentationModelAssembler<AppInstall, AppInstallModel> {
  override fun toModel(entity: AppInstall): AppInstallModel {
    return build(entity, appEnablementService.countEnabledProjectsForInstall(entity.id))
  }

  /** Builds the whole listing with the enabled-project counts fetched in a single query. */
  override fun toCollectionModel(entities: Iterable<AppInstall>): CollectionModel<AppInstallModel> {
    val installs = entities.toList()
    val counts = appEnablementService.countEnabledProjectsByInstall(installs.map { it.id })
    return CollectionModel.of(installs.map { build(it, counts[it.id] ?: 0) })
  }

  private fun build(
    entity: AppInstall,
    enabledProjectCount: Long,
  ): AppInstallModel {
    val registeredApp = entity.app
    val manifest = objectMapper.readValue<AppManifestDto>(registeredApp.manifestJson)
    return AppInstallModel(
      id = entity.id,
      manifestUrl = registeredApp.manifestUrl,
      appId = registeredApp.appId,
      name = registeredApp.name,
      version = registeredApp.version,
      baseUrl = registeredApp.baseUrl,
      icon = registeredApp.icon,
      enabledProjectCount = enabledProjectCount,
      modules = manifest.modules,
      scopes = entity.grantedScopes.map { it.value },
    )
  }
}

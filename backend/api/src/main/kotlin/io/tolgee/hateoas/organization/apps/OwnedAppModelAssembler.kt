package io.tolgee.hateoas.organization.apps

import io.tolgee.model.apps.App
import io.tolgee.service.apps.AppService
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class OwnedAppModelAssembler(
  private val appService: AppService,
) : RepresentationModelAssembler<App, OwnedAppModel> {
  override fun toModel(entity: App): OwnedAppModel {
    return build(entity, appService.countInstalls(entity.id))
  }

  /** Builds the whole owned-apps listing with the install counts fetched in a single query. */
  override fun toCollectionModel(entities: Iterable<App>): CollectionModel<OwnedAppModel> {
    val apps = entities.toList()
    val counts = appService.countInstallsByApp(apps.map { it.id })
    return CollectionModel.of(apps.map { build(it, counts[it.id] ?: 0) })
  }

  private fun build(
    entity: App,
    installCount: Long,
  ): OwnedAppModel {
    return OwnedAppModel(
      id = entity.id,
      appId = entity.appId,
      name = entity.name,
      version = entity.version,
      manifestUrl = entity.manifestUrl,
      baseUrl = entity.baseUrl,
      icon = entity.icon,
      clientId = entity.clientId,
      installCount = installCount,
      manifestLastCheckedAt = entity.manifestLastCheckedAt?.time,
      manifestFailureCount = entity.manifestFailureCount,
      manifestFirstFailedAt = entity.manifestFirstFailedAt?.time,
      manifestLastError = entity.manifestLastError,
      manifestLastFailureKind = entity.manifestLastFailureKind?.name,
      unhealthySince = entity.unhealthySince?.time,
    )
  }
}

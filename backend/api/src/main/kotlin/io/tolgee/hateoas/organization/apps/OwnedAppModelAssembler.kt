package io.tolgee.hateoas.organization.apps

import io.tolgee.model.apps.App
import io.tolgee.service.apps.AppService
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class OwnedAppModelAssembler(
  private val appService: AppService,
) : RepresentationModelAssembler<App, OwnedAppModel> {
  override fun toModel(entity: App): OwnedAppModel {
    return OwnedAppModel(
      id = entity.id,
      appId = entity.appId,
      name = entity.name,
      manifestUrl = entity.manifestUrl,
      baseUrl = entity.baseUrl,
      clientId = entity.clientId,
      installCount = appService.countInstalls(entity.id),
      manifestLastCheckedAt = entity.manifestLastCheckedAt?.time,
      manifestFailureCount = entity.manifestFailureCount,
      manifestFirstFailedAt = entity.manifestFirstFailedAt?.time,
      manifestLastError = entity.manifestLastError,
      manifestLastFailureKind = entity.manifestLastFailureKind?.name,
      unhealthySince = entity.unhealthySince?.time,
      availableToAllOrganizations = entity.availableToAllOrganizations,
    )
  }
}

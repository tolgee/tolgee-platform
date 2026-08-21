package io.tolgee.hateoas.organization.apps

import io.tolgee.model.apps.App
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class AvailableAppModelAssembler : RepresentationModelAssembler<App, AvailableAppModel> {
  override fun toModel(entity: App): AvailableAppModel {
    return AvailableAppModel(
      id = entity.id,
      appId = entity.appId,
      name = entity.name,
      baseUrl = entity.baseUrl,
      icon = entity.icon,
      manifestUrl = entity.manifestUrl,
    )
  }
}

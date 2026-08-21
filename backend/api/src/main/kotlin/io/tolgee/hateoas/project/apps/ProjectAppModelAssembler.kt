package io.tolgee.hateoas.project.apps

import io.tolgee.dtos.apps.AppManifestDto
import io.tolgee.model.apps.AppInstall
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

@Component
class ProjectAppModelAssembler(
  private val objectMapper: ObjectMapper,
) {
  fun toModel(
    install: AppInstall,
    enabled: Boolean,
  ): ProjectAppModel {
    val app = install.app
    val manifest = objectMapper.readValue<AppManifestDto>(app.manifestJson)
    return ProjectAppModel(
      id = install.id,
      appId = app.appId,
      name = app.name,
      version = app.version,
      baseUrl = app.baseUrl,
      modules = manifest.modules,
      enabled = enabled,
    )
  }
}

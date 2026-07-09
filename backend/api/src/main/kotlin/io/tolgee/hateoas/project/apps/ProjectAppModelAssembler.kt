package io.tolgee.hateoas.project.apps

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.dtos.apps.AppManifest
import io.tolgee.model.apps.AppInstall
import org.springframework.stereotype.Component

@Component
class ProjectAppModelAssembler(
  private val objectMapper: ObjectMapper,
) {
  fun toModel(
    install: AppInstall,
    enabled: Boolean,
  ): ProjectAppModel {
    val manifest = objectMapper.readValue<AppManifest>(install.manifestJson)
    return ProjectAppModel(
      id = install.id,
      manifestUrl = install.manifestUrl,
      appId = install.appId,
      name = install.name,
      version = install.version,
      baseUrl = install.baseUrl,
      modules = manifest.modules,
      enabled = enabled,
      decoratorsUrl = manifest.decoratorsUrl,
    )
  }
}

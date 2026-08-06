package io.tolgee.hateoas.apps

import io.tolgee.dtos.apps.AppEnabledProjectDto
import io.tolgee.model.apps.AppInstall
import org.springframework.stereotype.Component

@Component
class AppSelfInstallationModelAssembler {
  fun toModel(
    install: AppInstall,
    native: Boolean,
    enabledProjects: List<AppEnabledProjectDto>,
  ): AppSelfInstallationModel {
    return AppSelfInstallationModel(
      id = install.id,
      appId = install.appId,
      name = install.name,
      version = install.version,
      native = native,
      scopes = install.grantedScopes.map { it.value },
      enabledProjects = enabledProjects.map { toProjectModel(it) },
    )
  }

  private fun toProjectModel(project: AppEnabledProjectDto): AppSelfEnabledProjectModel {
    return AppSelfEnabledProjectModel(
      id = project.id,
      name = project.name,
      organization =
        AppSelfProjectOrganizationModel(
          id = project.organizationId,
          name = project.organizationName,
          slug = project.organizationSlug,
        ),
    )
  }
}

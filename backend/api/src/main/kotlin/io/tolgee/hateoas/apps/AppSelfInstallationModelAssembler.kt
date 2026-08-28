package io.tolgee.hateoas.apps

import io.tolgee.model.apps.AppInstall
import org.springframework.stereotype.Component

@Component
class AppSelfInstallationModelAssembler {
  fun toModel(install: AppInstall): AppSelfInstallationModel {
    return AppSelfInstallationModel(
      id = install.id,
      appId = install.app.appId,
      name = install.app.name,
      version = install.app.version,
      scopes = install.grantedScopes.map { it.value },
    )
  }
}

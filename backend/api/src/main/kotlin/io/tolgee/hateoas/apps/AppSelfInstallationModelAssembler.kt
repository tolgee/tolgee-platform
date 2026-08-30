package io.tolgee.hateoas.apps

import io.tolgee.model.apps.AppInstall
import io.tolgee.service.apps.AppService
import org.springframework.stereotype.Component

@Component
class AppSelfInstallationModelAssembler {
  fun toModel(install: AppInstall): AppSelfInstallationModel {
    val grantedScopes = install.grantedScopes.map { it.value }
    return AppSelfInstallationModel(
      id = install.id,
      appId = install.app.appId,
      name = install.app.name,
      version = install.app.version,
      scopes = grantedScopes,
      pendingScopes = (AppService.splitScopes(install.app.manifestScopes) - grantedScopes.toSet()).sorted(),
    )
  }
}

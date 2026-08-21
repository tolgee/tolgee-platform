package io.tolgee.hateoas.organization.apps

import io.tolgee.dtos.apps.AppManifestDto
import io.tolgee.hateoas.apps.AppModel
import io.tolgee.model.apps.AppInstall
import io.tolgee.service.apps.AppEnablementService
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppService
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
    return build(entity, app = null)
  }

  /**
   * Builds the response to registering or installing an app — the only place the app-level
   * plaintext credentials are disclosed. [AppInstallService.RegisterResult.appCredentials] is null
   * unless this very call registered the app, which is what keeps an organization that merely
   * installed somebody else's app from ever seeing its app-level credentials.
   */
  fun toModel(result: AppInstallService.RegisterResult): AppInstallModel {
    return build(result.install, appModel(result.app, result.appCredentials))
  }

  private fun appModel(
    app: AppService.AppSummary,
    credentials: AppService.AppCredentials?,
  ): AppModel {
    return AppModel(
      id = app.id,
      appId = app.appId,
      name = app.name,
      clientId = credentials?.clientId,
      clientSecret = credentials?.clientSecret,
      webhookSecret = credentials?.webhookSecret,
    )
  }

  private fun build(
    entity: AppInstall,
    app: AppModel?,
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
      enabledProjectCount = appEnablementService.listEnabledProjectsForInstall(entity.id).size.toLong(),
      modules = manifest.modules,
      scopes = entity.grantedScopes.map { it.value },
      pendingScopes = emptyList(),
      app = app,
    )
  }
}

package io.tolgee.hateoas.organization.apps

import io.tolgee.dtos.apps.AppManifest
import io.tolgee.hateoas.apps.AppModel
import io.tolgee.model.apps.AppInstall
import io.tolgee.service.apps.AppInstallService
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

@Component
class AppInstallModelAssembler(
  private val objectMapper: ObjectMapper,
) : RepresentationModelAssembler<AppInstall, AppInstallModel> {
  override fun toModel(entity: AppInstall): AppInstallModel {
    return build(entity, plaintextClientSecret = null, app = null)
  }

  /**
   * Builds the response to registering or installing an app — the only place either layer's
   * plaintext credentials are disclosed. [AppInstallService.RegisterResult.appCredentials] is null
   * unless this very call registered the app, which is what keeps an organization that merely
   * installed somebody else's app from ever seeing its app-level credentials.
   */
  fun toModel(result: AppInstallService.RegisterResult): AppInstallModel {
    val credentials = result.appCredentials
    val app =
      AppModel(
        id = result.app.id,
        appId = result.app.appId,
        name = result.app.name,
        clientId = credentials?.clientId,
        clientSecret = credentials?.clientSecret,
        webhookSecret = credentials?.webhookSecret,
      )
    return build(result.install, result.plaintextClientSecret, app)
  }

  fun toModel(result: AppInstallService.SelfRegisterResult): AppInstallModel {
    val credentials = result.appCredentials
    val app =
      AppModel(
        id = result.app.id,
        appId = result.app.appId,
        name = result.app.name,
        clientId = credentials?.clientId,
        clientSecret = credentials?.clientSecret,
        webhookSecret = credentials?.webhookSecret,
      )
    return build(result.install, result.plaintextClientSecret, app)
  }

  private fun build(
    entity: AppInstall,
    plaintextClientSecret: String?,
    app: AppModel?,
  ): AppInstallModel {
    val manifest = objectMapper.readValue<AppManifest>(entity.manifestJson)
    return AppInstallModel(
      id = entity.id,
      manifestUrl = entity.manifestUrl,
      appId = entity.appId,
      name = entity.name,
      version = entity.version,
      baseUrl = entity.baseUrl,
      modules = manifest.modules,
      scopes = entity.grantedScopes.map { it.value },
      clientId = entity.clientId,
      availableToAllOrganizations = entity.availableToAllOrganizations,
      clientSecret = plaintextClientSecret,
      app = app,
    )
  }
}

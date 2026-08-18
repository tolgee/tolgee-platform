package io.tolgee.hateoas.organization.apps

import io.tolgee.dtos.apps.AppLifecycleDeliveryOutcome
import io.tolgee.dtos.apps.AppManifest
import io.tolgee.hateoas.apps.AppModel
import io.tolgee.hateoas.apps.toModel
import io.tolgee.model.apps.AppInstall
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppService
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

@Component
class AppInstallModelAssembler(
  private val objectMapper: ObjectMapper,
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
    return build(result.install, appModel(result.app, result.appCredentials, result.delivery))
  }

  fun toModel(result: AppInstallService.SelfRegisterResult): AppInstallModel {
    return build(result.install, appModel(result.app, result.appCredentials, delivery = null), created = result.created)
  }

  private fun appModel(
    app: AppService.AppSummary,
    credentials: AppService.AppCredentials?,
    delivery: AppLifecycleDeliveryOutcome?,
  ): AppModel {
    return AppModel(
      id = app.id,
      appId = app.appId,
      name = app.name,
      clientId = credentials?.clientId,
      clientSecret = credentials?.clientSecret,
      webhookSecret = credentials?.webhookSecret,
      delivery = delivery?.toModel(),
    )
  }

  private fun build(
    entity: AppInstall,
    app: AppModel?,
    created: Boolean? = null,
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
      created = created,
      app = app,
    )
  }
}

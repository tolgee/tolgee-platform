package io.tolgee.hateoas.organization.apps

import io.tolgee.dtos.apps.AppManifestModules
import org.springframework.hateoas.server.core.Relation

@Relation(itemRelation = "appRegistration")
class AppRegistrationResponseModel(
  id: Long,
  manifestUrl: String,
  appId: String,
  name: String,
  version: String,
  baseUrl: String,
  modules: AppManifestModules,
  scopes: List<String>,
  webhookEvents: List<String>,
  webhookUrl: String?,
  clientId: String?,
  clientSecretPrefix: String?,
  webhookSecret: String?,
  decoratorsUrl: String?,
  val clientSecret: String,
) : AppInstallModel(
    id = id,
    manifestUrl = manifestUrl,
    appId = appId,
    name = name,
    version = version,
    baseUrl = baseUrl,
    modules = modules,
    scopes = scopes,
    webhookEvents = webhookEvents,
    webhookUrl = webhookUrl,
    clientId = clientId,
    clientSecretPrefix = clientSecretPrefix,
    webhookSecret = webhookSecret,
    decoratorsUrl = decoratorsUrl,
  )

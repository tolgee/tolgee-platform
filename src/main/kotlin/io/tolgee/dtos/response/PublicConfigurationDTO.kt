package io.tolgee.dtos.response

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.configuration.tolgee.TolgeeProperties

@Suppress("MemberVisibilityCanBePrivate", "unused")
class PublicConfigurationDTO(
  @Schema(hidden = true)
  properties: TolgeeProperties
) {
  val isAuthentication: Boolean = properties.authentication.enabled
  var authMethods: AuthMethodsDTO? = null
  val isPasswordResettable: Boolean
  val isAllowRegistrations: Boolean
  val screenshotsUrl = properties.screenshotsUrl
  val maxUploadFileSize = properties.maxUploadFileSize
  val clientSentryDsn = if (properties.sentry.enabled) properties.sentry.clientDsn else null
  val needsEmailVerification = properties.authentication.needsEmailVerification
  val userCanCreateProjects = properties.authentication.userCanCreateProjects
  val userCanCreateOrganizations = properties.authentication.userCanCreateOrganizations
  val socket = SocketIo(
    enabled = properties.socketIo.enabled,
    port = properties.socketIo.port,
    serverUrl = properties.socketIo.externalUrl,
    allowedTransports = properties.socketIo.allowedTransports
  )
  val appName = properties.appName

  class AuthMethodsDTO(val github: GithubPublicConfigDTO)
  data class GithubPublicConfigDTO(val clientId: String?) {
    val isEnabled: Boolean = clientId != null && clientId.isNotEmpty()
  }

  data class SocketIo(val enabled: Boolean, val port: Int, val serverUrl: String?, val allowedTransports: List<String>)

  init {
    if (isAuthentication) {
      authMethods = AuthMethodsDTO(GithubPublicConfigDTO(properties.authentication.github.clientId))
    }
    isPasswordResettable = properties.authentication.nativeEnabled
    isAllowRegistrations = properties.authentication.registrationsAllowed
  }
}

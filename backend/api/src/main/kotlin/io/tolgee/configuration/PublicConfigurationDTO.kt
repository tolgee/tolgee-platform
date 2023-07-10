package io.tolgee.configuration

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.FileStoragePath
import io.tolgee.constants.MtServiceType
import io.tolgee.dtos.response.PublicBillingConfigurationDTO

@Suppress("MemberVisibilityCanBePrivate", "unused")
class PublicConfigurationDTO(
  @Schema(hidden = true)
  properties: TolgeeProperties,
  val machineTranslationServices: MtServicesDTO,
  val billing: PublicBillingConfigurationDTO,
  val version: String,
) {
  val authentication: Boolean = properties.authentication.enabled
  var authMethods: AuthMethodsDTO? = null
  val passwordResettable: Boolean
  val allowRegistrations: Boolean
  val screenshotsUrl = properties.fileStorageUrl + "/" + FileStoragePath.SCREENSHOTS
  val maxUploadFileSize = properties.maxUploadFileSize
  val clientSentryDsn = properties.sentry.clientDsn
  val needsEmailVerification = properties.authentication.needsEmailVerification
  val userCanCreateOrganizations = properties.authentication.userCanCreateOrganizations
  val appName = properties.appName
  val showVersion: Boolean = properties.internal.showVersion
  val internalControllerEnabled: Boolean = properties.internal.controllerEnabled
  val maxTranslationTextLength: Long = properties.maxTranslationTextLength
  val recaptchaSiteKey = properties.recaptcha.siteKey
  val chatwootToken = properties.chatwootToken
  val capterraTracker = properties.capterraTracker
  val ga4Tag = properties.ga4Tag
  val postHogApiKey: String? = properties.postHog.apiKey
  val postHogHost: String? = properties.postHog.host

  class AuthMethodsDTO(
    val github: OAuthPublicConfigDTO,
    val google: OAuthPublicConfigDTO,
    val oauth2: OAuthPublicExtendsConfigDTO
  )

  data class OAuthPublicConfigDTO(val clientId: String?) {
    val enabled: Boolean = clientId != null && clientId.isNotEmpty()
  }

  data class OAuthPublicExtendsConfigDTO(
    val clientId: String?,
    val authorizationUrl: String?,
    val scopes: List<String>?
  ) {
    val enabled: Boolean = !clientId.isNullOrEmpty()
  }

  data class MtServicesDTO(
    val defaultPrimaryService: MtServiceType?,
    val services: Map<MtServiceType, MtServiceDTO>
  )

  data class MtServiceDTO(
    val enabled: Boolean,
    val defaultEnabledForProject: Boolean
  )

  init {
    if (authentication) {
      authMethods = AuthMethodsDTO(
        OAuthPublicConfigDTO(
          properties.authentication.github.clientId
        ),
        OAuthPublicConfigDTO(properties.authentication.google.clientId),
        OAuthPublicExtendsConfigDTO(
          properties.authentication.oauth2.clientId,
          properties.authentication.oauth2.authorizationUrl,
          properties.authentication.oauth2.scopes
        )
      )
    }
    passwordResettable = properties.authentication.nativeEnabled
    allowRegistrations = properties.authentication.registrationsAllowed
  }
}

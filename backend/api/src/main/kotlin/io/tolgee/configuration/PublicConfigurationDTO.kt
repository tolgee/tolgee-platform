package io.tolgee.configuration

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.FileStoragePath
import io.tolgee.constants.MtServiceType
import io.tolgee.dtos.response.PublicBillingConfigurationDTO

@Suppress("MemberVisibilityCanBePrivate", "unused")
class PublicConfigurationDTO(
  @Schema(hidden = true) properties: TolgeeProperties,
  val machineTranslationServices: MtServicesDTO,
  val billing: PublicBillingConfigurationDTO,
  val version: String,
) {
  val authentication: Boolean = properties.authentication.enabled
  val authMethods: AuthMethodsDTO? = properties.authentication.asAuthMethodsDTO()

  @Deprecated("Use nativeEnabled instead", ReplaceWith("nativeEnabled"))
  val passwordResettable: Boolean = properties.authentication.nativeEnabled
  val allowRegistrations: Boolean = properties.authentication.registrationsAllowed
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
  val nativeEnabled = properties.authentication.nativeEnabled
  val capterraTracker = properties.capterraTracker
  val ga4Tag = properties.ga4Tag
  val postHogApiKey: String? = properties.postHog.apiKey
  val postHogHost: String? = properties.postHog.host
  val contentDeliveryConfigured: Boolean = properties.contentDelivery.publicUrlPrefix != null
  val userSourceField: Boolean = properties.userSourceField
  val plausible: PlausibleDto =
    PlausibleDto(
      properties.plausible.domain,
      properties.plausible.url,
      properties.plausible.scriptUrl,
    )

  val slack: SlackDTO =
    SlackDTO(
      enabled = (
        properties.slack.signingSecret != null &&
          (properties.slack.clientId != null || properties.slack.token != null)
      ),
      connected = properties.slack.token != null,
    )

  companion object {
    private fun AuthenticationProperties.asAuthMethodsDTO(): AuthMethodsDTO? {
      if (!enabled) {
        return null
      }

      return AuthMethodsDTO(
        OAuthPublicConfigDTO(github.clientId),
        OAuthPublicConfigDTO(google.clientId),
        OAuthPublicExtendsConfigDTO(
          oauth2.clientId,
          oauth2.authorizationUrl,
          oauth2.scopes,
        ),
        SsoPublicConfigDTO(
          sso.enabled,
          sso.globalEnabled,
          sso.clientId,
          sso.domain,
          sso.customLogoUrl,
          sso.customLoginText,
        ),
      )
    }
  }

  class AuthMethodsDTO(
    val github: OAuthPublicConfigDTO,
    val google: OAuthPublicConfigDTO,
    val oauth2: OAuthPublicExtendsConfigDTO,
    val sso: SsoPublicConfigDTO,
  )

  data class OAuthPublicConfigDTO(val clientId: String?) {
    val enabled: Boolean = clientId != null && clientId.isNotEmpty()
  }

  data class OAuthPublicExtendsConfigDTO(
    val clientId: String?,
    val authorizationUrl: String?,
    val scopes: List<String>?,
  ) {
    val enabled: Boolean = !clientId.isNullOrEmpty()
  }

  data class SsoPublicConfigDTO(
    val enabled: Boolean,
    val globalEnabled: Boolean,
    val clientId: String?,
    val domain: String?,
    val customLogoUrl: String?,
    val customLoginText: String?,
  )

  data class MtServicesDTO(
    val defaultPrimaryService: MtServiceType?,
    val services: Map<MtServiceType, MtServiceDTO>,
  )

  data class MtServiceDTO(
    val enabled: Boolean,
    val defaultEnabledForProject: Boolean,
  )

  data class SlackDTO(
    val enabled: Boolean,
    val connected: Boolean,
  )
}

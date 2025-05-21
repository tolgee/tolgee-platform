package io.tolgee.api.publicConfiguration

import io.tolgee.configuration.PlausibleDto
import io.tolgee.constants.MtServiceType
import io.tolgee.dtos.response.PublicBillingConfigurationDTO
import io.tolgee.dtos.response.PublicLLMConfigurationDTO

@Suppress("MemberVisibilityCanBePrivate", "unused")
class PublicConfigurationDTO(
  val machineTranslationServices: MtServicesDTO,
  val billing: PublicBillingConfigurationDTO,
  val version: String,
  val authentication: Boolean,
  val authMethods: AuthMethodsDTO?,
  val nativeEnabled: Boolean,
  @Deprecated("Use nativeEnabled instead", ReplaceWith("nativeEnabled"))
  val passwordResettable: Boolean,
  val allowRegistrations: Boolean,
  val screenshotsUrl: String,
  val maxUploadFileSize: Int,
  val clientSentryDsn: String?,
  val needsEmailVerification: Boolean,
  val userCanCreateOrganizations: Boolean,
  val appName: String,
  val showVersion: Boolean,
  val internalControllerEnabled: Boolean,
  val maxTranslationTextLength: Long,
  val recaptchaSiteKey: String?,
  val chatwootToken: String?,
  val capterraTracker: String?,
  val ga4Tag: String?,
  val postHogApiKey: String?,
  val postHogHost: String?,
  val contentDeliveryConfigured: Boolean,
  val userSourceField: Boolean,
  val plausible: PlausibleDto,
  val slack: SlackDTO,
  val translationsViewLanguagesLimit: Int,
  val llm: PublicLLMConfigurationDTO,
) {
  class AuthMethodsDTO(
    val github: OAuthPublicConfigDTO,
    val google: OAuthPublicConfigDTO,
    val oauth2: OAuthPublicExtendsConfigDTO,
    val ssoGlobal: SsoGlobalPublicConfigDTO,
    val ssoOrganizations: SsoOrganizationsPublicConfigDTO,
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

  data class SsoGlobalPublicConfigDTO(
    val enabled: Boolean,
    val clientId: String?,
    val domain: String?,
    val customLogoUrl: String?,
    val customLoginText: String?,
  )

  data class SsoOrganizationsPublicConfigDTO(
    val enabled: Boolean,
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

package io.tolgee.api.publicConfiguration

import io.tolgee.api.publicConfiguration.PublicConfigurationDTO.AuthMethodsDTO
import io.tolgee.api.publicConfiguration.PublicConfigurationDTO.OAuthPublicConfigDTO
import io.tolgee.api.publicConfiguration.PublicConfigurationDTO.OAuthPublicExtendsConfigDTO
import io.tolgee.api.publicConfiguration.PublicConfigurationDTO.SsoGlobalPublicConfigDTO
import io.tolgee.api.publicConfiguration.PublicConfigurationDTO.SsoOrganizationsPublicConfigDTO
import io.tolgee.component.contentDelivery.ContentDeliveryFileStorageProvider
import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.configuration.PlausibleDto
import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.configuration.tolgee.machineTranslation.LLMProperties
import io.tolgee.constants.FileStoragePath
import io.tolgee.constants.MtServiceType
import io.tolgee.dtos.response.PublicLLMConfigurationDTO
import io.tolgee.util.VersionProvider
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class PublicConfigurationAssembler(
  private val properties: TolgeeProperties,
  private val applicationContext: ApplicationContext,
  private val publicBillingConfProvider: PublicBillingConfProvider,
  private val versionProvider: VersionProvider,
  private val contentDeliveryFileStorageProvider: ContentDeliveryFileStorageProvider,
  private val llmProperties: LLMProperties,
) {
  fun toDto(): PublicConfigurationDTO {
    return PublicConfigurationDTO(
      machineTranslationServices =
        PublicConfigurationDTO.MtServicesDTO(
          defaultPrimaryService = getPrimaryMtService(),
          services = getMtServices(),
        ),
      billing = publicBillingConfProvider(),
      version = versionProvider.version,
      authentication = properties.authentication.enabled,
      screenshotsUrl = properties.fileStorageUrl + "/" + FileStoragePath.SCREENSHOTS,
      maxUploadFileSize = properties.maxUploadFileSize,
      clientSentryDsn = properties.sentry.clientDsn,
      needsEmailVerification = properties.authentication.needsEmailVerification,
      userCanCreateOrganizations = properties.authentication.userCanCreateOrganizations,
      appName = properties.appName,
      showVersion = properties.internal.showVersion,
      internalControllerEnabled = properties.internal.controllerEnabled,
      maxTranslationTextLength = properties.maxTranslationTextLength,
      recaptchaSiteKey = properties.recaptcha.siteKey,
      chatwootToken = properties.chatwootToken,
      capterraTracker = properties.capterraTracker,
      ga4Tag = properties.ga4Tag,
      postHogApiKey = properties.postHog.apiKey,
      postHogHost = properties.postHog.host,
      contentDeliveryConfigured = contentDeliveryFileStorageProvider.isServerContentDeliveryConfigured(),
      userSourceField = properties.userSourceField,
      plausible =
        PlausibleDto(
          properties.plausible.domain,
          properties.plausible.url,
          properties.plausible.scriptUrl,
        ),
      slack =
        PublicConfigurationDTO.SlackDTO(
          enabled = (
            properties.slack.signingSecret != null &&
              (properties.slack.clientId != null || properties.slack.token != null)
          ),
          connected = properties.slack.token != null,
        ),
      nativeEnabled = properties.authentication.nativeEnabled,
      passwordResettable = properties.authentication.nativeEnabled,
      allowRegistrations = properties.authentication.registrationsAllowed,
      authMethods = properties.authentication.asAuthMethodsDTO(),
      translationsViewLanguagesLimit = properties.translationsViewLanguagesLimit,
      llm = PublicLLMConfigurationDTO(llmProperties.enabled),
    )
  }

  private fun AuthenticationProperties.asAuthMethodsDTO(): AuthMethodsDTO? {
    if (!enabled) {
      return null
    }

    return AuthMethodsDTO(
      OAuthPublicConfigDTO(
        github.clientId,
      ),
      OAuthPublicConfigDTO(google.clientId),
      OAuthPublicExtendsConfigDTO(
        oauth2.clientId,
        oauth2.authorizationUrl,
        oauth2.scopes,
      ),
      SsoGlobalPublicConfigDTO(
        ssoGlobal.enabled,
        ssoGlobal.clientId,
        ssoGlobal.domain,
        ssoGlobal.customLogoUrl,
        ssoGlobal.customLoginText,
      ),
      SsoOrganizationsPublicConfigDTO(
        ssoOrganizations.enabled,
      ),
    )
  }

  private fun getPrimaryMtService(): MtServiceType? {
    val primaryMtService =
      MtServiceType.entries.find {
        applicationContext.getBean(it.propertyClass).defaultPrimary
      }
    return primaryMtService
  }

  private fun getMtServices(): Map<MtServiceType, PublicConfigurationDTO.MtServiceDTO> {
    val mtServices =
      MtServiceType.entries
        .sortedBy { it.order }
        .associateWith {
          PublicConfigurationDTO.MtServiceDTO(
            applicationContext.getBean(it.providerClass).isEnabled,
            it.propertyClass?.let { applicationContext.getBean(it).defaultEnabled } ?: true,
          )
        }
    return mtServices
  }
}

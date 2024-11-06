package io.tolgee.security.thirdParty

import io.tolgee.configuration.tolgee.SsoGlobalProperties
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.Organization
import io.tolgee.model.SsoTenant
import kotlin.reflect.KProperty0

data class SsoTenantConfig(
  val name: String,
  val clientId: String,
  val clientSecret: String,
  val authorizationUri: String,
  val domain: String,
  val jwkSetUri: String,
  val tokenUri: String,
  val organization: Organization? = null,
  val entity: SsoTenant? = null,
) {
  companion object {
    fun SsoTenant.toConfig(): SsoTenantConfig? {
      if (!enabled) {
        return null
      }

      return SsoTenantConfig(
        name = name,
        clientId = clientId,
        clientSecret = clientSecret,
        authorizationUri = authorizationUri,
        domain = domain,
        jwkSetUri = jwkSetUri,
        tokenUri = tokenUri,
        organization = organization,
        entity = this,
      )
    }

    fun SsoGlobalProperties.toConfig(): SsoTenantConfig? {
      if (!globalEnabled) {
        return null
      }

      return SsoTenantConfig(
        name = "Global SSO Provider",
        clientId = ::clientId.validate(),
        clientSecret = ::clientSecret.validate(),
        authorizationUri = ::authorizationUri.validate(),
        domain = ::domain.validate(),
        jwkSetUri = ::jwkSetUri.validate(),
        tokenUri = ::tokenUri.validate(),
      )
    }

    // TODO: specific message "$name is missing in global SSO configuration",
    private fun <T : Any> KProperty0<T?>.validate(): T =
      this.get() ?: throw BadRequestException(
        Message.SSO_GLOBAL_CONFIG_MISSING_PROPERTIES,
        listOf(name),
      )
  }
}

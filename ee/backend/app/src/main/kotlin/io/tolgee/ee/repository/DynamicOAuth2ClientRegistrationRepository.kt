package io.tolgee.ee.repository

import io.tolgee.ee.data.DynamicOAuth2ClientRegistration
import io.tolgee.ee.model.Tenant
import io.tolgee.ee.service.TenantService
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.stereotype.Component

@Component
class DynamicOAuth2ClientRegistrationRepository(
  private val tenantService: TenantService,
) :
  ClientRegistrationRepository {
  private val dynamicClientRegistrations: MutableMap<String, DynamicOAuth2ClientRegistration> = mutableMapOf()

  override fun findByRegistrationId(registrationId: String): ClientRegistration {
    val tenant: Tenant = tenantService.getByDomain(registrationId)
    val dynamicRegistration = createDynamicClientRegistration(tenant)
    return dynamicRegistration.clientRegistration
  }

  private fun createDynamicClientRegistration(tenant: Tenant): DynamicOAuth2ClientRegistration {
    val clientRegistration =
      ClientRegistration.withRegistrationId(tenant.domain)
        .clientId(tenant.clientId)
        .clientSecret(tenant.clientSecret)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationUri(tenant.authorizationUri)
        .tokenUri(tenant.tokenUri)
        .jwkSetUri(tenant.jwkSetUri)
        .redirectUri(tenant.redirectUriBase + "/openId/auth_callback/" + tenant.domain)
        .scope("openid", "profile", "email")
        .build()

    val dynamicRegistration =
      DynamicOAuth2ClientRegistration(
        tenantId = tenant.id.toString(),
        clientRegistration = clientRegistration,
      )

    dynamicClientRegistrations[tenant.domain] = dynamicRegistration
    return dynamicRegistration
  }

  fun String.toLongOrThrow(): Long {
    return try {
      this.toLong()
    } catch (e: NumberFormatException) {
      throw IllegalArgumentException("Invalid tenant ID: $this")
    }
  }
}

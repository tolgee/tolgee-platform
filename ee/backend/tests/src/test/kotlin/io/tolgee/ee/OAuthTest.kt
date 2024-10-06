package io.tolgee.ee

import io.tolgee.ee.model.Tenant
import io.tolgee.ee.repository.DynamicOAuth2ClientRegistrationRepository
import io.tolgee.ee.service.OAuthService
import io.tolgee.ee.service.TenantService
import io.tolgee.ee.utils.OAuthMultiTenantsMocks
import io.tolgee.testing.AbstractControllerTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.client.RestTemplate

class OAuthTest : AbstractControllerTest() {
  @MockBean
  @Autowired
  private val restTemplate: RestTemplate? = null

  @Autowired
  private var authMvc: MockMvc? = null

  @Autowired
  private lateinit var dynamicOAuth2ClientRegistrationRepository: DynamicOAuth2ClientRegistrationRepository

  @Autowired
  private lateinit var oAuthService: OAuthService

  @Autowired
  private lateinit var tenantService: TenantService

  private val oAuthMultiTenantsMocks: OAuthMultiTenantsMocks by lazy {
    OAuthMultiTenantsMocks(authMvc, restTemplate, dynamicOAuth2ClientRegistrationRepository)
  }

  @Test
  fun authorize() {
    tenantService.save(
      Tenant().apply {
        name = "tenant1"
        domain = "registrationId"
        clientId = "clientId"
        clientSecret = "clientSecret"
        authorizationUri = "authorizationUri"
        jwkSetUri = "jwkSetUri"
        tokenUri = "tokenUri"
        redirectUriBase = "redirectUriBase"
        organizationId = 0L
      },
    )
    val clientRegistraion =
      dynamicOAuth2ClientRegistrationRepository
        .findByRegistrationId("registrationId")
        .clientRegistration
    oAuthMultiTenantsMocks.authorize(clientRegistraion.registrationId)
    val response = oAuthService.exchangeCodeForToken(clientRegistraion, "code", "redirectUrl")
    response
  }
}

package io.tolgee.ee

import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import io.tolgee.development.testDataBuilder.data.OAuthTestData
import io.tolgee.ee.model.SsoTenant
import io.tolgee.ee.service.OAuthService
import io.tolgee.ee.service.TenantService
import io.tolgee.ee.utils.OAuthMultiTenantsMocks
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.client.RestTemplate

class OAuthTest : AbstractControllerTest() {
  private lateinit var testData: OAuthTestData

  @MockBean
  @Autowired
  private val restTemplate: RestTemplate? = null

  @Autowired
  private var authMvc: MockMvc? = null

  @MockBean
  @Autowired
  private val jwtProcessor: ConfigurableJWTProcessor<SecurityContext>? = null

  @Autowired
  private lateinit var oAuthService: OAuthService

  @Autowired
  private lateinit var tenantService: TenantService

  private val oAuthMultiTenantsMocks: OAuthMultiTenantsMocks by lazy {
    OAuthMultiTenantsMocks(authMvc, restTemplate, tenantService, jwtProcessor)
  }

  @BeforeEach
  fun setup() {
    testData = OAuthTestData()
    testDataService.saveTestData(testData.root)
    addTenant()
  }

  private fun addTenant() {
    tenantService.save(
      SsoTenant().apply {
        name = "tenant1"
        domain = "registrationId"
        clientId = "clientId"
        clientSecret = "clientSecret"
        authorizationUri = "authorizationUri"
        jwkSetUri = "http://jwkSetUri"
        tokenUri = "http://tokenUri"
        redirectUriBase = "redirectUriBase"
        organizationId = testData.organization.id
      },
    )
  }

  @Test
  fun authorize() {
    val response = oAuthMultiTenantsMocks.authorize("registrationId")
    assertThat(response.response.status).isEqualTo(200)
  }
}

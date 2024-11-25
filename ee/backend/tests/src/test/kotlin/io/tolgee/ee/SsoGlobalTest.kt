package io.tolgee.ee

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.SsoTestData
import io.tolgee.dtos.request.organization.OrganizationDto
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.data.OAuth2TokenResponse
import io.tolgee.ee.security.thirdParty.SsoDelegateEe
import io.tolgee.ee.service.sso.TenantService
import io.tolgee.ee.utils.SsoMultiTenantsMocks
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.startsWith
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.only
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.web.client.RestTemplate
import java.util.*

@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ],
)
class SsoGlobalTest : AuthorizedControllerTest() {
  private lateinit var testData: SsoTestData

  @MockBean
  @Autowired
  private val restTemplate: RestTemplate? = null

  @Autowired
  private var authMvc: MockMvc? = null

  @Autowired
  private lateinit var ssoDelegate: SsoDelegateEe

  @Autowired
  private lateinit var tenantService: TenantService

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  private val ssoMultiTenantsMocks: SsoMultiTenantsMocks by lazy {
    SsoMultiTenantsMocks(authMvc, restTemplate, tenantService)
  }

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.SSO)
    currentDateProvider.forcedDate = currentDateProvider.date
    tolgeeProperties.authentication.ssoGlobal.apply {
      enabled = true
      domain = "registrationId"
      clientId = "dummy_client_id"
      clientSecret = "clientSecret"
      authorizationUri = "https://dummy-url.com"
      tokenUri = "http://tokenUri"
      jwkSetUri = "http://jwkSetUri"
    }
    testData = SsoTestData(false)
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun tearDown() {
    testDataService.cleanTestData(testData.root)
    tolgeeProperties.authentication.ssoGlobal.apply {
      enabled = false
      domain = ""
      clientId = ""
      clientSecret = ""
      authorizationUri = ""
      tokenUri = ""
      jwkSetUri = ""
    }
    currentDateProvider.forcedDate = null
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  fun `creates new user account and return access token on sso log in`() {
    val response = loginAsSsoUser()
    assertThat(response.response.status).isEqualTo(200)
    val result = jacksonObjectMapper().readValue(response.response.contentAsString, HashMap::class.java)
    result["accessToken"].assert.isNotNull
    result["tokenType"].assert.isEqualTo("Bearer")
    val userName = SsoMultiTenantsMocks.jwtClaimsSet.get("email") as String
    assertThat(userAccountService.get(userName)).isNotNull
  }

  @Test
  fun `does not return auth link when tenant is disabled`() {
    tolgeeProperties.authentication.ssoGlobal.enabled = false
    val response = ssoMultiTenantsMocks.getAuthLink("registrationId").response
    assertThat(response.status).isEqualTo(404)
    assertThat(response.contentAsString).contains(Message.SSO_DOMAIN_NOT_FOUND_OR_DISABLED.code)
  }

  @Test
  fun `does not auth user when tenant is disabled`() {
    tolgeeProperties.authentication.ssoGlobal.enabled = false
    val response = ssoMultiTenantsMocks.authorize("registrationId", tokenUri = testData.tenant.tokenUri)
    assertThat(response.response.status).isEqualTo(404)
    assertThat(response.response.contentAsString).contains(Message.SSO_DOMAIN_NOT_FOUND_OR_DISABLED.code)
  }

  @Test
  fun `new user belongs to organization associated with the sso issuer`() {
    loginAsSsoUser()
    val userName = SsoMultiTenantsMocks.jwtClaimsSet.get("email") as String
    val user = userAccountService.get(userName)
    assertThat(organizationRoleService.isUserOfRole(user.id, testData.organization.id, OrganizationRoleType.MEMBER))
      .isEqualTo(true)
  }

  @Test
  fun `doesn't authorize user when token exchange fails`() {
    val response =
      ssoMultiTenantsMocks.authorize(
        "registrationId",
        ResponseEntity<OAuth2TokenResponse>(null, null, 401),
      )
    assertThat(response.response.status).isEqualTo(401)
    assertThat(response.response.contentAsString).contains(Message.SSO_TOKEN_EXCHANGE_FAILED.code)
    val userName = SsoMultiTenantsMocks.jwtClaimsSet.get("email") as String
    assertThrows<NotFoundException> { userAccountService.get(userName) }
  }

  @Test
  fun `sso auth saves refresh token`() {
    loginAsSsoUser()
    val userName = SsoMultiTenantsMocks.jwtClaimsSet.get("email") as String
    val user = userAccountService.get(userName)
    assertThat(user.ssoRefreshToken).isNotNull
    val managedBy = organizationRoleService.getManagedBy(user.id)
    assertThat(managedBy).isNotNull
    assertThat(user.thirdPartyAuthType?.code()).isEqualTo("sso")
  }

  @Test
  fun `user account available validation works`() {
    loginAsSsoUser()
    val userName = SsoMultiTenantsMocks.jwtClaimsSet.get("email") as String
    val user = userAccountService.get(userName)
    val userDto = userAccountService.getDto(user.id)
    assertThat(
      ssoDelegate.verifyUserSsoAccountAvailable(
        userDto,
      ),
    ).isTrue
  }

  @Test
  fun `after timeout should call token endpoint `() {
    loginAsSsoUser()
    clearInvocations(restTemplate)
    val userName = SsoMultiTenantsMocks.jwtClaimsSet.get("email") as String
    val user = userAccountService.get(userName)
    val userDto = userAccountService.getDto(user.id)
    currentDateProvider.forcedDate = Date(currentDateProvider.date.time + 600_000)

    assertThat(
      ssoDelegate.verifyUserSsoAccountAvailable(
        userDto,
      ),
    ).isTrue

    verify(restTemplate, only())?.exchange(
      startsWith("http://tokenUri"),
      eq(HttpMethod.POST),
      any(HttpEntity::class.java),
      eq(OAuth2TokenResponse::class.java),
    )
  }

  @Test
  fun `sso auth works via global config`() {
    val response = ssoMultiTenantsMocks.authorize("registrationId")

    val result = jacksonObjectMapper().readValue(response.response.contentAsString, HashMap::class.java)
    result["accessToken"].assert.isNotNull
    result["tokenType"].assert.isEqualTo("Bearer")
  }

  fun organizationDto() =
    OrganizationDto(
      "Test org",
      "This is description",
      "test-org",
    )

  fun loginAsSsoUser(
    tokenResponse: ResponseEntity<OAuth2TokenResponse>? = SsoMultiTenantsMocks.defaultTokenResponse,
  ): MvcResult {
    return ssoMultiTenantsMocks.authorize("registrationId", tokenResponse = tokenResponse)
  }
}

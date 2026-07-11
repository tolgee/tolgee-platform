package io.tolgee.ee

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.SsoTestData
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.dtos.request.organization.OrganizationDto
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.data.OAuth2TokenResponse
import io.tolgee.ee.security.thirdParty.SsoDelegateEe
import io.tolgee.ee.utils.SsoMultiTenantsMocks
import io.tolgee.exceptions.NotFoundException
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.service.TenantService
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
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.web.client.RestTemplate
import java.util.Date
import java.util.HashMap

@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ],
)
class SsoGlobalTest : AuthorizedControllerTest() {
  private lateinit var testData: SsoTestData

  @MockitoBean
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
    tolgeeProperties.authentication.ssoGlobal.enabled = true
    testData = SsoTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun tearDown() {
    testDataService.cleanTestData(testData.root)
    tolgeeProperties.authentication.ssoGlobal.enabled = false
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
    assertThat(userAccountService.findActive(userName)).isNotNull
  }

  @Test
  fun `does not return auth link when tenant is disabled`() {
    tolgeeProperties.authentication.ssoGlobal.enabled = false
    val response = ssoMultiTenantsMocks.getAuthLink("domain.com").response
    assertThat(response.status).isEqualTo(404)
    assertThat(response.contentAsString).contains(Message.SSO_DOMAIN_NOT_FOUND_OR_DISABLED.code)
  }

  @Test
  fun `does not auth user when tenant is disabled`() {
    tolgeeProperties.authentication.ssoGlobal.enabled = false
    val response =
      ssoMultiTenantsMocks.authorize(
        "domain.com",
        tokenUri = tolgeeProperties.authentication.ssoGlobal.tokenUri,
      )
    assertThat(response.response.status).isEqualTo(404)
    assertThat(response.response.contentAsString).contains(Message.SSO_DOMAIN_NOT_FOUND_OR_DISABLED.code)
  }

  @Test
  fun `doesn't authorize user when token exchange fails`() {
    val response =
      ssoMultiTenantsMocks.authorize(
        "domain.com",
        ResponseEntity<OAuth2TokenResponse>(null, null, 401),
      )
    assertThat(response.response.status).isEqualTo(401)
    assertThat(response.response.contentAsString).contains(Message.SSO_TOKEN_EXCHANGE_FAILED.code)
    val userName = SsoMultiTenantsMocks.jwtClaimsSet.get("email") as String
    assertThrows<NotFoundException> { userAccountService.get(userName) }
  }

  @Test
  fun `doesn't allow sign up when enabled for domain`() {
    val dto =
      SignUpDto(
        name = "Pavel Novak",
        password = "aaaaaaaaa",
        email = "aaaa@domain.com",
        organizationName = "Jejda",
      )
    performPost("/api/public/sign_up", dto).andIsUnauthorized
  }

  @Test
  fun `sso auth saves refresh token`() {
    loginAsSsoUser()
    val userName = SsoMultiTenantsMocks.jwtClaimsSet.get("email") as String
    val user = userAccountService.get(userName)
    assertThat(user.ssoRefreshToken).isNotNull
    assertThat(user.thirdPartyAuthType?.code()).isEqualTo("sso_global")
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
    val response = ssoMultiTenantsMocks.authorize("domain.com")

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
    return ssoMultiTenantsMocks.authorize("domain.com", tokenResponse = tokenResponse)
  }
}

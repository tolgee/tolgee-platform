package io.tolgee.ee

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import io.tolgee.configuration.tolgee.SsoGlobalProperties
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.OAuthTestData
import io.tolgee.dtos.request.organization.OrganizationDto
import io.tolgee.ee.data.OAuth2TokenResponse
import io.tolgee.ee.security.thirdParty.SsoDelegateEe
import io.tolgee.ee.service.sso.TenantService
import io.tolgee.ee.utils.OAuthMultiTenantsMocks
import io.tolgee.ee.utils.OAuthMultiTenantsMocks.Companion.jwtClaimsSet
import io.tolgee.exceptions.NotFoundException
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.model.SsoTenant
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import jakarta.transaction.Transactional
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.times
import org.mockito.kotlin.clearInvocations
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
class OAuthTest : AuthorizedControllerTest() {
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
  private lateinit var ssoDelegate: SsoDelegateEe

  @Autowired
  private lateinit var tenantService: TenantService

  @Autowired
  lateinit var ssoGlobalProperties: SsoGlobalProperties

  private val oAuthMultiTenantsMocks: OAuthMultiTenantsMocks by lazy {
    OAuthMultiTenantsMocks(authMvc, restTemplate, tenantService, jwtProcessor)
  }

  @BeforeEach
  fun setup() {
    currentDateProvider.forcedDate = currentDateProvider.date
    ssoGlobalProperties.enabled = false
    testData = OAuthTestData()
    testDataService.saveTestData(testData.root)
  }

  private fun addTenant(): SsoTenant =
    tenantService.findTenant(testData.organization.id)
      ?: SsoTenant()
        .apply {
          name = "tenant1"
          domain = "registrationId"
          clientId = "clientId"
          clientSecret = "clientSecret"
          authorizationUri = "authorizationUri"
          jwkSetUri = "http://jwkSetUri"
          tokenUri = "http://tokenUri"
          organization = testData.organization
        }.let { tenantService.save(it) }

  @Test
  fun `creates new user account and return access token on sso log in`() {
    val response = loginAsSsoUser()
    assertThat(response.response.status).isEqualTo(200)
    val result = jacksonObjectMapper().readValue(response.response.contentAsString, HashMap::class.java)
    result["accessToken"].assert.isNotNull
    result["tokenType"].assert.isEqualTo("Bearer")
    val userName = jwtClaimsSet.getStringClaim("email")
    assertThat(userAccountService.get(userName)).isNotNull
  }

  @Test
  fun `does not return auth link when tenant is disabled`() {
    val tenant = addTenant()
    tenant.enabled = false
    tenantService.save(tenant)
    val response = oAuthMultiTenantsMocks.getAuthLink("registrationId").response
    assertThat(response.status).isEqualTo(404)
    assertThat(response.contentAsString).contains(Message.SSO_DOMAIN_NOT_FOUND_OR_DISABLED.code)
  }

  @Test
  fun `does not auth user when tenant is disabled`() {
    val tenant = addTenant()
    tenant.enabled = false
    tenantService.save(tenant)
    val response = oAuthMultiTenantsMocks.authorize("registrationId")
    assertThat(response.response.status).isEqualTo(404)
    assertThat(response.response.contentAsString).contains(Message.SSO_DOMAIN_NOT_FOUND_OR_DISABLED.code)
  }

  @Test
  fun `new user belongs to organization associated with the sso issuer`() {
    loginAsSsoUser()
    val userName = jwtClaimsSet.getStringClaim("email")
    val user = userAccountService.get(userName)
    assertThat(organizationRoleService.isUserOfRole(user.id, testData.organization.id, OrganizationRoleType.MEMBER))
      .isEqualTo(true)
  }

  @Test
  fun `doesn't authorize user when token exchange fails`() {
    addTenant()
    val response =
      oAuthMultiTenantsMocks.authorize(
        "registrationId",
        ResponseEntity<OAuth2TokenResponse>(null, null, 400),
      )
    assertThat(response.response.status).isEqualTo(400)
    assertThat(response.response.contentAsString).contains(Message.SSO_TOKEN_EXCHANGE_FAILED.code)
    val userName = jwtClaimsSet.getStringClaim("email")
    assertThrows<NotFoundException> { userAccountService.get(userName) }
  }

  @Transactional
  @Test
  fun `sso auth doesn't create demo project and user organization`() {
    loginAsSsoUser(jwtClaims = OAuthMultiTenantsMocks.jwtClaimsSet2)
    val user = userAccountService.get("mai2@mail.com")
    assertThat(user.organizationRoles.size).isEqualTo(1)
    assertThat(user.organizationRoles[0].organization?.id).isEqualTo(testData.organization.id)
  }

  @Transactional
  @Test
  fun `sso user can't create organization`() {
    loginAsSsoUser()
    val userName = jwtClaimsSet.getStringClaim("email")
    val user = userAccountService.get(userName)
    loginAsUser(user)
    performAuthPost(
      "/v2/organizations",
      organizationDto(),
    ).andIsForbidden
  }

  @Test
  fun `sso auth saves refresh token`() {
    loginAsSsoUser()
    val userName = jwtClaimsSet.getStringClaim("email")
    val user = userAccountService.get(userName)
    assertThat(user.ssoRefreshToken).isNotNull
    assertThat(user.ssoTenant).isNotNull
    assertThat(user.thirdPartyAuthType?.code()).isEqualTo("sso")
  }

  @Test
  fun `user is employee validation works`() {
    loginAsSsoUser()
    val userName = jwtClaimsSet.getStringClaim("email")
    val user = userAccountService.get(userName)
    assertThat(
      ssoDelegate.verifyUserSsoAccountAvailable(
        user.ssoTenant?.domain,
        user.id,
        user.ssoRefreshToken,
        user.thirdPartyAuthType!!,
        user.ssoSessionExpiry,
      ),
    ).isTrue
  }

  @Test
  fun `after timeout should call token endpoint `() {
    clearInvocations(restTemplate)
    loginAsSsoUser()
    val userName = jwtClaimsSet.getStringClaim("email")
    val user = userAccountService.get(userName)
    currentDateProvider.forcedDate = Date(currentDateProvider.date.time + 600_000)

    oAuthMultiTenantsMocks.mockTokenExchange("http://tokenUri")
    assertThat(
      ssoDelegate.verifyUserSsoAccountAvailable(
        user.ssoTenant?.domain,
        user.id,
        user.ssoRefreshToken,
        user.thirdPartyAuthType!!,
        user.ssoSessionExpiry,
      ),
    ).isTrue

    // first call is in loginAsSsoUser
    verify(restTemplate, times(2))?.exchange(
      anyString(),
      eq(HttpMethod.POST),
      any(HttpEntity::class.java),
      eq(OAuth2TokenResponse::class.java),
    )
  }

  @Test
  fun `sso auth works via global config`() {
    ssoGlobalProperties.enabled = true
    ssoGlobalProperties.domain = "registrationId"
    ssoGlobalProperties.clientId = "clientId"
    ssoGlobalProperties.clientSecret = "clientSecret"
    ssoGlobalProperties.authorizationUri = "authorizationUri"
    ssoGlobalProperties.tokenUri = "http://tokenUri"
    ssoGlobalProperties.jwkSetUri = "http://jwkSetUri"
    val response = oAuthMultiTenantsMocks.authorize("registrationId")

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

  fun loginAsSsoUser(jwtClaims: JWTClaimsSet = jwtClaimsSet): MvcResult {
    addTenant()
    return oAuthMultiTenantsMocks.authorize("registrationId", jwtClaims = jwtClaims)
  }
}

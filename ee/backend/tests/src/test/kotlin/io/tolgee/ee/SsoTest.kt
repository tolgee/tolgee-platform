package io.tolgee.ee

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.jsonwebtoken.Claims
import io.tolgee.configuration.tolgee.SsoGlobalProperties
import io.tolgee.configuration.tolgee.SsoOrganizationsProperties
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
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.startsWith
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
class SsoTest : AuthorizedControllerTest() {
  // TODO: separate version of these tests for global sso?
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

  @Autowired
  lateinit var ssoGlobalProperties: SsoGlobalProperties

  @Autowired
  lateinit var ssoOrganizationsProperties: SsoOrganizationsProperties

  private val ssoMultiTenantsMocks: SsoMultiTenantsMocks by lazy {
    SsoMultiTenantsMocks(authMvc, restTemplate, tenantService)
  }

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.SSO)

    currentDateProvider.forcedDate = currentDateProvider.date
    ssoGlobalProperties.enabled = false
    testData = SsoTestData()
    testDataService.saveTestData(testData.root)
  }

  private fun addTenant(): SsoTenant {
    ssoOrganizationsProperties.enabled = true
    return tenantService.findTenant(testData.organization.id)
      ?: SsoTenant()
        .apply {
          domain = "registrationId"
          clientId = "clientId"
          clientSecret = "clientSecret"
          authorizationUri = "authorizationUri"
          jwkSetUri = "http://jwkSetUri"
          tokenUri = "http://tokenUri"
          organization = testData.organization
        }.let { tenantService.save(it) }
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
    val tenant = addTenant()
    tenant.enabled = false
    tenantService.save(tenant)
    val response = ssoMultiTenantsMocks.getAuthLink("registrationId").response
    assertThat(response.status).isEqualTo(404)
    assertThat(response.contentAsString).contains(Message.SSO_DOMAIN_NOT_FOUND_OR_DISABLED.code)
  }

  @Test
  fun `does not auth user when tenant is disabled`() {
    val tenant = addTenant()
    tenant.enabled = false
    tenantService.save(tenant)
    val response = ssoMultiTenantsMocks.authorize("registrationId", tokenUri = tenant.tokenUri)
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
    addTenant()
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

  @Transactional
  @Test
  fun `sso auth doesn't create demo project and user organization`() {
    loginAsSsoUser(
      tokenResponse = SsoMultiTenantsMocks.defaultTokenResponse2,
      jwtClaims = SsoMultiTenantsMocks.jwtClaimsSet2
    )
    val userName = SsoMultiTenantsMocks.jwtClaimsSet2.get("email") as String
    val user = userAccountService.get(userName)
    assertThat(user.organizationRoles.size).isEqualTo(1)
    assertThat(user.organizationRoles[0].organization?.id).isEqualTo(testData.organization.id)
    assertThat(user.organizationRoles[0].managed).isTrue
  }

  @Transactional
  @Test
  fun `sso user can't create organization`() {
    loginAsSsoUser()
    val userName = SsoMultiTenantsMocks.jwtClaimsSet.get("email") as String
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

    // ssoMultiTenantsMocks.mockTokenExchange("http://tokenUri")
    assertThat(
      ssoDelegate.verifyUserSsoAccountAvailable(
        userDto,
      ),
    ).isTrue

    verify(restTemplate, times(1))?.exchange(
      startsWith("http://tokenUri"),
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
    jwtClaims: Claims = SsoMultiTenantsMocks.jwtClaimsSet
  ): MvcResult {
    addTenant()
    return ssoMultiTenantsMocks.authorize("registrationId", tokenResponse = tokenResponse, jwtClaims = jwtClaims)
  }
}

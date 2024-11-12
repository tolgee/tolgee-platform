package io.tolgee

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import io.tolgee.configuration.tolgee.SsoGlobalProperties
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.ChangeAuthTypeTestData
import io.tolgee.dtos.request.AuthProviderChangeRequestDto
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.service.sso.TenantService
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.service.AuthProviderChangeRequestService
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.GitHubAuthUtil
import io.tolgee.util.GoogleAuthUtil
import io.tolgee.util.SsoAuthUtil
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.client.RestTemplate

class ChangeAuthTypeTest : AbstractControllerTest() {
  @MockBean
  @Autowired
  private val restTemplate: RestTemplate? = null

  @Autowired
  private var authMvc: MockMvc? = null

  lateinit var changeAuthTypeTestData: ChangeAuthTypeTestData

  val objectMapper = jacksonObjectMapper()

  @Autowired
  private lateinit var authProviderChangeRequestService: AuthProviderChangeRequestService

  @Autowired
  lateinit var ssoGlobalProperties: SsoGlobalProperties

  @Autowired
  private lateinit var tenantService: TenantService

  @MockBean
  @Autowired
  private val jwtProcessor: ConfigurableJWTProcessor<SecurityContext>? = null

  private val gitHubAuthUtil: GitHubAuthUtil by lazy { GitHubAuthUtil(tolgeeProperties, authMvc, restTemplate) }
  private val googleAuthUtil: GoogleAuthUtil by lazy { GoogleAuthUtil(tolgeeProperties, authMvc, restTemplate) }
  private val ssoUtil: SsoAuthUtil by lazy { SsoAuthUtil(authMvc, restTemplate, tenantService, jwtProcessor) }

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeAll
  fun init() {
    changeAuthTypeTestData = ChangeAuthTypeTestData()
    testDataService.saveTestData(changeAuthTypeTestData.root)
    enabledFeaturesProvider.forceEnabled = setOf(Feature.SSO)
  }

  @Test
  fun `throws and creates new request to change provider`() {
    gitHubAuthUtil.authorizeGithubUser()
    val googleResponse =
      googleAuthUtil
        .authorizeGoogleUser(
          userResponse =
            ResponseEntity(
              googleAuthUtil.userResponseWithExisingEmail,
              HttpStatus.OK,
            ),
        ).response
    Assertions.assertThat(googleResponse.status).isEqualTo(401)
    Assertions.assertThat(googleResponse.contentAsString).contains(Message.USERNAME_ALREADY_EXISTS.code)
    val errorResponse: ErrorResponse = objectMapper.readValue(googleResponse.contentAsString)
    errorResponse.params[0].let {
      val request = authProviderChangeRequestService.findById(it).get()
      Assertions.assertThat(request).isNotNull
    }
  }

  @Test
  fun `change provider from GitHub to Google if request exists and confirmed by user`() {
    gitHubAuthUtil.authorizeGithubUser()
    var user = userAccountService.get(googleAuthUtil.userResponseWithExisingEmail.email!!)
    assertThat(user.thirdPartyAuthType).isEqualTo(ThirdPartyAuthType.GITHUB)

    val googleResponse =
      googleAuthUtil
        .authorizeGoogleUser(
          userResponse =
            ResponseEntity(
              googleAuthUtil.userResponseWithExisingEmail,
              HttpStatus.OK,
            ),
        ).response
    val errorResponse: ErrorResponse = objectMapper.readValue(googleResponse.contentAsString)
    val requestId = errorResponse.params[0]
    authProviderChangeRequestService.confirmOrCancel(
      AuthProviderChangeRequestDto(
        changeRequestId = requestId,
        isConfirmed = true,
      ),
    )
    assertThat(authProviderChangeRequestService.getById(requestId).isConfirmed).isTrue

    val successResponse = gitHubAuthUtil.authorizeGithubUser().response
    assertThat(successResponse.status).isEqualTo(200)

    user = userAccountService.get(googleAuthUtil.userResponseWithExisingEmail.email!!)
    assertThat(user.thirdPartyAuthType).isEqualTo(ThirdPartyAuthType.GOOGLE)
    assertThat(user.accountType).isEqualTo(UserAccount.AccountType.THIRD_PARTY)
  }

  @Test
  fun `change provider from Google to GitHub if request exists and confirmed by user`() {
    googleAuthUtil.authorizeGoogleUser(
      userResponse =
        ResponseEntity(
          googleAuthUtil.userResponseWithExisingEmail,
          HttpStatus.OK,
        ),
    )
    var user = userAccountService.get(googleAuthUtil.userResponseWithExisingEmail.email!!)
    assertThat(user.thirdPartyAuthType).isEqualTo(ThirdPartyAuthType.GOOGLE)

    val githubResponse = gitHubAuthUtil.authorizeGithubUser().response
    val errorResponse: ErrorResponse = objectMapper.readValue(githubResponse.contentAsString)
    val requestId = errorResponse.params[0]
    authProviderChangeRequestService.confirmOrCancel(
      AuthProviderChangeRequestDto(
        changeRequestId = requestId,
        isConfirmed = true,
      ),
    )
    assertThat(authProviderChangeRequestService.getById(requestId).isConfirmed).isTrue

    val successResponse =
      googleAuthUtil
        .authorizeGoogleUser(
          userResponse =
            ResponseEntity(
              googleAuthUtil.userResponseWithExisingEmail,
              HttpStatus.OK,
            ),
        ).response
    assertThat(successResponse.status).isEqualTo(200)

    user = userAccountService.get(googleAuthUtil.userResponseWithExisingEmail.email!!)
    assertThat(user.thirdPartyAuthType).isEqualTo(ThirdPartyAuthType.GITHUB)
    assertThat(user.accountType).isEqualTo(UserAccount.AccountType.THIRD_PARTY)
  }

  @Test
  fun `change provider from Google to Sso if request exists and confirmed by user`() {
    val domain = "sso.com"
    setSso(domain)
    googleAuthUtil.authorizeGoogleUser(
      userResponse =
        ResponseEntity(
          googleAuthUtil.userResponseWithExisingEmail,
          HttpStatus.OK,
        ),
    )
    var user = userAccountService.get(googleAuthUtil.userResponseWithExisingEmail.email!!)
    assertThat(user.thirdPartyAuthType).isEqualTo(ThirdPartyAuthType.GOOGLE)

    val ssoResponse = ssoUtil.authorize(domain, jwtClaims = SsoAuthUtil.jwtClaimsWithExistingUserSet).response
    val errorResponse: ErrorResponse = objectMapper.readValue(ssoResponse.contentAsString)
    val requestId = errorResponse.params[0]
    authProviderChangeRequestService.confirmOrCancel(
      AuthProviderChangeRequestDto(
        changeRequestId = requestId,
        isConfirmed = true,
      ),
    )
    assertThat(authProviderChangeRequestService.getById(requestId).isConfirmed).isTrue

    val successResponse =
      googleAuthUtil
        .authorizeGoogleUser(
          userResponse =
            ResponseEntity(
              googleAuthUtil.userResponseWithExisingEmail,
              HttpStatus.OK,
            ),
        ).response
    assertThat(successResponse.status).isEqualTo(200)

    user = userAccountService.get(googleAuthUtil.userResponseWithExisingEmail.email!!)
    assertThat(user.thirdPartyAuthType).isEqualTo(ThirdPartyAuthType.SSO_GLOBAL)
    assertThat(user.accountType).isEqualTo(UserAccount.AccountType.THIRD_PARTY)
    //assertThat(user.ssoTenant?.domain).isEqualTo(domain)
    assertThat(user.ssoSessionExpiry).isNotNull()
    assertThat(user.ssoRefreshToken).isNotNull()
  }

  @Test
  fun `change provider from native to Sso if request exists and confirmed by user`() {
    val domain = "sso.com"
    setSso(domain)

    testDataService.saveTestData(changeAuthTypeTestData.createUserExisting)
    doAuthentication(changeAuthTypeTestData.userExisting.username, "admin")
    val ssoResponse = ssoUtil.authorize(domain, jwtClaims = SsoAuthUtil.jwtClaimsWithExistingUserSet).response
    val errorResponse: ErrorResponse = objectMapper.readValue(ssoResponse.contentAsString)
    val requestId = errorResponse.params[0]
    authProviderChangeRequestService.confirmOrCancel(
      AuthProviderChangeRequestDto(
        changeRequestId = requestId,
        isConfirmed = true,
      ),
    )
    assertThat(authProviderChangeRequestService.getById(requestId).isConfirmed).isTrue

    doAuthentication(changeAuthTypeTestData.userExisting.username, "admin")

    val user = userAccountService.get(changeAuthTypeTestData.userExisting.username)
    assertThat(user.thirdPartyAuthType).isEqualTo(ThirdPartyAuthType.SSO_GLOBAL)
    assertThat(user.accountType).isEqualTo(UserAccount.AccountType.THIRD_PARTY)
    //assertThat(user.ssoTenant?.domain).isEqualTo(domain)
    assertThat(user.ssoSessionExpiry).isNotNull()
    assertThat(user.ssoRefreshToken).isNotNull()
  }

  private fun setSso(domain: String) {
    ssoGlobalProperties.enabled = true
    ssoGlobalProperties.domain = domain
    ssoGlobalProperties.clientId = "clientId"
    ssoGlobalProperties.clientSecret = "clientSecret"
    ssoGlobalProperties.authorizationUri = "authorizationUri"
    ssoGlobalProperties.tokenUri = "http://tokenUri"
    ssoGlobalProperties.jwkSetUri = "http://jwkSetUri"
  }

  data class ErrorResponse(
    val code: String,
    val params: List<Long>,
  )
}

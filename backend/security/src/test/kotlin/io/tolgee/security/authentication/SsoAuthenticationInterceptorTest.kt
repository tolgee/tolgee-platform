package io.tolgee.security.authentication

import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.service.TenantService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

class SsoAuthenticationInterceptorTest {
  private val authenticationFacade = Mockito.mock(AuthenticationFacade::class.java)

  private val userAccount = Mockito.mock(UserAccountDto::class.java)

  private val tolgeeProperties = Mockito.mock(TolgeeProperties::class.java)

  private val authenticationProperties = Mockito.mock(AuthenticationProperties::class.java)

  private val internalProperties = Mockito.mock(InternalProperties::class.java)

  private val tenantService = Mockito.mock(TenantService::class.java)

  private val ssoAuthenticationInterceptor =
    SsoAuthenticationInterceptor(authenticationFacade, tolgeeProperties, tenantService)

  private val mockMvc =
    MockMvcBuilders
      .standaloneSetup(TestController::class.java)
      .addInterceptors(ssoAuthenticationInterceptor)
      .build()

  @BeforeEach
  fun setupMocks() {
    Mockito.`when`(tolgeeProperties.authentication).thenReturn(authenticationProperties)
    Mockito.`when`(tolgeeProperties.internal).thenReturn(internalProperties)
    Mockito.`when`(authenticationProperties.enabled).thenReturn(true)
    Mockito.`when`(internalProperties.verifySsoAccountAvailableBypass).thenReturn(null)
    Mockito.`when`(authenticationFacade.authenticatedUser).thenReturn(userAccount)
    Mockito.`when`(authenticationFacade.isAuthenticated).thenReturn(true)
    Mockito.`when`(userAccount.username).thenReturn("user@domain.com")
    Mockito.`when`(userAccount.domain).thenReturn("domain.com")
    Mockito.`when`(userAccount.thirdPartyAuth).thenReturn(null)
    Mockito.`when`(tenantService.isSsoForcedForDomain("domain.com")).thenReturn(true)
  }

  @AfterEach
  fun resetMocks() {
    Mockito.reset(tenantService, userAccount)
  }

  @Test
  fun `it rejects access if sso is forced for the user`() {
    mockMvc.perform(get("/no-annotation")).andIsForbidden
  }

  @Test
  fun `it allows access when sso is not forced for the user`() {
    Mockito.`when`(tenantService.isSsoForcedForDomain("domain.com")).thenReturn(false)
    mockMvc.perform(get("/no-annotation")).andIsOk
  }

  @Test
  fun `it allows access when annotated by email verification bypass`() {
    mockMvc.perform(get("/sso-bypass")).andIsOk
  }

  @Test
  fun `it allows access for user using sso authentication`() {
    Mockito.`when`(userAccount.thirdPartyAuth).thenReturn(ThirdPartyAuthType.SSO)
    mockMvc.perform(get("/no-annotation")).andIsOk
  }

  @Test
  fun `it allows access for user using sso global authentication`() {
    Mockito.`when`(userAccount.thirdPartyAuth).thenReturn(ThirdPartyAuthType.SSO_GLOBAL)
    mockMvc.perform(get("/no-annotation")).andIsOk
  }

  @Test
  fun `it allows access for user with invalid email as username`() {
    Mockito.`when`(tenantService.isSsoForcedForDomain(any())).thenReturn(true)
    Mockito.`when`(userAccount.username).thenReturn("user")
    Mockito.`when`(userAccount.domain).thenReturn(null)
    mockMvc.perform(get("/no-annotation")).andIsOk
  }

  @RestController
  class TestController {
    @GetMapping("/no-annotation")
    fun noAnnotation(): String = "hello!"

    @GetMapping("/sso-bypass")
    @BypassForcedSsoAuthentication
    fun emailBypass(): String = "hello!"
  }
}

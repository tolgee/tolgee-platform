package io.tolgee.security.authentication

import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.service.EmailVerificationService
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

class AuthenticationInterceptorTest {
  private val authenticationFacade = Mockito.mock(AuthenticationFacade::class.java)

  private val userAccount = Mockito.mock(UserAccountDto::class.java)

  private val authenticationProperties = Mockito.mock(AuthenticationProperties::class.java)

  private val emailVerificationService = Mockito.mock(EmailVerificationService::class.java)

  private val tenantService = Mockito.mock(TenantService::class.java)

  private val authenticationInterceptor =
    AuthenticationInterceptor(authenticationFacade, authenticationProperties, emailVerificationService, tenantService)

  private val mockMvc =
    MockMvcBuilders.standaloneSetup(TestController::class.java)
      .addInterceptors(authenticationInterceptor)
      .build()

  @BeforeEach
  fun setupMocks() {
    Mockito.`when`(authenticationProperties.enabled).thenReturn(true)
    Mockito.`when`(authenticationFacade.authenticatedUser).thenReturn(userAccount)
    Mockito.`when`(authenticationFacade.isApiAuthentication).thenReturn(false)
    Mockito.`when`(authenticationFacade.isUserSuperAuthenticated).thenReturn(false)
    Mockito.`when`(userAccount.needsSuperJwt).thenReturn(true)
    Mockito.`when`(emailVerificationService.isVerified(any<UserAccountDto>())).thenReturn(true)
    Mockito.`when`(tenantService.isSsoForcedForDomain(any())).thenReturn(false)
  }

  @AfterEach
  fun resetMocks() {
    Mockito.reset(authenticationFacade, userAccount)
  }

  @Test
  fun `it doesn't interfere with basic endpoints`() {
    mockMvc.perform(get("/no-annotation")).andIsOk
  }

  @Test
  fun `it doesn't allow API key authentication by default`() {
    Mockito.`when`(authenticationFacade.isApiAuthentication).thenReturn(true)
    mockMvc.perform(get("/no-annotation")).andIsForbidden
    mockMvc.perform(get("/allow-api-access")).andIsOk
  }

  @Test
  fun `it enforces the super JWT requirement`() {
    mockMvc.perform(get("/requires-super-auth")).andIsForbidden
    Mockito.`when`(authenticationFacade.isUserSuperAuthenticated).thenReturn(true)
    mockMvc.perform(get("/requires-super-auth")).andIsOk

    Mockito.`when`(authenticationFacade.isUserSuperAuthenticated).thenReturn(false)
    Mockito.`when`(userAccount.needsSuperJwt).thenReturn(false)
    mockMvc.perform(get("/requires-super-auth")).andIsOk
  }

  @Test
  fun `it ignores super JWT requirement when authentication is disabled`() {
    mockMvc.perform(get("/requires-super-auth")).andIsForbidden
    Mockito.`when`(authenticationProperties.enabled).thenReturn(false)
    mockMvc.perform(get("/requires-super-auth")).andIsOk
  }

  @Test
  fun `rejects access if the user does not have a verified email`() {
    Mockito.`when`(emailVerificationService.isVerified(any<UserAccountDto>())).thenReturn(false)
    mockMvc.perform(get("/email-verified")).andIsForbidden
  }

  @Test
  fun `not throw when annotated by email verification bypass`() {
    Mockito.`when`(emailVerificationService.isVerified(any<UserAccountDto>())).thenReturn(false)
    mockMvc.perform(get("/email-bypass")).andIsOk
  }

  @RestController
  class TestController {
    @GetMapping("/no-annotation")
    fun noAnnotation(): String = "hello!"

    @GetMapping("/allow-api-access")
    @AllowApiAccess
    fun allowApiAccess(): String = "hello!"

    @GetMapping("/requires-super-auth")
    @RequiresSuperAuthentication
    fun superAuth(): String = "hello!"

    @GetMapping("/email-bypass")
    @BypassEmailVerification
    fun emailBypass(): String = "hello!"

    @GetMapping("/email-verified")
    fun emailVerified(): String = "hello!"
  }
}

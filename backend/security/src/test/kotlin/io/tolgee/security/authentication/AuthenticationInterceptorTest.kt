package io.tolgee.security.authentication

import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

class AuthenticationInterceptorTest {
  private val authenticationFacade = Mockito.mock(AuthenticationFacade::class.java)

  private val userAccount = Mockito.mock(UserAccountDto::class.java)

  private val authenticationProperties = Mockito.mock(AuthenticationProperties::class.java)

  private val authenticationInterceptor = AuthenticationInterceptor(authenticationFacade, authenticationProperties)

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
  }
}

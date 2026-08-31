package io.tolgee.security.authentication

import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.security.authorization.IsGlobalRoute
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
    MockMvcBuilders
      .standaloneSetup(TestController::class.java)
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
  fun `it allows an OAuth token on a project-scoped path`() {
    oauthAuthenticated()

    mockMvc.perform(get("/v2/projects/1/allow-api-access")).andIsOk
  }

  @Test
  fun `it refuses an OAuth token outside the project-scoped paths`() {
    oauthAuthenticated()

    mockMvc.perform(get("/allow-api-access")).andIsForbidden
  }

  @Test
  fun `it refuses an OAuth token on a project-scoped path restricted to one API token kind`() {
    // ONLY_PAK/ONLY_PAT say the endpoint was written for that one credential; an OAuth token is not it, whatever the
    // path would otherwise permit.
    oauthAuthenticated()

    mockMvc.perform(get("/v2/projects/1/only-pak")).andIsForbidden
  }

  @Test
  fun `an endpoint opened to OAuth that no OAuth token could reach still refuses the token`() {
    // The mis-annotation itself is caught at build time by AllowOAuthAccessAnnotationTest; at request time refusing
    // is the right answer either way, so this must not become a 500.
    oauthAuthenticated()

    mockMvc.perform(get("/v2/projects/1/only-pak-opened-to-oauth")).andIsForbidden
  }

  @Test
  fun `it allows an OAuth token outside the project-scoped paths when the handler opts in`() {
    oauthAuthenticated()

    mockMvc.perform(get("/explicitly-opened-to-oauth")).andIsOk
  }

  @Test
  fun `it refuses an OAuth token on a global route even under a project-scoped path`() {
    oauthAuthenticated()

    mockMvc.perform(get("/v2/projects/1/global-route")).andIsForbidden
  }

  private fun oauthAuthenticated() {
    Mockito.`when`(authenticationFacade.isApiAuthentication).thenReturn(true)
    Mockito.`when`(authenticationFacade.isOAuthTokenAuth).thenReturn(true)
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

    @GetMapping("/v2/projects/1/allow-api-access")
    @AllowApiAccess
    fun projectScoped(): String = "hello!"

    @GetMapping("/v2/projects/1/global-route")
    @AllowApiAccess
    @IsGlobalRoute
    fun projectScopedGlobalRoute(): String = "hello!"

    @GetMapping("/v2/projects/1/only-pak")
    @AllowApiAccess(tokenType = AuthTokenType.ONLY_PAK)
    fun projectScopedOnlyPak(): String = "hello!"

    @GetMapping("/explicitly-opened-to-oauth")
    @AllowApiAccess
    @AllowOAuthAccess
    fun explicitlyOpenedToOAuth(): String = "hello!"

    @GetMapping("/v2/projects/1/only-pak-opened-to-oauth")
    @AllowApiAccess(tokenType = AuthTokenType.ONLY_PAK)
    @AllowOAuthAccess
    fun onlyPakOpenedToOAuth(): String = "hello!"

    @GetMapping("/requires-super-auth")
    @RequiresSuperAuthentication
    fun superAuth(): String = "hello!"
  }
}

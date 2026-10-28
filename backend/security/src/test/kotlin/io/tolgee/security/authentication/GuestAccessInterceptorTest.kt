package io.tolgee.security.authentication

import io.tolgee.exceptions.AuthenticationException
import io.tolgee.fixtures.andIsOk
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.testing.assertions.Assertions.assertThat
import jakarta.servlet.ServletException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

class GuestAccessInterceptorTest {
  private val authenticationFacade = Mockito.mock(AuthenticationFacade::class.java)

  private val mockMvc =
    MockMvcBuilders
      .standaloneSetup(TestController::class.java)
      .addInterceptors(GuestAccessInterceptor(authenticationFacade))
      .build()

  @AfterEach
  fun resetMocks() {
    Mockito.reset(authenticationFacade)
  }

  @Test
  fun `a guest is told to sign in, whatever permission annotation the handler carries`() {
    Mockito.`when`(authenticationFacade.isGuest).thenReturn(true)

    listOf("/no-annotation", "/default-permissions").forEach { path ->
      val thrown = assertThrows<ServletException>(path) { mockMvc.perform(get(path)) }
      assertThat(thrown.cause).describedAs(path).isInstanceOf(AuthenticationException::class.java)
    }
  }

  @Test
  fun `a guest reaches a handler that opts in`() {
    Mockito.`when`(authenticationFacade.isGuest).thenReturn(true)

    mockMvc.perform(get("/allow-guest")).andIsOk
  }

  @Test
  fun `anyone else is not its concern`() {
    Mockito.`when`(authenticationFacade.isGuest).thenReturn(false)

    mockMvc.perform(get("/no-annotation")).andIsOk
  }

  @RestController
  class TestController {
    @GetMapping("/no-annotation")
    fun noAnnotation() = "hello"

    @GetMapping("/default-permissions")
    @UseDefaultPermissions
    fun defaultPermissions() = "hello"

    @GetMapping("/allow-guest")
    @AllowGuestAccess
    fun allowGuest() = "hello"
  }
}

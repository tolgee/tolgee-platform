package io.tolgee.security.authentication

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.service.EmailVerificationService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

class EmailValidationInterceptorTest {
  private val authenticationFacade = Mockito.mock(AuthenticationFacade::class.java)

  private val userAccount = Mockito.mock(UserAccountDto::class.java)

  private val emailVerificationService = Mockito.mock(EmailVerificationService::class.java)

  private val emailValidationInterceptor =
    EmailValidationInterceptor(authenticationFacade, emailVerificationService)

  private val mockMvc =
    MockMvcBuilders
      .standaloneSetup(TestController::class.java)
      .addInterceptors(emailValidationInterceptor)
      .build()

  @BeforeEach
  fun setupMocks() {
    Mockito.`when`(authenticationFacade.authenticatedUser).thenReturn(userAccount)
    Mockito.`when`(authenticationFacade.isAuthenticated).thenReturn(true)
    Mockito.`when`(emailVerificationService.isVerified(any<UserAccountDto>())).thenReturn(true)
  }

  @AfterEach
  fun resetMocks() {
    Mockito.reset(emailVerificationService)
  }

  @Test
  fun `rejects access if the user does not have a verified email`() {
    Mockito.`when`(emailVerificationService.isVerified(any<UserAccountDto>())).thenReturn(false)
    mockMvc.perform(get("/no-annotation")).andIsForbidden
  }

  @Test
  fun `not throw when annotated by email verification bypass`() {
    Mockito.`when`(emailVerificationService.isVerified(any<UserAccountDto>())).thenReturn(false)
    mockMvc.perform(get("/email-bypass")).andIsOk
  }

  @RestController
  class TestController {
    @GetMapping("/no-annotation")
    fun emailVerified(): String = "hello!"

    @GetMapping("/email-bypass")
    @BypassEmailVerification
    fun emailBypass(): String = "hello!"
  }
}

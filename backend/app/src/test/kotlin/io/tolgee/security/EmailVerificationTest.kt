package io.tolgee.security

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.authentication.needs-email-verification=true",
    "tolgee.front-end-url=dummy_frontend_url"
  ]
)
@AutoConfigureMockMvc
open class EmailVerificationTest : AbstractControllerTest() {

  @set:Autowired
  lateinit var mailSender: MailSender

  @BeforeEach
  fun setup() {
    every { mailSender.send(any()) } returns Unit
  }

  @BeforeEach
  fun setupConstructor() {
    mockkConstructor(SimpleMailMessage::class)
  }

  @Test
  fun doesNotLoginWhenNotVerified() {
    val createUser = dbPopulator.createUserIfNotExists(initialUsername)
    emailVerificationService.createForUser(createUser)

    val response = doAuthentication(initialUsername, initialPassword)
    assertThat(response).error().hasCode("email_not_verified")
  }

  @Test
  fun doesLoginWhenVerified() {
    dbPopulator.createUserIfNotExists(initialUsername)
    val result = doAuthentication(initialUsername, initialPassword)
    assertThat(result.response.status).isEqualTo(200)
  }

  @Test
  fun verifiesEmail() {
    val createUser = dbPopulator.createUserIfNotExists(initialUsername)
    val emailVerification = emailVerificationService.createForUser(createUser)
    mvc.perform(get("/api/public/verify_email/${createUser.id}/${emailVerification!!.code}"))
      .andExpect(status().isOk).andReturn()

    assertThat(emailVerificationRepository.findById(emailVerification.id!!)).isEmpty
  }

  @Test
  @Transactional
  fun verifiesNewEmail() {
    val createUser = dbPopulator.createUserIfNotExists(initialUsername)
    val emailVerification = emailVerificationService.createForUser(createUser, newEmail = "this.is@new.email")
    mvc.perform(get("/api/public/verify_email/${createUser.id}/${emailVerification!!.code}"))
      .andExpect(status().isOk).andReturn()
    assertThat(emailVerificationRepository.findById(emailVerification.id!!)).isEmpty
    assertThat(userAccountService.findOptional(createUser.username).get().username).isEqualTo("this.is@new.email")
  }

  @Test
  fun doesNotVerifyWithWrongCode() {
    val createUser = dbPopulator.createUserIfNotExists(initialUsername)
    val emailVerification = emailVerificationService.createForUser(createUser)
    mvc.perform(get("/api/public/verify_email/${createUser.id}/wrong_code"))
      .andExpect(status().isNotFound).andReturn()

    assertThat(emailVerificationRepository.findById(emailVerification!!.id!!)).isPresent
  }

  @Test
  @Transactional
  fun doesNotVerifyWithWrongUser() {
    val createUser = dbPopulator.createUserIfNotExists(initialUsername)
    val emailVerification = emailVerificationService.createForUser(createUser)
    mvc.perform(get("/api/public/verify_email/${createUser.id + 1L}/${emailVerification!!.code}"))
      .andExpect(status().isNotFound).andReturn()

    assertThat(emailVerificationRepository.findById(emailVerification.id!!)).isPresent
  }

  val signUpDto = SignUpDto("Test Name", "aaa@aaa.com", "testtest", null)

  protected fun perform(): MvcResult {
    return mvc.perform(
      post("/api/public/sign_up")
        .content(mapper.writeValueAsString(signUpDto))
        .accept(MediaType.ALL)
        .contentType(MediaType.APPLICATION_JSON)
    )
      .andReturn()
  }

  @Test
  fun signUpSavesVerification() {
    perform()

    val user = userAccountService.findOptional(signUpDto.email).orElseThrow { NotFoundException() }

    verify { mailSender.send(any()) }

    verify { anyConstructed<SimpleMailMessage>().subject = "Tolgee e-mail verification" }

    verify {
      anyConstructed<SimpleMailMessage>().text = match {
        it.contains("dummy_frontend_url/login/verify_email/${user.id}/")
      }
    }

    assertThat(userAccountService[user.id]).isNotNull
  }

  @Test
  fun signUpFrontendUrlIsPrimary() {
    signUpDto.callbackUrl = "dummyCallbackUrl"

    perform()

    val user = userAccountService.findOptional(signUpDto.email).orElseThrow { NotFoundException() }

    verify {
      anyConstructed<SimpleMailMessage>().text = match {
        it.contains("dummy_frontend_url/login/verify_email/${user.id}/")
      }
    }

    assertThat(userAccountService[user.id]).isNotNull
  }

  @Test
  fun signUpDoesNotReturnToken() {
    assertThat(perform().response.contentAsString).isEqualTo("")
  }

  @SpringBootTest(
    properties = [
      "tolgee.authentication.needs-email-verification=true",
    ]
  )

  @AutoConfigureMockMvc
  @ContextRecreatingTest
  class EmailVerificationNoFrontendUrlTest : AbstractControllerTest() {

    val signUpDto = SignUpDto("Test Name", "aaa@aaa.com", "testtest", null)

    @set:Autowired
    lateinit var mailSender: MailSender

    @Test
    fun usesCallbackUrlIs() {
      mockkConstructor(SimpleMailMessage::class)

      every { mailSender.send(any()) } returns Unit

      signUpDto.callbackUrl = "dummyCallbackUrl"

      mvc.perform(
        post("/api/public/sign_up")
          .content(mapper.writeValueAsString(signUpDto))
          .accept(MediaType.ALL)
          .contentType(MediaType.APPLICATION_JSON)
      )
        .andReturn()

      val user = userAccountService.findOptional(signUpDto.email).orElseThrow { NotFoundException() }

      verify {
        anyConstructed<SimpleMailMessage>().text = match {
          it.contains("dummyCallbackUrl/login/verify_email/${user.id}/")
        }
      }

      assertThat(userAccountService[user.id]).isNotNull
    }
  }
}

@Configuration
class MockBeanMailSender {
  @Bean
  @Primary
  fun mailSender(): JavaMailSender {
    return mockk()
  }
}

package io.tolgee.security

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.fixtures.JavaMailSenderMocked
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import javax.mail.internet.MimeMessage

class EmailVerificationTest : AbstractControllerTest(), JavaMailSenderMocked {

  override lateinit var messageArgumentCaptor: ArgumentCaptor<MimeMessage>

  @Autowired
  @MockBean
  override lateinit var javaMailSender: JavaMailSender

  @Autowired
  override lateinit var tolgeeProperties: TolgeeProperties

  @BeforeEach
  fun setup() {
    resetProperties()
    tolgeeProperties.authentication.needsEmailVerification = true
  }

  @AfterEach
  fun cleanup() {
    resetProperties()
  }

  private fun resetProperties() {
    tolgeeProperties.authentication.needsEmailVerification = false
    tolgeeProperties.frontEndUrl = "dummy_frontend_url"
    tolgeeProperties.smtp.from = "aaa@aaa.aa"
  }

  @Test
  fun doesNotLoginWhenNotVerified() {
    val createUser = dbPopulator.createUserIfNotExists(initialUsername)
    emailVerificationService.createForUser(createUser)

    val response = doAuthentication(initialUsername, initialPassword)
    assertThat(response.andReturn()).error().hasCode("email_not_verified")
  }

  @Test
  fun doesLoginWhenVerified() {
    dbPopulator.createUserIfNotExists(initialUsername)
    val result = doAuthentication(initialUsername, initialPassword)
    assertThat(result.andReturn().response.status).isEqualTo(200)
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
    assertThat(userAccountService.findActive(createUser.username)!!.username).isEqualTo("this.is@new.email")
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

  val signUpDto = SignUpDto("Test Name", "aaa@aaa.com", null, "testtest")

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
    val user = userAccountService.findActive(signUpDto.email) ?: throw NotFoundException()
    verify(javaMailSender).send(messageArgumentCaptor.capture())

    assertThat(messageArgumentCaptor.value.subject).isEqualTo("Tolgee e-mail verification")

    assertThat(getMessageContent()).contains("dummy_frontend_url/login/verify_email/${user.id}/")

    assertThat(userAccountService.findActive(user.id)).isNotNull
  }

  @Test
  fun `uses frontend url over provided callback url`() {
    signUpDto.callbackUrl = "dummyCallbackUrl"
    perform()
    val user = userAccountService.findActive(signUpDto.email) ?: throw NotFoundException()

    assertThat(getMessageContent()).contains("dummy_frontend_url/login/verify_email/${user.id}/")

    assertThat(userAccountService.findActive(user.id)).isNotNull
  }

  private fun getMessageContent(): String {
    verify(javaMailSender).send(messageArgumentCaptor.capture())
    return messageArgumentCaptor.value.tolgeeStandardMessageContent
  }

  @Test
  fun signUpDoesNotReturnToken() {
    assertThat(perform().response.contentAsString).isEqualTo("")
  }

  @Test
  fun `uses callback url when no frontendUrl provided`() {
    signUpDto.callbackUrl = "dummyCallbackUrl"
    tolgeeProperties.frontEndUrl = null
    perform()

    val user = userAccountService.findActive(signUpDto.email) ?: throw NotFoundException()

    assertThat(getMessageContent()).contains("dummyCallbackUrl/login/verify_email/${user.id}/")
  }
}

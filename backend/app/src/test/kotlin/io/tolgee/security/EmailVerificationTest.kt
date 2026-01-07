package io.tolgee.security

import io.tolgee.config.TestEmailConfiguration
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.fixtures.EmailTestUtil
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@Import(TestEmailConfiguration::class)
class EmailVerificationTest : AbstractControllerTest() {
  @Autowired
  private lateinit var emailTestUtil: EmailTestUtil

  @Autowired
  override lateinit var tolgeeProperties: TolgeeProperties

  private var defaultFrontendUrl: String? = null

  @BeforeEach
  fun setup() {
    defaultFrontendUrl = tolgeeProperties.frontEndUrl
    resetProperties()
    tolgeeProperties.authentication.needsEmailVerification = true
    emailTestUtil.initMocks()
  }

  @AfterEach
  fun cleanup() {
    resetProperties()
  }

  private fun resetProperties() {
    tolgeeProperties.authentication.needsEmailVerification = false
    tolgeeProperties.frontEndUrl = defaultFrontendUrl
    tolgeeProperties.smtp.from = "aaa@aaa.aa"
  }

  @Test
  fun loginWhenNotVerified() {
    val createUser = dbPopulator.createUserIfNotExists(initialUsername)
    emailVerificationService.createForUser(createUser)

    val response = doAuthentication(initialUsername, initialPassword)
    assertThat(response.andReturn().response.status).isEqualTo(200)
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
    mvc
      .perform(get("/api/public/verify_email/${createUser.id}/${emailVerification!!.code}"))
      .andExpect(status().isOk)
      .andReturn()

    assertThat(emailVerificationRepository.findById(emailVerification.id!!)).isEmpty
  }

  @Test
  @Transactional
  fun verifiesNewEmail() {
    val createUser = dbPopulator.createUserIfNotExists(initialUsername)
    val emailVerification = emailVerificationService.createForUser(createUser, newEmail = "this.is@new.email")
    mvc
      .perform(get("/api/public/verify_email/${createUser.id}/${emailVerification!!.code}"))
      .andExpect(status().isOk)
      .andReturn()
    assertThat(emailVerificationRepository.findById(emailVerification.id!!)).isEmpty
    assertThat(userAccountService.findActive(createUser.username)!!.username).isEqualTo("this.is@new.email")
  }

  @Test
  fun doesNotVerifyWithWrongCode() {
    val createUser = dbPopulator.createUserIfNotExists(initialUsername)
    val emailVerification = emailVerificationService.createForUser(createUser)
    mvc
      .perform(get("/api/public/verify_email/${createUser.id}/wrong_code"))
      .andExpect(status().isBadRequest)
      .andReturn()

    assertThat(emailVerificationRepository.findById(emailVerification!!.id!!)).isPresent
  }

  @Test
  @Transactional
  fun doesNotVerifyWithWrongUser() {
    val createUser = dbPopulator.createUserIfNotExists(initialUsername)
    val emailVerification = emailVerificationService.createForUser(createUser)
    mvc
      .perform(get("/api/public/verify_email/${createUser.id + 1L}/${emailVerification!!.code}"))
      .andExpect(status().isNotFound)
      .andReturn()

    assertThat(emailVerificationRepository.findById(emailVerification.id!!)).isPresent
  }

  val signUpDto = SignUpDto("Test Name", "aaa@aaa.com", null, "testtest")

  protected fun perform(): MvcResult {
    return mvc
      .perform(
        post("/api/public/sign_up")
          .content(mapper.writeValueAsString(signUpDto))
          .accept(MediaType.ALL)
          .contentType(MediaType.APPLICATION_JSON),
      ).andReturn()
  }

  @Test
  fun signUpSavesVerification() {
    perform()
    val user = userAccountService.findActive(signUpDto.email) ?: throw NotFoundException()
    emailTestUtil.verifyEmailSent()

    assertThat(emailTestUtil.messageArgumentCaptor.firstValue.subject).isEqualTo("Tolgee e-mail verification")

    assertThat(getMessageContent()).contains("https://dummy-url.com/login/verify_email/${user.id}/")

    assertThat(userAccountService.findActive(user.id)).isNotNull
  }

  @Test
  fun `uses frontend url over provided callback url`() {
    signUpDto.callbackUrl = "dummyCallbackUrl"
    perform()
    val user = userAccountService.findActive(signUpDto.email) ?: throw NotFoundException()

    assertThat(getMessageContent()).contains("https://dummy-url.com/login/verify_email/${user.id}/")

    assertThat(userAccountService.findActive(user.id)).isNotNull
  }

  private fun getMessageContent(): String {
    emailTestUtil.verifyEmailSent()
    return emailTestUtil.messageContents.single()
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

package io.tolgee.controllers

import io.tolgee.component.emailContacts.EmailServiceManager
import io.tolgee.component.emailContacts.MailJetEmailServiceManager
import io.tolgee.config.TestEmailConfiguration
import io.tolgee.dtos.request.UserUpdateRequestDto
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.fixtures.EmailTestUtil
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.UserAccount
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.util.GitHubAuthUtil
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.web.client.RestTemplate

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmailConfiguration::class)
class MarketingEmailingTest : AuthorizedControllerTest() {
  @Autowired
  @MockitoBean
  lateinit var mailjetEmailServiceManager: MailJetEmailServiceManager

  @MockitoBean
  @Autowired
  private val restTemplate: RestTemplate? = null

  @Autowired
  private lateinit var emailTestUtil: EmailTestUtil

  @Autowired
  private var authMvc: MockMvc? = null

  private val gitHubAuthUtil: GitHubAuthUtil by lazy { GitHubAuthUtil(tolgeeProperties, authMvc, restTemplate) }

  lateinit var updateRequestDto: UserUpdateRequestDto

  @BeforeAll
  fun initDto() {
    updateRequestDto =
      UserUpdateRequestDto(
        name = "New Name",
        email = "newemail@test.com",
        currentPassword = initialPassword,
      )
  }

  @BeforeEach
  fun setup() {
    Mockito.clearInvocations(mailjetEmailServiceManager)
    tolgeeProperties.smtp.from = "aa@aa.com"
    emailTestUtil.initMocks()
  }

  @AfterEach
  fun cleanUp() {
    tolgeeProperties.authentication.needsEmailVerification = false
  }

  private val testMail = "mail@test.com"
  val testName = "Pavel Novak"

  @Test
  fun `adds contact on user sign up `() {
    val dto = SignUpDto(name = testName, password = "aaaaaaaaaa", email = testMail)
    performPost("/api/public/sign_up", dto)
      .andIsOk
    verifyCreateContactCalled()
  }

  @Test
  fun `adds contact after verification when needs-verification is on`() {
    tolgeeProperties.authentication.needsEmailVerification = true
    val dto = SignUpDto(name = testName, password = "aaaaaaaaaa", email = testMail)
    performPost("/api/public/sign_up", dto)
      .andIsOk
    verify(mailjetEmailServiceManager, times(0)).submitNewContact(any(), any())

    executeInNewTransaction {
      val user = userAccountService.get(testMail)
      acceptEmailVerification(user)
    }
    Thread.sleep(100)
    verifyCreateContactCalled()
  }

  @Test
  fun `updates contact when user is updated`() {
    val user = dbPopulator.createUserIfNotExists(username = testMail, name = testName)
    userAccount = user
    performAuthPut("/v2/user", updateRequestDto)
      .andIsOk
    verifyEmailUpdated()
  }

  @Test
  fun `updates contact email when verified`() {
    tolgeeProperties.authentication.needsEmailVerification = true
    val user = dbPopulator.createUserIfNotExists(username = testMail, name = testName)
    val updatedUser =
      executeInNewTransaction {
        val updatedUser = userAccountService.get(user.id)
        val request = mock<HttpServletRequest>()
        userAccountService.update(userAccountService.get(user.id), updateRequestDto, request)
        updatedUser
      }
    Thread.sleep(100)
    verify(mailjetEmailServiceManager).updateContact(eq(testMail), any(), any())
    Mockito.clearInvocations(mailjetEmailServiceManager)
    executeInNewTransaction {
      acceptEmailVerification(updatedUser)
    }
    verifyEmailUpdated()
  }

  @Test
  fun `adds contact when registered via github`() {
    gitHubAuthUtil.authorizeGithubUser()
    verifyCreateContactCalled("fake_email@email.com", "fakeName")
  }

  private fun acceptEmailVerification(user: UserAccount) {
    val emailVerificationCode = user.emailVerification!!.code
    mvc
      .perform(MockMvcRequestBuilders.get("/api/public/verify_email/${user.id}/$emailVerificationCode"))
      .andExpect(MockMvcResultMatchers.status().isOk)
      .andReturn()
  }

  private fun verifyEmailUpdated() {
    Thread.sleep(100)
    forServiceManagers {
      val lastInvocation =
        Mockito
          .mockingDetails(it)
          .invocations
          .last { it.method.name == "updateContact" }
      val invocationNewEmail = lastInvocation.arguments[1]
      val invocationNewName = lastInvocation.arguments[2]
      invocationNewName.assert.isEqualTo(updateRequestDto.name)
      invocationNewEmail.assert.isEqualTo(updateRequestDto.email)
      Mockito.clearInvocations(it)
    }
  }

  private fun verifyCreateContactCalled(
    email: String = testMail,
    name: String = testName,
  ) {
    forServiceManagers {
      val lastInvocation =
        Mockito
          .mockingDetails(it)
          .invocations
          .last { it.method.name == "submitNewContact" }
      val invocationEmail = lastInvocation.arguments[1]
      val invocationName = lastInvocation.arguments[0]
      invocationName.assert.isEqualTo(name)
      invocationEmail.assert.isEqualTo(email)
      Mockito.clearInvocations(it)
    }
  }

  private fun forServiceManagers(fn: (EmailServiceManager) -> Unit) {
    fn(mailjetEmailServiceManager)
  }
}

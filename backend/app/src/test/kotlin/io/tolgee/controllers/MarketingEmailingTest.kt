package io.tolgee.controllers

import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.configuration.tolgee.SendInBlueProperties
import io.tolgee.dtos.request.UserUpdateRequestDto
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.fixtures.EmailTestUtil
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.UserAccount
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.GitHubAuthUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.web.client.RestTemplate
import sibApi.ContactsApi
import sibModel.CreateContact
import sibModel.UpdateContact

@SpringBootTest
@AutoConfigureMockMvc
class MarketingEmailingTest : AuthorizedControllerTest() {

  @Autowired
  lateinit var sendInBlueProperties: SendInBlueProperties

  @Autowired
  @MockBean
  lateinit var contactsApi: ContactsApi

  lateinit var createContactArgumentCaptor: ArgumentCaptor<CreateContact>
  lateinit var updateContactArgumentCaptor: ArgumentCaptor<UpdateContact>

  @MockBean
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
    updateRequestDto = UserUpdateRequestDto(
      name = "New Name",
      email = "newemail@test.com",
      currentPassword = initialPassword
    )
  }

  @BeforeEach
  fun setup() {
    Mockito.clearInvocations(contactsApi)
    tolgeeProperties.frontEndUrl = "https://aaa"
    tolgeeProperties.smtp.from = "aa@aa.com"
    createContactArgumentCaptor = ArgumentCaptor.forClass(CreateContact::class.java)
    updateContactArgumentCaptor = ArgumentCaptor.forClass(UpdateContact::class.java)
    emailTestUtil.initMocks()
  }

  @AfterEach
  fun cleanUp() {
    sendInBlueProperties.listId = null
    tolgeeProperties.authentication.needsEmailVerification = false
    tolgeeProperties.frontEndUrl = null
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
    tolgeeProperties.frontEndUrl = "https://aaa"
    tolgeeProperties.authentication.needsEmailVerification = true
    val dto = SignUpDto(name = testName, password = "aaaaaaaaaa", email = testMail)
    performPost("/api/public/sign_up", dto)
      .andIsOk
    verify(contactsApi, times(0)).createContact(any())

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
    verifyEmailSentOnUpdate()
  }

  @Test
  fun `updates contact email when verified`() {
    tolgeeProperties.authentication.needsEmailVerification = true
    val user = dbPopulator.createUserIfNotExists(username = testMail, name = testName)
    val updatedUser = executeInNewTransaction {
      val updatedUser = userAccountService.get(user.id)
      userAccountService.update(userAccountService.get(user.id), updateRequestDto)
      updatedUser
    }
    Thread.sleep(100)
    verify(contactsApi).updateContact(eq(testMail), any())
    Mockito.clearInvocations(contactsApi)
    executeInNewTransaction {
      acceptEmailVerification(updatedUser)
    }
    verifyEmailSentOnUpdate()
  }

  @Test
  fun `adds contact when registered via github`() {
    gitHubAuthUtil.authorizeGithubUser()
    verifyCreateContactCalled("fake_email@email.com", "fakeName")
  }

  private fun acceptEmailVerification(user: UserAccount) {
    val emailVerificationCode = user.emailVerification!!.code
    mvc.perform(MockMvcRequestBuilders.get("/api/public/verify_email/${user.id}/$emailVerificationCode"))
      .andExpect(MockMvcResultMatchers.status().isOk).andReturn()
  }

  private fun verifyEmailSentOnUpdate() {
    Thread.sleep(100)
    verify(contactsApi).updateContact(eq(testMail), updateContactArgumentCaptor.capture())
    val attributes = updateContactArgumentCaptor.value.attributes as Map<String, String>
    assertThat(attributes["NAME"] as String).isEqualTo(updateRequestDto.name)
    assertThat(attributes["EMAIL"] as String).isEqualTo(updateRequestDto.email)
    Mockito.clearInvocations(contactsApi)
  }

  private fun verifyCreateContactCalled(email: String = testMail, name: String = testName) {
    Thread.sleep(100)
    verify(contactsApi).createContact(createContactArgumentCaptor.capture())
    assertThat(createContactArgumentCaptor.value.email).isEqualTo(email)
    val attributes = createContactArgumentCaptor.value.attributes as Map<String, String>
    assertThat(attributes["NAME"] as String).isEqualTo(name)
  }
}

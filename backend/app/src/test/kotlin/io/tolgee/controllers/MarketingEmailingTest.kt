package io.tolgee.controllers

import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.configuration.tolgee.SendInBlueProperties
import io.tolgee.dtos.request.UserUpdateRequestDto
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.UserAccount
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import sibApi.ContactsApi
import sibModel.CreateContact
import sibModel.UpdateContact

@SpringBootTest
@AutoConfigureMockMvc
class MarketingEmailingTest : AuthorizedControllerTest() {

  @Autowired
  lateinit var sendInBlueProperties: SendInBlueProperties

  @Autowired
  lateinit var authenticationProperties: AuthenticationProperties

  @Autowired
  @MockBean
  lateinit var contactsApi: ContactsApi

  @Autowired
  @MockBean
  lateinit var javaMailSender: JavaMailSender

  lateinit var createContactArgumentCaptor: ArgumentCaptor<CreateContact>
  lateinit var updateContactArgumentCaptor: ArgumentCaptor<UpdateContact>

  val updateRequestDto = UserUpdateRequestDto(
    name = "New Name",
    email = "newemail@test.com"
  )

  @BeforeEach
  fun setup() {
    Mockito.clearInvocations(contactsApi)
    Mockito.clearInvocations(javaMailSender)
    tolgeeProperties.frontEndUrl = "https://aaa"
    tolgeeProperties.smtp.from = "aa@aa.com"
    whenever(javaMailSender.createMimeMessage()).thenReturn(mock())
    createContactArgumentCaptor = ArgumentCaptor.forClass(CreateContact::class.java)
    updateContactArgumentCaptor = ArgumentCaptor.forClass(UpdateContact::class.java)
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

    val user = userAccountService.get(testMail)
    acceptEmailVerification(user)
    Thread.sleep(100)
    verifyCreateContactCalled()
  }

  @Test
  fun `updates contact when user is updated`() {
    val user = dbPopulator.createUserIfNotExists(username = testMail, name = testName)
    userAccount = user
    performAuthPost("/api/user", updateRequestDto)
      .andIsOk
    verifyEmailSentOnUpdate()
  }

  @Test
  fun `updates contact email when verified`() {
    tolgeeProperties.authentication.needsEmailVerification = true
    val user = dbPopulator.createUserIfNotExists(username = testMail, name = testName)
    userAccountService.update(user, updateRequestDto)
    Thread.sleep(100)
    verify(contactsApi).updateContact(eq(testMail), any())
    Mockito.clearInvocations(contactsApi)
    acceptEmailVerification(user)
    verifyEmailSentOnUpdate()
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

  private fun verifyCreateContactCalled() {
    Thread.sleep(100)
    verify(contactsApi).createContact(createContactArgumentCaptor.capture())
    assertThat(createContactArgumentCaptor.value.email).isEqualTo(testMail)
    val attributes = createContactArgumentCaptor.value.attributes as Map<String, String>
    assertThat(attributes["NAME"] as String).isEqualTo(testName)
  }
}

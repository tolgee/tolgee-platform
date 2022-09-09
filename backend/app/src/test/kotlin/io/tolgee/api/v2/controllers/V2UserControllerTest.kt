package io.tolgee.api.v2.controllers

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.request.UserUpdatePasswordRequestDto
import io.tolgee.dtos.request.UserUpdateRequestDto
import io.tolgee.fixtures.JavaMailSenderMocked
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import javax.mail.internet.MimeMessage

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.front-end-url=https://fake.frontend.url"
  ]
)
class V2UserControllerTest : AuthorizedControllerTest(), JavaMailSenderMocked {

  @MockBean
  @Autowired
  override lateinit var javaMailSender: JavaMailSender

  @Autowired
  override lateinit var tolgeeProperties: TolgeeProperties

  @Autowired
  lateinit var passwordEncoder: PasswordEncoder

  override lateinit var messageArgumentCaptor: ArgumentCaptor<MimeMessage>

  @Test
  fun `it updates the user profile`() {
    val requestDTO = UserUpdateRequestDto(
      email = "ben@ben.aa",
      name = "Ben's new name",
      currentPassword = initialPassword
    )
    performAuthPut("/v2/user", requestDTO).andExpect(MockMvcResultMatchers.status().isOk)
    val fromDb = userAccountService.findOptional(requestDTO.email)
    Assertions.assertThat(fromDb).isNotEmpty
    Assertions.assertThat(fromDb.get().name).isEqualTo(requestDTO.name)
  }

  @Test
  fun `it updates the user password`() {
    val requestDTO = UserUpdatePasswordRequestDto(
      password = "super new password",
      currentPassword = initialPassword
    )
    performAuthPut("/v2/user/password", requestDTO).andExpect(MockMvcResultMatchers.status().isOk)
    val fromDb = userAccountService.findOptional(initialUsername)
    Assertions.assertThat(fromDb).isNotEmpty
    Assertions.assertThat(passwordEncoder.matches(requestDTO.password, fromDb.get().password))
      .describedAs("Password is changed").isTrue
  }

  @Test
  fun `it validates the user update request data`() {
    var requestDTO = UserUpdateRequestDto(
      email = "ben@ben.aa",
      name = "",
      currentPassword = initialPassword
    )
    var mvcResult = performAuthPut("/v2/user", requestDTO)
      .andExpect(MockMvcResultMatchers.status().isBadRequest).andReturn()
    val standardValidation = assertThat(mvcResult).error().isStandardValidation
    standardValidation.onField("name")

    requestDTO = UserUpdateRequestDto(
      email = "ben@ben.aa",
      name = "a",
      currentPassword = initialPassword
    )
    dbPopulator.createUserIfNotExists(requestDTO.email)
    mvcResult = performAuthPut("/v2/user", requestDTO)
      .andExpect(MockMvcResultMatchers.status().isBadRequest).andReturn()
    assertThat(mvcResult)
      .error().isCustomValidation.hasMessage("username_already_exists")
  }

  @Test
  fun `it validates the password change request data`() {
    val requestDto = UserUpdatePasswordRequestDto(
      password = "",
      currentPassword = initialPassword
    )
    val mvcResult = performAuthPut("/v2/user/password", requestDto)
      .andExpect(MockMvcResultMatchers.status().isBadRequest).andReturn()
    val standardValidation = assertThat(mvcResult).error().isStandardValidation
    standardValidation.onField("password")
  }

  @Test
  fun `it sends an email when updating user email`() {
    val oldNeedsVerification = tolgeeProperties.authentication.needsEmailVerification
    tolgeeProperties.authentication.needsEmailVerification = true

    val requestDTO = UserUpdateRequestDto(
      email = "ben@ben.aaa",
      name = "Ben Ben",
      currentPassword = initialPassword
    )
    performAuthPut("/v2/user", requestDTO).andIsOk

    verify(javaMailSender).send(messageArgumentCaptor.capture())
    assertThat(messageArgumentCaptor.value.tolgeeStandardMessageContent)
      .contains(tolgeeProperties.frontEndUrl.toString())

    tolgeeProperties.authentication.needsEmailVerification = oldNeedsVerification
  }

  @Test
  fun `it doesn't allow updating the email without password`() {
    loginAsUser(dbPopulator.createUserIfNotExists("ben@ben.aa"))
    val requestDTO = UserUpdateRequestDto(name = "a", email = "ben@ben.zz")
    performAuthPut("/v2/user", requestDTO).andExpect(MockMvcResultMatchers.status().isBadRequest)
  }

  @Test
  fun `it doesn't allow updating the email with an invalid password`() {
    loginAsUser(dbPopulator.createUserIfNotExists("ben@ben.aa"))
    val requestDTO = UserUpdateRequestDto(name = "a", email = "ben@ben.zz", currentPassword = "meow meow meow")
    performAuthPut("/v2/user", requestDTO).andExpect(MockMvcResultMatchers.status().isForbidden)
  }

  @Test
  fun `it doesn't allow updating the password without password`() {
    loginAsUser(dbPopulator.createUserIfNotExists("ben@ben.aa"))
    val requestDTO = UserUpdatePasswordRequestDto(password = "vewy secuwe paffword")
    performAuthPut("/v2/user/password", requestDTO).andExpect(MockMvcResultMatchers.status().isBadRequest)
  }

  @Test
  fun `it doesn't allow updating the password with an invalid password`() {
    loginAsUser(dbPopulator.createUserIfNotExists("ben@ben.aa"))
    val requestDTO = UserUpdatePasswordRequestDto(password = "vewy secuwe paffword", currentPassword = "meow meow meow")
    performAuthPut("/v2/user/password", requestDTO).andExpect(MockMvcResultMatchers.status().isForbidden)
  }

  @Test
  fun `it allows updating the display name without password`() {
    loginAsUser(dbPopulator.createUserIfNotExists("ben@ben.aa"))
    val requestDTO = UserUpdateRequestDto(name = "zzz", email = "ben@ben.aa")
    performAuthPut("/v2/user", requestDTO).andExpect(MockMvcResultMatchers.status().isOk)
  }

  @Test
  fun `it invalidates tokens generated prior a password change`() {
    val requestDTO = UserUpdatePasswordRequestDto(
      password = "super new password",
      currentPassword = initialPassword
    )

    loginAsAdminIfNotLogged()
    Thread.sleep(1000)

    performAuthPut("/v2/user/password", requestDTO).andExpect(MockMvcResultMatchers.status().isOk)
    refreshUser()
    performAuthGet("/v2/user").andExpect(MockMvcResultMatchers.status().isUnauthorized)
  }
}

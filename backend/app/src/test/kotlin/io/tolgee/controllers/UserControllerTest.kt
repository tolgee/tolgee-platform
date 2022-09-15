package io.tolgee.controllers

import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import javax.mail.internet.MimeMessage

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.front-end-url=https://fake.frontend.url"
  ]
)
class UserControllerTest : AuthorizedControllerTest(), JavaMailSenderMocked {

  @MockBean
  @Autowired
  override lateinit var javaMailSender: JavaMailSender

  @Autowired
  override lateinit var tolgeeProperties: TolgeeProperties

  @Autowired
  lateinit var authenticationProperties: AuthenticationProperties

  override lateinit var messageArgumentCaptor: ArgumentCaptor<MimeMessage>

  @Test
  fun updateUser() {
    val requestDTO = UserUpdateRequestDto(
      email = "ben@ben.aa",
      name = "Ben's new name",
      currentPassword = initialPassword
    )
    performAuthPost("/api/user", requestDTO).andExpect(MockMvcResultMatchers.status().isOk)
    val fromDb = userAccountService.find(requestDTO.email)
    Assertions.assertThat(fromDb!!.name).isEqualTo(requestDTO.name)
  }

  @Test
  fun updateUserValidation() {
    var requestDTO = UserUpdateRequestDto(
      email = "ben@ben.aa",
      name = "",
      currentPassword = initialPassword
    )
    var mvcResult = performAuthPost("/api/user", requestDTO)
      .andExpect(MockMvcResultMatchers.status().isBadRequest).andReturn()
    val standardValidation = assertThat(mvcResult).error().isStandardValidation
    standardValidation.onField("name")

    requestDTO = UserUpdateRequestDto(
      email = "ben@ben.aa",
      name = "a",
      currentPassword = initialPassword
    )
    dbPopulator.createUserIfNotExists(requestDTO.email)
    mvcResult = performAuthPost("/api/user", requestDTO)
      .andExpect(MockMvcResultMatchers.status().isBadRequest).andReturn()
    assertThat(mvcResult)
      .error().isCustomValidation.hasMessage("username_already_exists")
  }

  @Test
  fun updateUserSendsEmail() {
    val oldNeedsVerification = tolgeeProperties.authentication.needsEmailVerification
    tolgeeProperties.authentication.needsEmailVerification = true

    val requestDTO = UserUpdateRequestDto(
      email = "ben@ben.aaa",
      name = "Ben Ben",
      currentPassword = initialPassword
    )
    performAuthPost("/api/user", requestDTO).andIsOk

    verify(javaMailSender).send(messageArgumentCaptor.capture())
    assertThat(messageArgumentCaptor.value.tolgeeStandardMessageContent)
      .contains(tolgeeProperties.frontEndUrl.toString())

    tolgeeProperties.authentication.needsEmailVerification = oldNeedsVerification
  }
}

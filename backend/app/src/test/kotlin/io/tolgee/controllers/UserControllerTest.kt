package io.tolgee.controllers

import io.tolgee.dtos.request.UserUpdateRequestDTO
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import org.assertj.core.api.Assertions
import org.mockito.kotlin.argThat
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.testng.annotations.Test

@SpringBootTest(
  properties = [
    "tolgee.front-end-url=https://fake.frontend.url"
  ]
)
class UserControllerTest : AuthorizedControllerTest() {

  @MockBean
  @Autowired
  lateinit var javaMailSender: JavaMailSender

  @Test
  fun updateUser() {
    val requestDTO = UserUpdateRequestDTO(
      email = "ben@ben.aa",
      password = "super new password",
      name = "Ben's new name"
    )
    performAuthPost("/api/user", requestDTO).andExpect(MockMvcResultMatchers.status().isOk)
    val fromDb = userAccountService.getByUserName(requestDTO.email)
    Assertions.assertThat(fromDb).isNotEmpty
    val bCryptPasswordEncoder = BCryptPasswordEncoder()
    Assertions.assertThat(bCryptPasswordEncoder.matches(requestDTO.password, fromDb.get().password))
      .describedAs("Password is changed").isTrue
    Assertions.assertThat(fromDb.get().name).isEqualTo(requestDTO.name)
  }

  @Test
  fun updateUserValidation() {
    var requestDTO = UserUpdateRequestDTO(
      email = "ben@ben.aa",
      password = "",
      name = ""
    )
    var mvcResult = performAuthPost("/api/user", requestDTO)
      .andExpect(MockMvcResultMatchers.status().isBadRequest).andReturn()
    val standardValidation = io.tolgee.testing.assertions.Assertions.assertThat(mvcResult).error().isStandardValidation
    standardValidation.onField("password")
    standardValidation.onField("name")

    requestDTO = UserUpdateRequestDTO(
      email = "ben@ben.aa",
      password = "aksjhd  dasdsa",
      name = "a"
    )
    dbPopulator.createUserIfNotExists(requestDTO.email!!)
    mvcResult = performAuthPost("/api/user", requestDTO)
      .andExpect(MockMvcResultMatchers.status().isBadRequest).andReturn()
    io.tolgee.testing.assertions.Assertions.assertThat(mvcResult)
      .error().isCustomValidation.hasMessage("username_already_exists")
  }

  @Test
  fun updateUserSendsEmail() {
    val oldNeedsVerification = tolgeeProperties.authentication.needsEmailVerification
    tolgeeProperties.authentication.needsEmailVerification = true

    dbPopulator.createUserIfNotExists("ben@ben.aa")
    val requestDTO = UserUpdateRequestDTO(
      email = "ben@ben.aaa",
      name = "Ben Ben"
    )
    performAuthPost("/api/user", requestDTO).andIsOk
    verify(javaMailSender).send(
      argThat<SimpleMailMessage> {
        this.from == tolgeeProperties.smtp.from && this.text!!.contains(tolgeeProperties.frontEndUrl.toString())
      }
    )
    tolgeeProperties.authentication.needsEmailVerification = oldNeedsVerification
  }
}

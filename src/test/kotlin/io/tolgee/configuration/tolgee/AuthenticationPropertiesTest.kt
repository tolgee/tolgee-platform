package io.tolgee.configuration.tolgee

import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.service.UserAccountService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.testng.annotations.Test

@AutoConfigureMockMvc
@SpringBootTest(
  properties = [
    "tolgee.authentication.jwtSecret=test_jwt_secret"
  ]
)
class AuthenticationPropertiesTest : AbstractTestNGSpringContextTests() {
  @set:Autowired
  lateinit var tolgeeProperties: TolgeeProperties

  @set:Autowired
  lateinit var userAccountService: UserAccountService

  @Test
  fun testCreateInitialUserDisabled() {
    assertThat(tolgeeProperties.authentication.createInitialUser).isEqualTo(false)
    assertThat(userAccountService.getByUserName(tolgeeProperties.authentication.initialUsername)).isEmpty
  }

  @Test
  fun testJwtProperty() {
    assertThat(tolgeeProperties.authentication.jwtSecret).isEqualTo("test_jwt_secret")
  }
}

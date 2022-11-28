package io.tolgee.configuration.tolgee

import io.tolgee.CleanDbBeforeClass
import io.tolgee.service.security.UserAccountService
import io.tolgee.testing.AbstractTransactionalTest
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@AutoConfigureMockMvc
@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.authentication.jwtSecret=test_jwt_secret"
  ]
)
@CleanDbBeforeClass
class AuthenticationPropertiesTest : AbstractTransactionalTest() {
  @set:Autowired
  lateinit var tolgeeProperties: TolgeeProperties

  @set:Autowired
  lateinit var userAccountService: UserAccountService

  @Test
  fun testCreateInitialUserDisabled() {
    assertThat(tolgeeProperties.authentication.createInitialUser).isEqualTo(false)
    assertThat(userAccountService.findActive(tolgeeProperties.authentication.initialUsername)).isNull()
  }

  @Test
  fun testJwtProperty() {
    assertThat(tolgeeProperties.authentication.jwtSecret).isEqualTo("test_jwt_secret")
  }
}

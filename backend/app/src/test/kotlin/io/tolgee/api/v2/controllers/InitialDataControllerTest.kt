package io.tolgee.api.v2.controllers

import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class InitialDataControllerTest : AuthorizedControllerTest() {
  @Test
  fun `returns initial data when authenticated`() {
    performAuthGet("/v2/public/initial-data").andPrettyPrint.andIsOk.andAssertThatJson {
      node("serverConfiguration.authentication").isEqualTo(true)
      node("userInfo.name").isEqualTo("admin")
      node("preferredOrganization.name").isEqualTo("admin")
    }
  }

  @Test
  fun `returns initial data when not authenticated`() {
    performGet("/v2/public/initial-data").andPrettyPrint.andIsOk.andAssertThatJson {
      node("serverConfiguration.authentication").isEqualTo(true)
      node("userInfo").isEqualTo(null)
      node("preferredOrganization").isEqualTo(null)
    }
  }
}

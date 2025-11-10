package io.tolgee.api.v2.controllers.configurationProps

import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AbstractControllerTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class ConfigurationPropsControllerTest : AbstractControllerTest() {
  @Test
  fun `configuration properties endpoint returns data`() {
    performGet("/v2/public/configuration-properties")
      .andIsOk
      .andAssertThatJson {
        isArray.isNotEmpty
        node("[0].name").isString.isNotEmpty
        node("[0].children").isArray
        node("[0].children[0].defaultValue").isString
      }
  }
}

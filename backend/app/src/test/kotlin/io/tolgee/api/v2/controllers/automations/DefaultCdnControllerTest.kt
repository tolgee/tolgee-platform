package io.tolgee.api.v2.controllers.automations

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.ResultActions

class DefaultCdnControllerTest : ProjectAuthControllerTest("/v2/projects/") {

  @AfterEach()
  fun resetProps() {
    tolgeeProperties.cdn.publicUrlPrefix = null
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creates default automation`() {
    createTheDefault().andIsOk.andAssertThatJson {
      node("id").isValidId
      node("name").isEqualTo("Default CDN Automation")
      node("actions[0].cdnExporter.publicUrl").isString.contains("https://dummy")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns default automation`() {
    createTheDefault()
    performProjectAuthGet("default-project-cdn").andIsOk.andAssertThatJson {
      node("id").isValidId
    }
  }

  private fun createTheDefault(): ResultActions {
    tolgeeProperties.cdn.publicUrlPrefix = "https://dummy"
    return performProjectAuthPost(
      "default-project-cdn",
      mapOf<String, Any>()
    )
  }
}

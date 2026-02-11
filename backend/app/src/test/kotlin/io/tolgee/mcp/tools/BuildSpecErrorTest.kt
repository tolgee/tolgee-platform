package io.tolgee.mcp.tools

import io.tolgee.mcp.buildSpec
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.RequiresSuperAuthentication
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

class BuildSpecErrorTest {
  @RestController
  class TestController {
    @GetMapping("/no-api-access")
    fun noApiAccess(): String = "nope"

    @PostMapping("/super-auth")
    @AllowApiAccess
    @RequiresSuperAuthentication
    fun superAuth(): String = "nope"
  }

  @Test
  fun `method without AllowApiAccess throws IllegalArgumentException`() {
    assertThatThrownBy {
      buildSpec(TestController::class.java, "noApiAccess", "bad_tool")
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("lacks @AllowApiAccess")
  }

  @Test
  fun `method with RequiresSuperAuthentication throws IllegalArgumentException`() {
    assertThatThrownBy {
      buildSpec(TestController::class.java, "superAuth", "bad_tool")
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("requires super authentication")
  }
}

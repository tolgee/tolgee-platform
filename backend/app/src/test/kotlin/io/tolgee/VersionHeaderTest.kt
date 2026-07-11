package io.tolgee

import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders

class VersionHeaderTest : AbstractControllerTest() {
  @Test
  fun `adds header on endpoints`() {
    performGet("/v2/public/initial-data")
      .andIsOk
      .andReturn()
      .response
      .getHeader("X-Tolgee-Version")
      .assert
      .isEqualTo("??")
  }

  @Test
  fun `adds header when using with wrong PAT on authenticated endpoints`() {
    performGet(
      "/v2/user",
      HttpHeaders().apply {
        add("X-API-Key", "tgpat_nopat")
      },
    ).andIsUnauthorized.andReturn().response.getHeader("X-Tolgee-Version").assert.isEqualTo("??")
  }
}

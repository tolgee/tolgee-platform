package io.tolgee

import io.tolgee.fixtures.andIsForbidden
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders

class VersionHeaderTest : AbstractControllerTest() {
  @Test
  fun `user doesnt authorize with wrong PAT`() {
    performGet(
      "/v2/user",
      HttpHeaders().apply {
        add("X-API-Key", "tgpat_nopat")
      }
    ).andIsForbidden.andReturn().response.getHeader("X-Tolgee-Version").assert.isEqualTo("??")
  }
}

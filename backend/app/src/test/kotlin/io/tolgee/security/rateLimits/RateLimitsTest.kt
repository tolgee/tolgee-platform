package io.tolgee.security.rateLimits

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.testng.annotations.Test

@AutoConfigureMockMvc
@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ]
)
class RateLimitsTest : AbstractRateLimitsTest() {
  @Test
  override fun `it doesn't allow more then set in configuration`() {
    super.`it doesn't allow more then set in configuration`()
  }
}

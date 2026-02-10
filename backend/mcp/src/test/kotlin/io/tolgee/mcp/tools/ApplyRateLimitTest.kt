package io.tolgee.mcp.tools

import io.tolgee.mcp.RateLimitSpec
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.time.Duration

class ApplyRateLimitTest : McpSecurityTestBase() {
  @Test
  fun `no rate limit policy does not call rateLimitService`() {
    sut.executeAs(spec(rateLimitPolicy = null)) {}

    verify(rateLimitService, never()).checkPerUserRateLimit(any(), any(), any())
  }

  @Test
  fun `rate limit policy calls rateLimitService with correct args`() {
    val policy = RateLimitSpec(limit = 100, refillDurationInMs = 60_000)

    sut.executeAs(spec(mcpOperation = "my_op", rateLimitPolicy = policy)) {}

    verify(rateLimitService).checkPerUserRateLimit(
      eq("mcp.my_op"),
      eq(100),
      eq(Duration.ofMillis(60_000)),
    )
  }
}

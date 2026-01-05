package io.tolgee.security.rateLimits

import io.tolgee.constants.Caches
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsRateLimited
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.security.ratelimit.RateLimitedException
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.ContextRecreatingTest
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@AutoConfigureMockMvc
@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
    "tolgee.rate-limits.ip-request-limit=10",
    "tolgee.rate-limits.ip-request-window=10000",
    "tolgee.rate-limits.user-request-limit=15",
    "tolgee.rate-limits.user-request-window=10000",
    "tolgee.rate-limits.email-verification-request-limit=5",
    "tolgee.rate-limits.email-verification-request-window=10000",
  ],
)
class RateLimitsTest : AuthorizedControllerTest() {
  @BeforeEach
  fun clearCache() {
    cacheManager.getCache(Caches.RATE_LIMITS)?.clear()
  }

  @Test
  fun `email verification request limit works`() {
    val createUser = dbPopulator.createUserIfNotExists(initialUsername)
    val mockedRequest = Mockito.mock<HttpServletRequest>()
    whenever(mockedRequest.remoteAddr).thenAnswer { "0.0.0.0" }

    (0..4).forEach { _ ->
      emailVerificationService.resendEmailVerification(createUser, mockedRequest, newEmail = "newEmail@gmail.com")
    }
    assertThrows<RateLimitedException> {
      emailVerificationService.resendEmailVerification(createUser, mockedRequest, newEmail = "newEmail@gmail.com")
    }
  }

  @Test
  fun `ip request limit works`() {
    (1..10).forEach { _ ->
      performGet("/api/public/configuration").andIsOk
    }
    performGet("/api/public/configuration").andIsRateLimited
  }

  @Test
  fun `user request limit works`() {
    (1..15).forEach { _ ->
      performAuthGet("/v2/projects").andIsOk
    }
    performAuthGet("/v2/projects").andIsRateLimited
  }

  @Test
  fun `limits auth endpoints`() {
    (1..5).forEach { _ ->
      performPost("/api/public/generatetoken?bla", mapOf("username" to "a", "password" to "p")).andIsUnauthorized
    }
    performPost("/api/public/generatetoken?bla", mapOf("username" to "a", "password" to "p")).andIsRateLimited
  }

  @Test
  fun `limits the export`() {
    val projectTestData = TranslationsTestData()
    testDataService.saveTestData(projectTestData.root)
    loginAsUser(projectTestData.user.username)
    (1..10).forEach { _ ->
      performAuthGet("/v2/projects/${projectTestData.project.id}/export").andIsOk
    }
    performAuthGet("/v2/projects/${projectTestData.project.id}/export").andIsRateLimited
  }
}

package io.tolgee.api.v2.controllers.translations

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Caches
import io.tolgee.development.testDataBuilder.data.TranslationCommentsTestData
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsRateLimited
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
    "tolgee.rate-limits.ip-request-limit=2",
    "tolgee.rate-limits.ip-request-window=10000",
    "tolgee.rate-limits.user-request-limit=3",
    "tolgee.rate-limits.user-request-window=10000",
  ],
)
@AutoConfigureMockMvc
@ContextRecreatingTest
class TranslationCommentControllerRateLimitTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TranslationCommentsTestData

  @BeforeEach
  fun setup(testInfo: TestInfo) {
    testData = TranslationCommentsTestData()
    testDataService.saveTestData(testData.root)

    // Only set projectSupplier for tests with @ProjectJWTAuthTestMethod annotation
    val method = testInfo.testMethod.orElse(null)
    if (method?.getAnnotation(ProjectJWTAuthTestMethod::class.java) != null) {
      this.projectSupplier = { testData.project }
      userAccount = testData.user
    }
  }

  @BeforeEach
  fun clearCache() {
    cacheManager.getCache(Caches.RATE_LIMITS)?.clear()
  }

  @Test
  fun `returns 429 when IP rate limit exceeded`() {
    val url = "/v2/projects/${testData.project.id}/translations/${testData.translation.id}/comments"

    // First 2 requests return 403 (no auth) but count toward rate limit
    repeat(2) {
      performGet(url).andIsForbidden
    }

    // Third request should be rate limited
    performGet(url).andIsRateLimited
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `returns 429 when user rate limit exceeded`() {
    // First 3 requests should succeed (user-request-limit=3)
    repeat(3) {
      performProjectAuthGet("translations/${testData.translation.id}/comments").andIsOk
    }

    // Fourth request should be rate limited
    performProjectAuthGet("translations/${testData.translation.id}/comments").andIsRateLimited
  }
}

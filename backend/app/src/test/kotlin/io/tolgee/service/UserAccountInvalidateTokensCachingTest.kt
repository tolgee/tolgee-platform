package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.constants.Caches
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ],
)
class UserAccountInvalidateTokensCachingTest : AbstractSpringTest() {
  private lateinit var testData: BaseTestData

  @BeforeEach
  fun setup() {
    testData = BaseTestData("invalidate_tokens_caching_user", "invalidate_tokens_caching_project")
    testDataService.saveTestData(testData.root)
    clearCaches()
  }

  @AfterEach
  fun clean() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `invalidating tokens evicts the cached user, so the new cutoff is what the next request reads`() {
    userAccountService.getDto(testData.user.id)
    cachedUser().assert.isNotNull

    userAccountService.invalidateTokens(userAccountService.get(testData.user.id))

    cachedUser().assert.isNull()
    userAccountService
      .getDto(testData.user.id)
      .tokensValidNotBefore.assert
      .isNotNull()
  }

  private fun cachedUser(): UserAccountDto? =
    cacheManager
      .getCache(Caches.USER_ACCOUNTS)!!
      .get(testData.user.id)
      ?.get() as UserAccountDto?
}

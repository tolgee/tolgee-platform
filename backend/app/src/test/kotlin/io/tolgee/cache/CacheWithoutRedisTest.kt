package io.tolgee.cache

import io.tolgee.testing.assertions.Assertions.assertThat
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.testng.annotations.Test

@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ]
)
class CacheWithoutRedisTest : AbstractCacheTest() {
  @Test
  fun `it has proper cache manager`() {
    assertThat(cacheManager).isInstanceOf(CaffeineCacheManager::class.java)
  }
}

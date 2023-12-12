package io.tolgee.cache

import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.caffeine.CaffeineCacheManager

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
    "tolgee.internal.fake-mt-providers=false",
    "disable-server-app-test-mock-overrides=true"
  ]
)
class CacheWithoutRedisTest : AbstractCacheTest() {
  @Test
  fun `it has proper cache manager`() {
    assertThat(unwrappedCacheManager).isInstanceOf(CaffeineCacheManager::class.java)
  }
}

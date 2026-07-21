package io.tolgee.unit.cache

import io.tolgee.configuration.TolgeeRedissonSpringCacheManager
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.redisson.api.RedissonClient
import org.redisson.spring.cache.CacheConfig

class TolgeeRedissonSpringCacheManagerTest {
  @Test
  fun `applies the configured default ttl to every cache, even unregistered ones`() {
    val manager = TolgeeRedissonSpringCacheManager(mock<RedissonClient>(), 4242L)

    val createDefaultConfig =
      TolgeeRedissonSpringCacheManager::class.java
        .getDeclaredMethod("createDefaultConfig")
        .apply { isAccessible = true }
    val config = createDefaultConfig.invoke(manager) as CacheConfig

    config.ttl.assert.isEqualTo(4242L)
    config.maxIdleTime.assert.isEqualTo(4242L)
  }
}

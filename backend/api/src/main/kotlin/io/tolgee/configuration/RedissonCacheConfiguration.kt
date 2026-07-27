package io.tolgee.configuration

import io.tolgee.component.EnumNameKryo5Codec
import io.tolgee.component.cache.CacheFingerprintRegistry
import io.tolgee.component.cache.FingerprintingCacheManager
import io.tolgee.configuration.tolgee.TolgeeProperties
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.RedisOperations

@Configuration
@EnableCaching
@ConditionalOnClass(Redisson::class, RedisOperations::class)
@AutoConfigureAfter(ConditionalRedissonAutoConfiguration::class)
@ConditionalOnExpression("\${tolgee.cache.use-redis:false} and \${tolgee.cache.enabled:false}")
class RedissonCacheConfiguration(
  private val tolgeeProperties: TolgeeProperties,
  private val cacheFingerprintRegistry: CacheFingerprintRegistry,
) {
  @Bean
  fun cacheManager(redissonClient: RedissonClient): CacheManager {
    val redissonCacheManager =
      TolgeeRedissonSpringCacheManager(
        redissonClient,
        tolgeeProperties.cache.defaultTtl,
        EnumNameKryo5Codec(),
      )
    return FingerprintingCacheManager(
      TransactionAwareCacheManagerProxy(redissonCacheManager),
      cacheFingerprintRegistry,
    )
  }
}

package io.tolgee.configuration

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.redisson.Redisson
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.cache.CacheManager
import org.springframework.cache.support.NoOpCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.RedisOperations

@Configuration
@ConditionalOnClass(Redisson::class, RedisOperations::class)
@AutoConfigureAfter(ConditionalRedissonAutoConfiguration::class)
@ConditionalOnExpression("\${tolgee.cache.enabled:false} == false")
class NoOpCacheConfiguration(
  val tolgeeProperties: TolgeeProperties,
) {
  @Bean
  fun cacheManager(): CacheManager {
    return NoOpCacheManager()
  }
}

package io.tolgee.configuration

import com.github.benmanes.caffeine.cache.Caffeine
import io.tolgee.configuration.tolgee.TolgeeProperties
import org.redisson.Redisson
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.RedisOperations
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
@ConditionalOnClass(Redisson::class, RedisOperations::class)
@AutoConfigureAfter(ConditionalRedissonAutoConfiguration::class)
@ConditionalOnExpression("\${tolgee.cache.use-redis:false} == false and \${tolgee.cache.enabled:false}")
class CaffeineCacheConfiguration(
  val tolgeeProperties: TolgeeProperties,
) {
  @Bean
  fun caffeineConfig(): Caffeine<Any, Any> {
    val builder =
      Caffeine
        .newBuilder()
        .expireAfterWrite(tolgeeProperties.cache.defaultTtl, TimeUnit.MILLISECONDS)
        .expireAfterAccess(tolgeeProperties.cache.defaultTtl, TimeUnit.MILLISECONDS)
    if (tolgeeProperties.cache.caffeineMaxSize > 0) {
      builder.maximumSize(tolgeeProperties.cache.caffeineMaxSize)
    }
    return builder
  }

  @Bean
  fun cacheManager(caffeine: Caffeine<Any, Any>): CacheManager {
    val caffeineCacheManager = CaffeineCacheManager()
    caffeineCacheManager.setCaffeine(caffeine)
    return TransactionAwareCacheManagerProxy(caffeineCacheManager)
  }
}

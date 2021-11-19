package io.tolgee.configuration

import io.tolgee.component.lockingProvider.LockingProvider
import io.tolgee.component.lockingProvider.RedissonLockingProvider
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.RedisOperations

@Configuration
@ConditionalOnClass(Redisson::class, RedisOperations::class)
@AutoConfigureAfter(ConditionalRedissonAutoConfiguration::class)
@ConditionalOnProperty(name = ["tolgee.cache.use-redis"], havingValue = "true")
class RedisLockingConfiguration(
  val redissonClient: RedissonClient
) {
  @Bean
  fun getLockingProvider(): LockingProvider {
    return RedissonLockingProvider(redissonClient)
  }
}

package io.tolgee.batch.state

import io.tolgee.component.LockingProvider
import io.tolgee.component.UsingRedisProvider
import org.redisson.api.RedissonClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy

/**
 * Configuration that provides the appropriate BatchJobStateProvider implementation
 * based on whether Redis is being used.
 */
@Configuration
class BatchJobStateConfiguration(
  private val usingRedisProvider: UsingRedisProvider,
  private val initializer: BatchJobStateInitializer,
  private val lockingProvider: LockingProvider,
  @Lazy
  private val redissonClient: RedissonClient,
) {
  @Bean
  fun batchJobStateProvider(): BatchJobStateProvider {
    return if (usingRedisProvider.areWeUsingRedis) {
      RedisBatchJobStateStorage(initializer, lockingProvider, redissonClient)
    } else {
      LocalBatchJobStateStorage(initializer, lockingProvider)
    }
  }
}

package io.tolgee.configuration

import io.tolgee.component.LockingProvider
import io.tolgee.component.lockingProvider.SimpleLockingProvider
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@AutoConfigureAfter(RedisLockingConfiguration::class)
class SimpleLockingConfiguration {
  @ConditionalOnMissingBean(LockingProvider::class)
  @Bean
  fun simpleLockingProvider(): LockingProvider {
    return SimpleLockingProvider()
  }
}

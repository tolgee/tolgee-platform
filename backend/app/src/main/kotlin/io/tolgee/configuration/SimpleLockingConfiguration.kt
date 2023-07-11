package io.tolgee.configuration

import io.tolgee.component.LockingProvider
import io.tolgee.component.lockingProvider.SimpleLockingProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnMissingBean(LockingProvider::class)
class SimpleLockingConfiguration {
  @Bean
  fun getLockingProvider(): LockingProvider {
    return SimpleLockingProvider()
  }
}

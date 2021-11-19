package io.tolgee.configuration

import io.tolgee.component.lockingProvider.LockingProvider
import io.tolgee.component.lockingProvider.SimpleLockingProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnExpression("\${tolgee.cache.use-redis:false} == false")
class SimpleLockingConfiguration {
  @Bean
  fun getLockingProvider(): LockingProvider {
    return SimpleLockingProvider()
  }
}

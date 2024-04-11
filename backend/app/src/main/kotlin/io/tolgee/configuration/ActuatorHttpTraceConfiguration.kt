package io.tolgee.configuration

import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ActuatorHttpTraceConfiguration {
  @Bean
  fun createTraceRepository(): InMemoryHttpExchangeRepository {
    return InMemoryHttpExchangeRepository()
  }
}

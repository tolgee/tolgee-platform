package io.tolgee.postgresRunners

import io.tolgee.PostgresRunner
import io.tolgee.configuration.tolgee.PostgresAutostartProperties.PostgresAutostartMode
import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PostgresRunnerConfiguration {
  @Bean
  fun postgresRunner(tolgeeProperties: TolgeeProperties): PostgresRunner? {
    val postgresAutostartProperties = tolgeeProperties.postgresAutostart
    if (!postgresAutostartProperties.enabled) {
      return null
    }
    if (postgresAutostartProperties.mode == PostgresAutostartMode.DOCKER) {
      return PostgresDockerRunner(tolgeeProperties)
    }
    if (postgresAutostartProperties.mode == PostgresAutostartMode.EMBEDDED) {
      return PostgresEmbeddedRunner(tolgeeProperties)
    }
    return null
  }
}

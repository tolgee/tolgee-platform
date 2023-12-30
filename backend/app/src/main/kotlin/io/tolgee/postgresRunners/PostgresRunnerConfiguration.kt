package io.tolgee.postgresRunners

import io.tolgee.PostgresRunner
import io.tolgee.configuration.tolgee.FileStorageProperties
import io.tolgee.configuration.tolgee.PostgresAutostartProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PostgresRunnerConfiguration {
  @Bean
  fun postgresRunner(
    postgresAutostartProperties: PostgresAutostartProperties,
    storageProperties: FileStorageProperties,
  ): PostgresRunner? {
    if (!postgresAutostartProperties.enabled) {
      return null
    }
    if (postgresAutostartProperties.mode == PostgresAutostartProperties.PostgresAutostartMode.DOCKER) {
      return PostgresDockerRunner(postgresAutostartProperties)
    }
    if (postgresAutostartProperties.mode == PostgresAutostartProperties.PostgresAutostartMode.EMBEDDED) {
      return PostgresEmbeddedRunner(postgresAutostartProperties, storageProperties)
    }
    return null
  }
}

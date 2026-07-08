package io.tolgee.commandLineRunners

import io.tolgee.configuration.tolgee.PostgresAutostartProperties
import io.tolgee.configuration.tolgee.PostgresAutostartProperties.PostgresAutostartMode
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class EmbeddedPostgresDeprecationCommandLineRunner(
  private val postgresAutostartProperties: PostgresAutostartProperties,
) : CommandLineRunner {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun run(vararg args: String) {
    if (!postgresAutostartProperties.enabled || postgresAutostartProperties.mode != PostgresAutostartMode.EMBEDDED) {
      return
    }

    logger.warn(
      "The embedded PostgreSQL bundled in the tolgee/tolgee image is deprecated and will be removed in a future " +
        "major version. Migrate to an external database and use the slim tolgee/tolgee image. " +
        "See https://docs.tolgee.io/platform/self_hosting/running_with_docker" +
        "#running-with-docker-compose-with-external-postgresql-database",
    )
  }
}

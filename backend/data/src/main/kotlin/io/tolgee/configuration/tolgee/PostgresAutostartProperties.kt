package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.postgres-autostart")
class PostgresAutostartProperties {
  var enabled: Boolean = true
  var mode: PostgresAutostartMode = PostgresAutostartMode.DOCKER
  var user: String = "postgres"
  var password: String = "postgres"
  var databaseName: String = "postgres"
  var port: String = "25432"
  var containerName: String = "tolgee_postgres"

  enum class PostgresAutostartMode {
    /**
     * Starts docker container with postgres
     */
    DOCKER,

    /**
     * Expects that postgres is installed in the same container.
     * So the Postgres is started with Tolgee.
     */
    EMBEDDED
  }
}

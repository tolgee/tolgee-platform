package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.postgres-autostart")
@DocProperty(
  description = "Defines whether and how is PostgreSQL started on Tolgee startup.",
  displayName = "Postgres autostart",
)
class PostgresAutostartProperties {
  @DocProperty(description = "Whether to start PostgreSQL on Tolgee startup.")
  var enabled: Boolean = true

  @DocProperty(
    description =
      "How is Tolgee running PostgreSQL.\n" +
        "\n" +
        "Options:\n" +
        "- `DOCKER` - Tolgee tries to run Postgres Docker container in your machine. " +
        "This is default option when running Tolgee using Java. " +
        "See [Running with Java](/self_hosting/running_with_java.mdx).\n" +
        "- `EMBEDDED` - Tolgee tries to run it's embedded PostgreSQL " +
        "which is bundled in the `tolgee/tolgee` Docker image.",
  )
  var mode: PostgresAutostartMode = PostgresAutostartMode.DOCKER

  @DocProperty(description = "Database user to bootstrap Postgres with.")
  var user: String = "postgres"

  @DocProperty(description = "Database password to bootstrap Postgres with.")
  var password: String = "postgres"

  @DocProperty(description = "The name of the database created to store Tolgee data.")
  var databaseName: String = "postgres"

  @DocProperty(description = "The max time to wait for running postgres in seconds.")
  var maxWaitTime: Long = 300

  @DocProperty(
    description =
      "The port of Postgres to listen on host machine.\n" +
        "This setting is applicable only for `DOCKER` mode.",
  )
  var port: String = "25432"

  @DocProperty(
    description =
      "The container name of the Postgres container.\n" +
        "This setting is applicable only for `DOCKER` mode.",
  )
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
    EMBEDDED,
  }

  @DocProperty(
    description =
      "When true, Tolgee will stop the Postgres container on Tolgee shutdown. " +
        "This setting is applicable only for `DOCKER` mode.",
  )
  var stop: Boolean = true
}

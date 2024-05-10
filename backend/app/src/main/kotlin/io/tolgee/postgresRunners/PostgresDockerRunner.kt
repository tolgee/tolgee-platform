package io.tolgee.postgresRunners

import io.tolgee.PostgresRunner
import io.tolgee.configuration.tolgee.PostgresAutostartProperties
import io.tolgee.misc.dockerRunner.DockerContainerRunner
import org.slf4j.LoggerFactory

class PostgresDockerRunner(
  private val postgresAutostartProperties: PostgresAutostartProperties,
) : PostgresRunner {
  private var instance: DockerContainerRunner? = null
  private val logger = LoggerFactory.getLogger(javaClass)

  override fun run() {
    instance =
      DockerContainerRunner(
        image = "postgres:16.3",
        expose = mapOf(postgresAutostartProperties.port to "5432"),
        waitForLog = "database system is ready to accept connections",
        waitForLogTimesForNewContainer = 2,
        waitForLogTimesForExistingContainer = 1,
        rm = false,
        name = postgresAutostartProperties.containerName,
        stopBeforeStart = false,
        env =
          mapOf(
            "POSTGRES_PASSWORD" to postgresAutostartProperties.password,
            "POSTGRES_USER" to postgresAutostartProperties.user,
            "POSTGRES_DB" to postgresAutostartProperties.databaseName,
          ),
        command =
          "postgres -c max_connections=10000 -c random_page_cost=1.0 " +
            "-c fsync=off -c synchronous_commit=off -c full_page_writes=off",
        timeout = 300000,
      ).also {
        logger.info("Starting Postgres Docker container")
        it.run()
      }
  }

  override fun stop() {
    if (postgresAutostartProperties.stop) {
      instance?.let {
        logger.info("Stopping Postgres container")
        it.stop()
      }
    }
  }

  override val shouldRunMigrations: Boolean
    // we don't want to run migrations when the container existed, and we are not stopping it,
    // this happens only for tests and there we can delete the database and start again with migrations
    get() = instance?.containerExisted != true || postgresAutostartProperties.stop

  override val datasourceUrl by lazy {
    "jdbc:postgresql://localhost:${postgresAutostartProperties.port}/${postgresAutostartProperties.databaseName}"
  }
}

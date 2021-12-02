package io.tolgee.postgresStarters

import io.tolgee.configuration.tolgee.PostgresAutostartProperties
import io.tolgee.misc.dockerRunner.DockerContainerRunner
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import javax.annotation.PreDestroy

@Component
@Scope(SCOPE_SINGLETON)
class PostgresDockerRunner(
  protected val postgresAutostartProperties: PostgresAutostartProperties,
) : PostgresRunner {
  private var instance: DockerContainerRunner? = null
  private val logger = LoggerFactory.getLogger(javaClass)

  override fun run() {
    instance = DockerContainerRunner(
      image = "postgres:13",
      expose = mapOf(postgresAutostartProperties.port to "5432"),
      name = postgresAutostartProperties.containerName,
      waitForLog = "database system is ready to accept connections",
      waitForLogTimesForNewContainer = 2,
      waitForLogTimesForExistingContainer = 1,
      timeout = 300000,
      rm = false,
      env = mapOf(
        "POSTGRES_PASSWORD" to postgresAutostartProperties.password,
        "POSTGRES_USER" to postgresAutostartProperties.user,
        "POSTGRES_DB" to postgresAutostartProperties.databaseName,
      ),
      command = "postgres -c max_connections=10000",
      stopBeforeStart = false
    ).also {
      logger.info("Running Postgres Docker container. This may take some time...")
      it.run()
    }
  }

  override val datasourceUrl by lazy {
    "jdbc:postgresql://localhost:${postgresAutostartProperties.port}/${postgresAutostartProperties.databaseName}"
  }

  @PreDestroy
  fun preDestroy() {
    instance?.let {
      logger.info("Stopping Postgres container")
      it.stop()
    }
  }
}

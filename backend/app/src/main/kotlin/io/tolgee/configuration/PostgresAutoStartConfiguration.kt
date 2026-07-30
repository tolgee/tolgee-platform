package io.tolgee.configuration

import io.tolgee.PostgresRunner
import io.tolgee.configuration.tolgee.PostgresAutostartProperties
import io.tolgee.util.Logging
import org.apache.commons.lang3.exception.ExceptionUtils
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.sql.SQLException
import javax.sql.DataSource

@Configuration
@ConditionalOnProperty(name = ["tolgee.postgres-autostart.enabled"], havingValue = "true")
class PostgresAutoStartConfiguration(
  val postgresAutostartProperties: PostgresAutostartProperties,
) : Logging {
  private var dataSource: DataSource? = null

  @Bean("dataSource")
  @ConfigurationProperties(prefix = "spring.datasource")
  fun getDataSource(postgresRunner: PostgresRunner?): DataSource {
    postgresRunner ?: throw IllegalStateException("Postgres runner is not initialized")
    dataSource?.let { return it }
    postgresRunner.run()
    waitForPostgresRunning(postgresRunner)
    dataSource = buildDataSource(postgresRunner)
    return dataSource!!
  }

  private fun buildDataSource(postgresRunner: PostgresRunner): DataSource {
    val dataSourceBuilder = DataSourceBuilder.create()
    dataSourceBuilder.url(postgresRunner.datasourceUrl)
    dataSourceBuilder.username(postgresAutostartProperties.user)
    dataSourceBuilder.password(postgresAutostartProperties.password)
    return dataSourceBuilder.build()
  }

  /**
   * Postgres reports 57P03 while it is still starting; the message it pairs with that state is
   * translated by lc_messages, so matching the text fails on a non-English server.
   */
  internal fun isStartingUp(e: Throwable): Boolean =
    ExceptionUtils.getThrowableList(e).any { it is SQLException && it.sqlState == POSTGRES_CANNOT_CONNECT_NOW }

  private fun waitForPostgresRunning(postgresRunner: PostgresRunner) {
    val localDataSource = buildDataSource(postgresRunner)
    val maxRetries = postgresAutostartProperties.maxWaitTime
    val retryInterval = 1000L
    var numTries = 0
    while (numTries < maxRetries) {
      try {
        localDataSource.connection?.use { conn ->
          val statement = conn.createStatement()
          statement.executeQuery("SELECT 1") // Execute a simple SQL statement
        }
        // If we got this far without an exception, break the loop
        break
      } catch (e: Exception) {
        if (!isStartingUp(e)) {
          throw e
        }
        // Wait and then try again
        Thread.sleep(retryInterval)
        numTries++
      }
    }
  }

  companion object {
    private const val POSTGRES_CANNOT_CONNECT_NOW = "57P03"
  }
}

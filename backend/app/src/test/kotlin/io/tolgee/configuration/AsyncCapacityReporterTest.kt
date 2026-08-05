package io.tolgee.configuration

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.zaxxer.hikari.HikariDataSource
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class AsyncCapacityReporterTest {
  @Test
  fun `stays quiet when a quarter of the connection pool is left for ordinary requests`() {
    val events = report(connectionPoolSize = 100, batchConcurrency = 20)

    events.warnings.assert.isEmpty()
    events.infos.assert.anyMatch { it.contains("100 database connections") }
  }

  @Test
  fun `warns when the pools plus batch jobs crowd out ordinary requests`() {
    val events = report(connectionPoolSize = 100, batchConcurrency = 40)

    events.warnings.assert.anyMatch { it.contains("may starve the database connection pool") }
  }

  @Test
  fun `names the connection pool property that the active binding mode actually reads`() {
    report(connectionPoolSize = 100, batchConcurrency = 40, postgresAutostart = true)
      .warnings.assert
      .anyMatch { it.contains("spring.datasource.maximum-pool-size") }

    report(connectionPoolSize = 100, batchConcurrency = 40, postgresAutostart = false)
      .warnings.assert
      .anyMatch { it.contains("spring.datasource.hikari.maximum-pool-size") }
  }

  @Test
  fun `sits exactly on the boundary without warning, and warns one job past it`() {
    report(connectionPoolSize = 60, batchConcurrency = 15).warnings.assert.isEmpty()
    report(connectionPoolSize = 60, batchConcurrency = 16).warnings.assert.isNotEmpty()
  }

  @Test
  fun `reports without a warning when the DataSource cannot report a pool size`() {
    val events = report(connectionPoolSize = null, batchConcurrency = 40)

    events.warnings.assert.isEmpty()
    events.infos.assert.anyMatch { it.contains("streaming threads") }
  }

  private fun report(
    connectionPoolSize: Int?,
    batchConcurrency: Int,
    postgresAutostart: Boolean = true,
  ): CapturedLog {
    val properties = TolgeeProperties()
    properties.batch.concurrency = batchConcurrency
    properties.postgresAutostart.enabled = postgresAutostart

    val dataSource =
      connectionPoolSize?.let {
        HikariDataSource().apply { maximumPoolSize = it }
      }
    val reporter = AsyncCapacityReporter(AsyncExecutorFactory(properties, providerOf(dataSource)), properties)

    val logger = LoggerFactory.getLogger(AsyncCapacityReporter::class.java) as Logger
    val appender = ListAppender<ILoggingEvent>().apply { start() }
    logger.addAppender(appender)
    try {
      reporter.report()
    } finally {
      logger.detachAppender(appender)
    }
    return CapturedLog(appender.list)
  }

  private class CapturedLog(
    events: List<ILoggingEvent>,
  ) {
    val warnings = events.filter { it.level == Level.WARN }.map { it.formattedMessage }
    val infos = events.filter { it.level == Level.INFO }.map { it.formattedMessage }
  }
}

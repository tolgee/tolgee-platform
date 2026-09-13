package io.tolgee.configuration

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.mock.env.MockEnvironment

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
    // 60 -> 20 streaming + 10 background + 2 serial, reserve 60/4 = 15.
    report(connectionPoolSize = 60, batchConcurrency = 13).warnings.assert.isEmpty()
    report(connectionPoolSize = 60, batchConcurrency = 14).warnings.assert.isNotEmpty()
  }

  /** The warning spells the sum out term by term; one dropped term silently misleads the operator. */
  @Test
  fun `the warned equation adds up to the total it prints`() {
    val warning = report(connectionPoolSize = 100, batchConcurrency = 40).warnings.single()

    val terms =
      Regex("""(\d+) (?:streaming|background|batch|serial) """)
        .findAll(warning)
        .map { it.groupValues[1].toInt() }
        .toList()
    val total = Regex("""= (\d+) against""").find(warning)!!.groupValues[1].toInt()

    terms.assert.hasSize(4)
    terms.sum().assert.isEqualTo(total)
  }

  @Test
  fun `reports without a warning when the connection pool size is not configured`() {
    val events = report(connectionPoolSize = null, batchConcurrency = 40)

    events.warnings.assert.isEmpty()
    events.infos.assert.anyMatch { it.contains("streaming threads") }
  }

  /** Guards the two divisors against the reporter's reserve without restating either. */
  @Test
  fun `the shipped derivation never trips the reporter`() {
    listOf(10, 20, 30, 60, 100, 200).forEach { poolSize ->
      report(connectionPoolSize = poolSize, batchConcurrency = 1)
        .warnings.assert
        .describedAs("connection pool of $poolSize")
        .isEmpty()
    }
  }

  private fun report(
    connectionPoolSize: Int?,
    batchConcurrency: Int,
    postgresAutostart: Boolean = true,
  ): CapturedLog {
    val properties = TolgeeProperties()
    properties.batch.concurrency = batchConcurrency
    properties.postgresAutostart.enabled = postgresAutostart

    val environment = MockEnvironment()
    connectionPoolSize?.let { environment.setProperty(poolSizeProperty(postgresAutostart), it.toString()) }
    val reporter = AsyncCapacityReporter(AsyncExecutorFactory(properties, environment), properties)

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

  private fun poolSizeProperty(postgresAutostart: Boolean): String {
    if (postgresAutostart) return AsyncExecutorFactory.AUTOSTART_POOL_SIZE_PROPERTY
    return AsyncExecutorFactory.HIKARI_POOL_SIZE_PROPERTY
  }

  private class CapturedLog(
    events: List<ILoggingEvent>,
  ) {
    val warnings = events.filter { it.level == Level.WARN }.map { it.formattedMessage }
    val infos = events.filter { it.level == Level.INFO }.map { it.formattedMessage }
  }
}

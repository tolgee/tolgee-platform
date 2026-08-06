package io.tolgee.configuration

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class AsyncCapacityReporter(
  private val asyncExecutorFactory: AsyncExecutorFactory,
  private val tolgeeProperties: TolgeeProperties,
) : Logging {
  @EventListener(ApplicationReadyEvent::class)
  fun report() {
    val streaming = asyncExecutorFactory.streamingMaxThreads
    val streamingQueue = asyncExecutorFactory.streamingQueueCapacity
    val background = asyncExecutorFactory.backgroundMaxThreads
    val batch = tolgeeProperties.batch.concurrency
    val connectionPoolSize = asyncExecutorFactory.connectionPoolSize

    if (connectionPoolSize == null) {
      logger.info(
        "Async capacity: $streaming streaming threads, $background background threads. The DataSource " +
          "does not report a pool size, so these were derived from an assumed " +
          "${AsyncExecutorFactory.FALLBACK_CONNECTION_POOL_SIZE} connections — set " +
          "tolgee.async.streaming.max-threads and tolgee.async.background.max-threads explicitly.",
      )
      return
    }

    logger.info(
      "Async capacity: $streaming streaming threads (queue $streamingQueue), $background background " +
        "threads, $SERIAL_POOLS serial pools, $batch batch jobs, $connectionPoolSize database connections.",
    )

    val reserved = streaming + background + batch + SERIAL_POOLS
    if (reserved <= connectionPoolSize - minimumSyncReserve(connectionPoolSize)) return

    logger.warn(
      "Async thread pools may starve the database connection pool: $streaming streaming + " +
        "$background background + $batch batch = $reserved against only $connectionPoolSize " +
        "connections. Every streaming response holds one connection for its whole duration, so " +
        "1/$SYNC_RESERVE_DIVISOR of the pool should stay free for ordinary requests. Raise " +
        "${connectionPoolSizeProperty()}, or lower tolgee.async.streaming.max-threads, " +
        "tolgee.async.background.max-threads and tolgee.batch.concurrency.",
    )
  }

  private fun minimumSyncReserve(connectionPoolSize: Int) = connectionPoolSize / SYNC_RESERVE_DIVISOR

  private fun connectionPoolSizeProperty(): String {
    if (tolgeeProperties.postgresAutostart.enabled) return "spring.datasource.maximum-pool-size"
    return "spring.datasource.hikari.maximum-pool-size"
  }

  companion object {
    const val SYNC_RESERVE_DIVISOR = 4

    /** The websocket and automation executors, one thread each. */
    const val SERIAL_POOLS = 2
  }
}

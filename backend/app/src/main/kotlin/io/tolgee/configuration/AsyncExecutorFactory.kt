package io.tolgee.configuration

import com.zaxxer.hikari.HikariDataSource
import io.sentry.spring7.SentryTaskDecorator
import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.beans.factory.ObjectProvider
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadPoolExecutor
import javax.sql.DataSource

@Component
class AsyncExecutorFactory(
  private val tolgeeProperties: TolgeeProperties,
  private val dataSourceProvider: ObjectProvider<DataSource>,
) {
  /**
   * Null when the DataSource is not a HikariDataSource, which is the only thing that can report a
   * pool size. Read from the live bean rather than from `spring.datasource.*`, because
   * PostgresAutoStartConfiguration binds those properties straight onto the HikariDataSource and so
   * which key wins depends on whether autostart is enabled.
   */
  val connectionPoolSize: Int? by lazy {
    (dataSourceProvider.ifAvailable as? HikariDataSource)?.maximumPoolSize
  }

  val streamingMaxThreads: Int
    get() = resolvePoolSize(tolgeeProperties.async.streaming.maxThreads, STREAMING_POOL_DIVISOR)

  val streamingQueueCapacity: Int
    get() {
      val configured = tolgeeProperties.async.streaming.queueCapacity
      if (configured >= 0) return configured
      return streamingMaxThreads
    }

  val backgroundMaxThreads: Int
    get() = resolvePoolSize(tolgeeProperties.async.background.maxThreads, BACKGROUND_POOL_DIVISOR)

  fun create(
    threadNamePrefix: String,
    maxThreads: Int,
    queueCapacity: Int,
    keepAliveSeconds: Int,
    rejectedExecutionHandler: RejectedExecutionHandler = ThreadPoolExecutor.AbortPolicy(),
  ): ThreadPoolTaskExecutor {
    return ThreadPoolTaskExecutor().apply {
      corePoolSize = maxThreads
      maxPoolSize = maxThreads
      this.queueCapacity = queueCapacity
      this.keepAliveSeconds = keepAliveSeconds
      // ThreadPoolExecutor rejects allowCoreThreadTimeOut with a non-positive keep-alive.
      setAllowCoreThreadTimeOut(keepAliveSeconds > 0)
      setThreadNamePrefix(threadNamePrefix)
      setRejectedExecutionHandler(rejectedExecutionHandler)
      setTaskDecorator(
        CompositeTaskDecorator(
          OtelContextTaskDecorator(),
          SentryTaskDecorator(),
        ),
      )
    }
  }

  private fun resolvePoolSize(
    configured: Int,
    divisor: Int,
  ): Int {
    if (configured > 0) return configured
    val poolSize = connectionPoolSize ?: FALLBACK_CONNECTION_POOL_SIZE
    return maxOf(MIN_POOL_SIZE, poolSize / divisor)
  }

  companion object {
    const val MIN_POOL_SIZE = 2
    const val STREAMING_POOL_DIVISOR = 3
    const val BACKGROUND_POOL_DIVISOR = 6
    const val FALLBACK_CONNECTION_POOL_SIZE = 10

    /** Matches the pre-existing @Async behaviour: background work is queued, never rejected. */
    const val UNBOUNDED_QUEUE = Int.MAX_VALUE

    const val STREAMING_THREAD_NAME_PREFIX = "tolgee-stream-"
    const val BACKGROUND_THREAD_NAME_PREFIX = "tolgee-async-"
    const val WEBSOCKET_THREAD_NAME_PREFIX = "tolgee-ws-"
  }
}

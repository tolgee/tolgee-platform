package io.tolgee.configuration

import io.sentry.spring7.SentryTaskDecorator
import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.core.env.Environment
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadPoolExecutor

@Component
class AsyncExecutorFactory(
  private val tolgeeProperties: TolgeeProperties,
  private val environment: Environment,
) {
  /**
   * PostgresAutoStartConfiguration binds the DataSource from `spring.datasource` and Boot's own
   * autoconfiguration binds it from `spring.datasource.hikari`; each ignores the other's key.
   */
  val connectionPoolSizeProperty: String
    get() {
      if (tolgeeProperties.postgresAutostart.enabled) return AUTOSTART_POOL_SIZE_PROPERTY
      return HIKARI_POOL_SIZE_PROPERTY
    }

  /**
   * Bound from configuration rather than read off the DataSource bean. The pools are sized while the
   * async infrastructure is still being assembled, and resolving that bean here would create the
   * DataSource at that point — starting Postgres under postgres-autostart — instead of when the
   * persistence layer asks for it.
   */
  val connectionPoolSize: Int? by lazy {
    Binder.get(environment).bind(connectionPoolSizeProperty, Int::class.javaObjectType).orElse(null)
  }

  val streamingMaxThreads: Int
    get() = resolvePoolSize(tolgeeProperties.async.streaming.maxThreads, STREAMING_POOL_DIVISOR)

  val streamingQueueCapacity: Int
    get() {
      val configured = tolgeeProperties.async.streaming.queueCapacity
      if (configured >= 0) return configured
      return maxOf(MIN_QUEUE_CAPACITY, streamingMaxThreads)
    }

  val backgroundMaxThreads: Int
    get() = resolvePoolSize(tolgeeProperties.async.background.maxThreads, BACKGROUND_POOL_DIVISOR)

  fun create(
    threadNamePrefix: String,
    maxThreads: Int,
    queueCapacity: Int,
    keepAliveSeconds: Int,
    drainOnShutdown: Boolean = true,
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
      // Making these beans gave them a shutdown they never had. Without this, a task submitted by a
      // request still in flight when the context starts closing is rejected into its caller.
      setAcceptTasksAfterContextClose(true)
      // Queued streams have a client that is already gone once the connector stops; background work
      // does not, so only that is worth draining.
      setWaitForTasksToCompleteOnShutdown(drainOnShutdown)
      setAwaitTerminationSeconds(SHUTDOWN_DRAIN_SECONDS)
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

    /** HikariCP's own default, which is what an instance that configures nothing ends up with. */
    const val FALLBACK_CONNECTION_POOL_SIZE = 10

    const val AUTOSTART_POOL_SIZE_PROPERTY = "spring.datasource.maximum-pool-size"
    const val HIKARI_POOL_SIZE_PROPERTY = "spring.datasource.hikari.maximum-pool-size"

    /** A burst is a burst regardless of how many threads drain it; 3 threads still deserve a buffer. */
    const val MIN_QUEUE_CAPACITY = 50

    /**
     * Spring destroys the pools one at a time, so this is paid once per pool. Four pools at 5s stay
     * inside Kubernetes' default terminationGracePeriodSeconds of 30, which the Tolgee chart does
     * not override — going over it means the kubelet SIGKILLs mid-drain and the wait buys nothing.
     */
    const val SHUTDOWN_DRAIN_SECONDS = 5

    const val UNBOUNDED_QUEUE = Int.MAX_VALUE

    const val STREAMING_THREAD_NAME_PREFIX = "tolgee-stream-"
    const val BACKGROUND_THREAD_NAME_PREFIX = "tolgee-async-"
    const val WEBSOCKET_THREAD_NAME_PREFIX = "tolgee-ws-"
    const val AUTOMATION_THREAD_NAME_PREFIX = "tolgee-automation-"
  }
}

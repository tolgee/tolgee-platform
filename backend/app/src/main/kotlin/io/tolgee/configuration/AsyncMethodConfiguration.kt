package io.tolgee.configuration

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
class AsyncMethodConfiguration(
  // ObjectProvider: AsyncConfigurers are collected while BeanPostProcessors are still being set up,
  // and eager injection of a @ConfigurationProperties bean here can bind it before
  // ConfigurationPropertiesBindingPostProcessor has run — silently yielding all-default values.
  private val asyncExecutorFactory: ObjectProvider<AsyncExecutorFactory>,
  private val tolgeeProperties: ObjectProvider<TolgeeProperties>,
) : AsyncConfigurer {
  override fun getAsyncExecutor(): Executor = backgroundAsyncExecutor()

  @Primary
  @Bean(BACKGROUND_EXECUTOR_BEAN_NAME)
  fun backgroundAsyncExecutor(): ThreadPoolTaskExecutor {
    val factory = asyncExecutorFactory.getObject()
    return factory.create(
      threadNamePrefix = AsyncExecutorFactory.BACKGROUND_THREAD_NAME_PREFIX,
      maxThreads = factory.backgroundMaxThreads,
      queueCapacity = AsyncExecutorFactory.UNBOUNDED_QUEUE,
      keepAliveSeconds =
        tolgeeProperties
          .getObject()
          .async.background.keepAliveSeconds,
    )
  }

  /**
   * Single-threaded, because the webapp applies activity events as ordered deltas: two edits of the
   * same key delivered out of order leave another user's editor showing a stale value. The queue is
   * unbounded for the same reason — any rejection policy would either drop an event or run it on the
   * caller ahead of everything already queued.
   */
  @Bean(WEBSOCKET_EXECUTOR_BEAN_NAME)
  fun websocketAsyncExecutor(): ThreadPoolTaskExecutor =
    asyncExecutorFactory.getObject().create(
      threadNamePrefix = AsyncExecutorFactory.WEBSOCKET_THREAD_NAME_PREFIX,
      maxThreads = 1,
      queueCapacity = AsyncExecutorFactory.UNBOUNDED_QUEUE,
      keepAliveSeconds = WEBSOCKET_KEEP_ALIVE_SECONDS,
    )

  companion object {
    const val BACKGROUND_EXECUTOR_BEAN_NAME = "backgroundAsyncExecutor"
    const val WEBSOCKET_EXECUTOR_BEAN_NAME = "websocketAsyncExecutor"
    const val WEBSOCKET_KEEP_ALIVE_SECONDS = 60
  }
}

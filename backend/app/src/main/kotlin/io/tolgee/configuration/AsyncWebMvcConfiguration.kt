package io.tolgee.configuration

import io.tolgee.Metrics
import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class AsyncWebMvcConfiguration(
  private val asyncExecutorFactory: AsyncExecutorFactory,
  private val tolgeeProperties: TolgeeProperties,
  private val metrics: Metrics,
) : WebMvcConfigurer {
  override fun configureAsyncSupport(configurer: AsyncSupportConfigurer) {
    // WebSecurityConfig disables AuthorizationFilter on ASYNC dispatch and every interceptor bails
    // there, so a streaming body can only reach the caller's principal through this wrapper.
    configurer.setTaskExecutor(DelegatingSecurityContextAsyncTaskExecutor(streamingAsyncExecutor()))
  }

  /** Kept unwrapped so Boot's executor metrics, which type-check the bean, still bind to it. */
  @Bean(STREAMING_EXECUTOR_BEAN_NAME)
  fun streamingAsyncExecutor(): ThreadPoolTaskExecutor =
    asyncExecutorFactory.create(
      threadNamePrefix = AsyncExecutorFactory.STREAMING_THREAD_NAME_PREFIX,
      maxThreads = asyncExecutorFactory.streamingMaxThreads,
      queueCapacity = asyncExecutorFactory.streamingQueueCapacity,
      keepAliveSeconds = tolgeeProperties.async.streaming.keepAliveSeconds,
      drainOnShutdown = false,
      rejectedExecutionHandler = StreamingAbortPolicy { metrics.streamingRejectedCounter.increment() },
    )

  companion object {
    const val STREAMING_EXECUTOR_BEAN_NAME = "streamingAsyncExecutor"
  }
}

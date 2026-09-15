package io.tolgee.configuration

import io.sentry.spring7.SentryTaskDecorator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.ThreadPoolExecutor

@Configuration
class SessionBookkeepingExecutorConfiguration {
  /**
   * The default `@Async` executor is single-threaded with an unbounded queue, so anything posted to
   * it delays every other async task behind it - Slack notifications, e-mails, websocket broadcasts.
   * Session last-used stamps happen on every authenticated request, so they get their own pool.
   *
   * The queue is bounded and overflow is discarded: a dropped stamp is re-issued by the next request
   * past the debounce interval, so losing one is cheaper than making callers wait.
   */
  @Bean(SessionBookkeepingExecutor.BEAN_NAME)
  fun sessionBookkeepingExecutor(): ThreadPoolTaskExecutor {
    return ThreadPoolTaskExecutor().apply {
      corePoolSize = 1
      maxPoolSize = 2
      queueCapacity = 1000
      setThreadNamePrefix("session-bookkeeping-")
      setRejectedExecutionHandler(ThreadPoolExecutor.DiscardPolicy())
      // the same tracing and error context every other executor in the app propagates
      setTaskDecorator(
        CompositeTaskDecorator(
          OtelContextTaskDecorator(),
          SentryTaskDecorator(),
        ),
      )
      initialize()
    }
  }
}

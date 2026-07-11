package io.tolgee.configuration

import io.sentry.spring.jakarta.SentryTaskDecorator
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
class AsyncMethodConfiguration : AsyncConfigurer {
  override fun getAsyncExecutor(): Executor {
    val executor = ThreadPoolTaskExecutor()
    // Chain decorators: OTEL context propagation + Sentry context propagation
    executor.setTaskDecorator(
      CompositeTaskDecorator(
        OtelContextTaskDecorator(),
        SentryTaskDecorator(),
      ),
    )
    executor.initialize()
    return executor
  }
}

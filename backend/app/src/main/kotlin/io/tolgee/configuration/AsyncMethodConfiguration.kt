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
    executor.setTaskDecorator(SentryTaskDecorator())
    executor.initialize()
    return executor
  }
}

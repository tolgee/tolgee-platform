package io.tolgee.configuration

import io.sentry.spring.SentryTaskDecorator
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurerSupport
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
internal class AsyncMethodConfiguration : AsyncConfigurerSupport() {
  override fun getAsyncExecutor(): Executor {
    val executor = ThreadPoolTaskExecutor()
    executor.setTaskDecorator(SentryTaskDecorator())
    executor.initialize()
    return executor
  }
}

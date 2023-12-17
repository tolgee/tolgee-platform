package io.tolgee.configuration

import io.sentry.spring.jakarta.SentryTaskDecorator
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class AsyncWebMvcConfiguration : WebMvcConfigurer {
  override fun configureAsyncSupport(configurer: AsyncSupportConfigurer) {
    configurer.setTaskExecutor(asyncExecutor())
  }

  private fun asyncExecutor(): AsyncTaskExecutor {
    val asyncExecutor = ThreadPoolTaskExecutor()
    asyncExecutor.setTaskDecorator(SentryTaskDecorator())
    asyncExecutor.initialize()
    return DelegatingSecurityContextAsyncTaskExecutor(asyncExecutor)
  }
}

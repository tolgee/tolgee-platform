package io.tolgee.configuration

import io.sentry.spring.SentryTaskDecorator
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
internal class AsyncWebMvcConfiguration : WebMvcConfigurer {
  override fun configureAsyncSupport(configurer: AsyncSupportConfigurer) {
    configurer.setTaskExecutor(asyncExecutor())
  }

  private fun asyncExecutor(): AsyncTaskExecutor {
    val executor = SecurityContextAwareThreadPoolTaskExecutor()
    executor.setTaskDecorator(SentryTaskDecorator())
    executor.initialize()
    return executor
  }

  class SecurityContextAwareThreadPoolTaskExecutor : ThreadPoolTaskExecutor() {
    override fun execute(task: Runnable) {
      val currentAuthentication = SecurityContextHolder.getContext().authentication
      super.execute {
        try {
          val ctx = SecurityContextHolder.createEmptyContext()
          ctx.authentication = currentAuthentication
          SecurityContextHolder.setContext(ctx)
          task.run()
        } finally {
          SecurityContextHolder.clearContext()
        }
      }
    }
  }
}

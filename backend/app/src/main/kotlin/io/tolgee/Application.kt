package io.tolgee

import io.tolgee.configuration.Banner
import org.redisson.spring.starter.RedissonAutoConfiguration
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication(exclude = [RedissonAutoConfiguration::class], scanBasePackages = ["io.tolgee"])
@EnableJpaAuditing
@EntityScan("io.tolgee.model")
@ConfigurationPropertiesScan
@EnableJpaRepositories("io.tolgee.repository")
class Application {
  companion object {
    private val log = LoggerFactory.getLogger(Application::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
      val app = SpringApplication(Application::class.java)
      app.applicationStartup = BufferingApplicationStartup(10000)
      app.setBanner(Banner())
      try {
        app.run(*args)
      } catch (e: Exception) {
        preventNonNullExitCodeOnSilentExitException(e)
      }
    }

    private fun preventNonNullExitCodeOnSilentExitException(e: Exception) {
      if (e.toString().contains("SilentExitException")) {
        log.info("Ignoring Silent Exit Exception...")
        return
      }
      throw e
    }
  }
}

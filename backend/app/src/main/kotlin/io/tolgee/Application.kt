package io.tolgee

import io.tolgee.configuration.Banner
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.development.DbPopulatorReal
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.security.InitialPasswordManager
import io.tolgee.service.UserAccountService
import org.redisson.spring.starter.RedissonAutoConfiguration
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication(exclude = [RedissonAutoConfiguration::class])
@EnableJpaAuditing
@ConfigurationPropertiesScan
class Application(
  populator: DbPopulatorReal,
  userAccountService: UserAccountService,
  properties: TolgeeProperties,
  initialPasswordManager: InitialPasswordManager,
) {
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

  init {
    if (properties.internal.populate) {
      populator.autoPopulate()
    }

    val initialUsername = properties.authentication.initialUsername
    if (properties.authentication.createInitialUser && !userAccountService.isAnyUserAccount &&
      userAccountService.findOptional(initialUsername).isEmpty
    ) {
      val initialPassword = initialPasswordManager.initialPassword
      userAccountService.createUser(
        SignUpDto(email = initialUsername, password = initialPassword, name = initialUsername)
      )
    }
  }
}

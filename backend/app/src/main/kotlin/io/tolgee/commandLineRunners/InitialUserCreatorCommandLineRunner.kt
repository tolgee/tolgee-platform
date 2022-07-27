package io.tolgee.commandLineRunners

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.model.UserAccount
import io.tolgee.security.InitialPasswordManager
import io.tolgee.service.UserAccountService
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.stereotype.Component

@Component
class InitialUserCreatorCommandLineRunner(
  private val properties: TolgeeProperties,
  private val userAccountService: UserAccountService,
  private val initialPasswordManager: InitialPasswordManager
) :
  CommandLineRunner, ApplicationListener<ContextClosedEvent> {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun run(vararg args: String) {
    val initialUsername = properties.authentication.initialUsername
    if (properties.authentication.createInitialUser && !userAccountService.isAnyUserAccount &&
      userAccountService.findOptional(initialUsername).isEmpty
    ) {
      logger.info("Creating initial user...")
      val initialPassword = initialPasswordManager.initialPassword
      userAccountService.createUser(
        SignUpDto(email = initialUsername, password = initialPassword, name = initialUsername),
        UserAccount.Role.ADMIN
      )
    }
  }

  override fun onApplicationEvent(event: ContextClosedEvent) {
    // we don't need this
  }
}

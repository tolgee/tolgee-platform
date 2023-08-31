package io.tolgee.commandLineRunners

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.dtos.request.organization.OrganizationDto
import io.tolgee.model.UserAccount
import io.tolgee.security.InitialPasswordManager
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.security.UserAccountService
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(0)
class InitialUserCreatorCommandLineRunner(
  private val properties: TolgeeProperties,
  private val userAccountService: UserAccountService,
  private val initialPasswordManager: InitialPasswordManager,
  private val organizationService: OrganizationService
) : CommandLineRunner, ApplicationListener<ContextClosedEvent> {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun run(vararg args: String) {
    if (userAccountService.findInitialUser() == null) {
      logger.info("Creating initial user...")

      val initialUsername = properties.authentication.initialUsername
      val initialPassword = initialPasswordManager.initialPassword
      val user = userAccountService.createUser(
        SignUpDto(email = initialUsername, password = initialPassword, name = initialUsername),
        UserAccount.Role.ADMIN
      )

      organizationService.create(
        OrganizationDto(
          properties.authentication.initialUsername,
        ),
        userAccount = user
      )
    }
  }

  override fun onApplicationEvent(event: ContextClosedEvent) {
    // we don't need this
  }
}

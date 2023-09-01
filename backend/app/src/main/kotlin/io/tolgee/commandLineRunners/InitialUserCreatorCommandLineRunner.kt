package io.tolgee.commandLineRunners

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.dtos.request.organization.OrganizationDto
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
    val initialUser = userAccountService.findInitialUser()
    if (initialUser == null) {
      logger.info("Creating initial user...")

      val initialUsername = properties.authentication.initialUsername
      val initialPassword = initialPasswordManager.initialPassword
      val user = userAccountService.createInitialUser(
        SignUpDto(email = initialUsername, password = initialPassword, name = initialUsername),
      )

      // If the user was already existing, it may already have assigned orgs.
      // To avoid conflicts, we only create the org if the user doesn't have any.
      organizationService.create(
        OrganizationDto(
          properties.authentication.initialUsername,
        ),
        userAccount = user
      )
    } else if (initialUser.username == "___implicit_user") {
      // Legacy installations might have promoted the implicit user as initial user.
      // If that's the case, we update its username, name and password to match what it'd have been otherwise.
      logger.info("Migrating initial user...")

      val initialUsername = properties.authentication.initialUsername
      val initialPassword = initialPasswordManager.initialPassword

      initialUser.username = initialUsername
      initialUser.name = initialUsername
      userAccountService.save(initialUser)
      userAccountService.setUserPassword(initialUser, initialPassword)
    }
  }

  override fun onApplicationEvent(event: ContextClosedEvent) {
    // we don't need this
  }
}

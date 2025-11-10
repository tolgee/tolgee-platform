package io.tolgee.commandLineRunners

import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.request.organization.OrganizationDto
import io.tolgee.model.UserAccount
import io.tolgee.security.InitialPasswordManager
import io.tolgee.service.QuickStartService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.security.UserAccountService
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.core.annotation.Order
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Order(0)
class InitialUserCreatorCommandLineRunner(
  private val properties: TolgeeProperties,
  private val userAccountService: UserAccountService,
  private val initialPasswordManager: InitialPasswordManager,
  private val organizationService: OrganizationService,
  private val passwordEncoder: PasswordEncoder,
  private val internalProperties: InternalProperties,
  private val quickStartService: QuickStartService,
) : CommandLineRunner,
  ApplicationListener<ContextClosedEvent> {
  private val logger = LoggerFactory.getLogger(this::class.java)

  @Transactional
  override fun run(vararg args: String) {
    if (internalProperties.disableInitialUserCreation) {
      return
    }
    val initialUser = userAccountService.findInitialUser()
    if (initialUser == null) {
      createInitialUser()
    } else if (!initialUser.passwordChanged) {
      updatePasswordIfNecessary(initialUser)
    }
  }

  fun createInitialUser() {
    logger.info("Creating initial user...")
    val initialUsername = properties.authentication.initialUsername

    // Check if the account already exists.
    // This can only be the case on Tolgee 3.x series and should be removed on Tolgee 4.
    val candidate = userAccountService.findActive(initialUsername)
    if (candidate != null) {
      candidate.isInitialUser = true
      userAccountService.save(candidate)
      return
    }

    val initialPassword = initialPasswordManager.initialPassword
    val user =
      UserAccount(
        username = initialUsername,
        password = passwordEncoder.encode(initialPassword),
        name = initialUsername,
        role = UserAccount.Role.ADMIN,
      ).apply {
        passwordChanged = false
        isInitialUser = true
      }

    userAccountService.createUser(userAccount = user)
    userAccountService.transferLegacyNoAuthUser()

    // If the user was already existing, it may already have assigned orgs.
    // To avoid conflicts, we only create the org if the user doesn't have any.
    val organization =
      organizationService.create(
        OrganizationDto(
          properties.authentication.initialUsername,
        ),
        userAccount = user,
      )

    if (properties.authentication.createDemoForInitialUser) {
      quickStartService.create(user, organization)
    }
  }

  fun updatePasswordIfNecessary(initialUser: UserAccount) {
    val initialPassword = initialPasswordManager.initialPassword
    if (!passwordEncoder.matches(initialPassword, initialUser.password)) {
      logger.info("Updating initial user password...")
      userAccountService.setUserPassword(initialUser, initialPassword)
    }
  }

  override fun onApplicationEvent(event: ContextClosedEvent) {
    // we don't need this
  }
}

package io.tolgee.service.apps

import io.tolgee.component.KeyGenerator
import io.tolgee.model.UserAccount
import io.tolgee.repository.UserAccountRepository
import io.tolgee.service.security.UserAccountService
import org.springframework.stereotype.Service

/**
 * Owns the account an app install acts as — see [io.tolgee.model.apps.AppInstall.principal].
 *
 * The row is created directly rather than through [UserAccountService.createUser]: nobody signed up,
 * no welcome mail is owed and no seat was taken, so none of the events that path publishes apply.
 */
@Service
class AppInstallPrincipalService(
  private val userAccountRepository: UserAccountRepository,
  private val userAccountService: UserAccountService,
  private val keyGenerator: KeyGenerator,
) {
  fun create(appName: String): UserAccount {
    val principal =
      UserAccount().apply {
        username = USERNAME_PREFIX + keyGenerator.generate(128)
        name = displayName(appName)
        role = UserAccount.Role.USER
        // Refuses the native sign-in path before any credential is compared, on top of being
        // filtered out of the lookup that finds an account by username at all.
        accountType = UserAccount.AccountType.MANAGED
        isAppPrincipal = true
      }
    return userAccountRepository.save(principal)
  }

  /**
   * Retires the principal of an install that is gone. Soft deletion is what keeps the rows the
   * install already wrote — comments, imports, batch jobs — pointing at something, exactly as it
   * does for a person who leaves.
   */
  fun retire(principal: UserAccount) {
    userAccountService.delete(principal)
  }

  companion object {
    const val USERNAME_PREFIX = "___app_"

    /** Long enough to leave the marker visible after truncation to the column's 255 characters. */
    private const val NAME_SUFFIX = " [app]"

    fun displayName(appName: String): String {
      return appName.take(255 - NAME_SUFFIX.length) + NAME_SUFFIX
    }
  }
}

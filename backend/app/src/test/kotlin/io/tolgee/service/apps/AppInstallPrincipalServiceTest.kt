package io.tolgee.service.apps

import io.tolgee.AbstractSpringTest
import io.tolgee.model.UserAccount
import io.tolgee.repository.UserAccountRepository
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * The account an app install acts as is a real user row, but never a person: it is MANAGED, flagged
 * [UserAccount.isAppPrincipal], and must stay out of every lookup that finds a human account.
 */
class AppInstallPrincipalServiceTest : AbstractSpringTest() {
  @Autowired
  private lateinit var appInstallPrincipalService: AppInstallPrincipalService

  @Autowired
  private lateinit var userAccountRepository: UserAccountRepository

  private var principalId: Long? = null

  @AfterEach
  fun cleanup() {
    principalId?.let { id ->
      executeInNewTransaction(platformTransactionManager) {
        userAccountRepository.deleteById(id)
      }
    }
  }

  @Test
  fun `creates a managed, app-principal account filtered from member lookups`() {
    val principal =
      executeInNewTransaction(platformTransactionManager) {
        appInstallPrincipalService.create("My Dashboard App")
      }
    principalId = principal.id

    principal.accountType.assert.isEqualTo(UserAccount.AccountType.MANAGED)
    principal.isAppPrincipal.assert.isTrue()
    principal.role.assert.isEqualTo(UserAccount.Role.USER)
    principal.username.assert.startsWith(AppInstallPrincipalService.USERNAME_PREFIX)
    principal.name.assert.endsWith("[app]")

    executeInNewTransaction(platformTransactionManager) {
      userAccountRepository.findActive(principal.username).assert.isNull()
      userAccountRepository.findActiveOrDisabled(principal.username).assert.isNull()
    }
  }
}

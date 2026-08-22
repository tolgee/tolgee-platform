package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.UserAccount
import io.tolgee.model.apps.AppInstall
import java.util.UUID

class AppInstallBuilder(
  val organizationBuilder: OrganizationBuilder,
) : BaseEntityDataBuilder<AppInstall, AppInstallBuilder>() {
  /**
   * The install's own account — see [AppInstall.principal]. Created and persisted as a real,
   * MANAGED, app-principal [UserAccount] so every user foreign key an install writes resolves,
   * and cleaned up with the rest of the graph by its username in [TestDataService.cleanTestData].
   */
  val principalBuilder: UserAccountBuilder =
    UserAccountBuilder(organizationBuilder.testDataBuilder).apply {
      rawPassword = null
      self.apply {
        username = PRINCIPAL_USERNAME_PREFIX + UUID.randomUUID().toString().replace("-", "")
        name = "Test App [app]"
        role = UserAccount.Role.USER
        accountType = UserAccount.AccountType.MANAGED
        isAppPrincipal = true
      }
    }

  override var self: AppInstall =
    AppInstall().apply {
      organization = organizationBuilder.self
      principal = principalBuilder.self
    }

  init {
    organizationBuilder.testDataBuilder.data.userAccounts
      .add(principalBuilder)
  }

  companion object {
    const val PRINCIPAL_USERNAME_PREFIX = "___app_"
  }
}

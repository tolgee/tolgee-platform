package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.UserAccount

open class NativeAppsTestData : AppsTestData() {
  lateinit var admin: UserAccount
  lateinit var supporter: UserAccount

  init {
    root.apply {
      admin =
        addUserAccount {
          username = "apps-test-admin@test.com"
          role = UserAccount.Role.ADMIN
        }.self

      supporter =
        addUserAccount {
          username = "apps-test-supporter@test.com"
          role = UserAccount.Role.SUPPORTER
        }.self
    }
  }
}

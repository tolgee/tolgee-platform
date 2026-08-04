package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.UserAccount

class NativeAppsTestData : AppsTestData() {
  lateinit var admin: UserAccount

  init {
    root.apply {
      admin =
        addUserAccount {
          username = "apps-test-admin@test.com"
          role = UserAccount.Role.ADMIN
        }.self
    }
  }
}

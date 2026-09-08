package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.UserAccount

class TestClockTestData : BaseTestData() {
  lateinit var admin: UserAccount

  init {
    root.addUserAccount {
      username = "admin@test-clock.com"
      role = UserAccount.Role.ADMIN
      admin = this
    }
  }
}

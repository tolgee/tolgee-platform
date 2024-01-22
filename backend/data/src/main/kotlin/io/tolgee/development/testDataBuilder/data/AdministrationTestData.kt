package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.UserAccount

class AdministrationTestData {
  lateinit var admin: UserAccount
  lateinit var user: UserAccount

  val root =
    TestDataBuilder().apply {
      addUserAccount {
        username = "admin@admin.com"
        name = "Peter Administrator"
        role = UserAccount.Role.ADMIN
        admin = this
      }

      addUserAccount {
        username = "user@user.com"
        name = "John User"
        role = UserAccount.Role.USER
        user = this
      }
    }
}

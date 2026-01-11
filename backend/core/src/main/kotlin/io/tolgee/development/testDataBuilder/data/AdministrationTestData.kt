package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.builders.UserAccountBuilder
import io.tolgee.model.UserAccount

class AdministrationTestData {
  lateinit var admin: UserAccount
  lateinit var supporter: UserAccount
  lateinit var user: UserAccount
  var adminBuilder: UserAccountBuilder

  val root =
    TestDataBuilder().apply {
      adminBuilder =
        addUserAccount {
          username = "admin@admin.com"
          name = "Peter Administrator"
          role = UserAccount.Role.ADMIN
          admin = this
        }

      addUserAccount {
        username = "supporter@supporter.com"
        name = "Matthew Supporter"
        role = UserAccount.Role.SUPPORTER
        supporter = this
      }

      addUserAccount {
        username = "user@user.com"
        name = "John User"
        role = UserAccount.Role.USER
        user = this
      }
    }
}

package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.UserAccount

class UserAccountUsernameUniqueIndexTestData : BaseTestData() {
  val existingUser: UserAccount = root.addUserAccount { username = "ciunique@test.com" }.self
}

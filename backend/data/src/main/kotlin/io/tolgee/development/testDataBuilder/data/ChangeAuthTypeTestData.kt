package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.UserAccount

class ChangeAuthTypeTestData: BaseTestData() {

  var userExisting: UserAccount

  val createUserExisting: TestDataBuilder =
    TestDataBuilder().apply {
      userExisting = addUserAccount {
        name = "fake_email@email.com"
        username = "fake_email@email.com"
      }.self
    }
}

package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.UserAccount

class SensitiveOperationProtectionTestData {
  companion object {
    val TOTP_KEY = "meowmeowmeowmeow".toByteArray()
  }

  lateinit var franta: UserAccount
  lateinit var pepa: UserAccount

  val root = TestDataBuilder {
    addUserAccount {
      username = "franta"
      name = "Franta"
      franta = this
    }.build {
    }
    addUserAccount {
      username = "pepa"
      name = "Pepa"
      totpKey = TOTP_KEY
      pepa = this
    }
  }
}

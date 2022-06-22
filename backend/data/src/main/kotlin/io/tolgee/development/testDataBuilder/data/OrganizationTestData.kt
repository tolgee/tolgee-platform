package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.UserAccount

class OrganizationTestData : BaseTestData() {
  lateinit var franta: UserAccount

  init {
    root.apply {
      addUserAccount {
        name = "Franta Kocourek"
        username = "to.si@proladite.krkovicku"
        franta = this
      }
      projectBuilder.addPermission {
        user = franta
      }
    }
  }
}

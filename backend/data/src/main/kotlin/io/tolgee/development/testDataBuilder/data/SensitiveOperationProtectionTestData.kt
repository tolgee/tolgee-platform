package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Project
import io.tolgee.model.UserAccount

class SensitiveOperationProtectionTestData {
  companion object {
    val TOTP_KEY = "meowmeowmeowmeow".toByteArray()
  }

  lateinit var franta: UserAccount
  lateinit var pepa: UserAccount
  lateinit var frantasProject: Project
  lateinit var pepasProject: Project

  val root =
    TestDataBuilder {
      addUserAccount {
        username = "franta"
        name = "Franta"
        franta = this
      }.build {
        addProject {
          name = "Project"
          organizationOwner = this@build.defaultOrganizationBuilder.self
          frantasProject = this
        }
      }
      addUserAccount {
        username = "pepa"
        name = "Pepa"
        totpKey = TOTP_KEY
        pepa = this
      }.build {
        addProject {
          name = "Project"
          organizationOwner = this@build.defaultOrganizationBuilder.self
          pepasProject = this
        }
      }
    }
}

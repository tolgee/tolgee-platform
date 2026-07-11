package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType

class SelfHostedLimitsTestData {
  lateinit var user: UserAccount
  lateinit var project: Project
  lateinit var englishLanguage: Language
  lateinit var germanLanguage: Language

  val root =
    TestDataBuilder().apply {
      val userAccountBuilder =
        addUserAccount {
          username = "franta"
          name = "Franta User"
          role = UserAccount.Role.USER
          user = this
        }

      addProject {
        name = "Self-hosted Limits Test Project"
        organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self
        project = this
      }.build project@{
        addPermission {
          user = this@SelfHostedLimitsTestData.user
          type = ProjectPermissionType.MANAGE
        }

        englishLanguage =
          addLanguage {
            name = "English"
            tag = "en"
            originalName = "English"
            this@project.self.baseLanguage = this
          }.self

        germanLanguage =
          addLanguage {
            name = "German"
            tag = "de"
            originalName = "Deutsch"
          }.self

        // Add a test key with English and German translations
        addKey {
          name = "test"
        }.build {
          addTranslation {
            language = englishLanguage
            text = "test"
          }

          addTranslation {
            language = germanLanguage
            text = "test"
          }
        }
      }
    }
}

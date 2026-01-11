package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.OrganizationBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.translation.Translation

class FormerUserTestData {
  lateinit var activeUser: UserAccount
  lateinit var formerUser: UserAccount
  lateinit var organizationBuilder: OrganizationBuilder
  lateinit var translation: Translation
  lateinit var project: Project

  val root =
    TestDataBuilder().apply {
      addUserAccount {
        username = "admin@admin.com"
        name = "Peter Administrator"
        role = UserAccount.Role.ADMIN
        activeUser = this
      }.build {
        organizationBuilder = defaultOrganizationBuilder
      }

      addUserAccountWithoutOrganization {
        username = "will@be.removed"
        name = "Will Be Removed"
        role = UserAccount.Role.USER
        formerUser = this
      }

      organizationBuilder.build {
        addRole {
          user = formerUser
          type = OrganizationRoleType.OWNER
        }
      }

      addProject {
        organizationOwner = organizationBuilder.self
        name = "project"
        project = this
      }.build {
        val key =
          addKey {
            name = "key"
          }
        val en = addEnglish()
        addTranslation {
          text = "helloo"
          language = en.self
          this.key = key.self
          translation = this
        }.build {
          addComment {
            text = "Hellooo!"
            author = formerUser
          }
        }
      }
    }
}

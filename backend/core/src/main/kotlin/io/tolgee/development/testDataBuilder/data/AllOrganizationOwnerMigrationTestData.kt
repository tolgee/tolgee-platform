package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.enums.ProjectPermissionType

class AllOrganizationOwnerMigrationTestData {
  val root =
    TestDataBuilder().apply {
      addUserAccount {
        name = "User with 2 projects"
        username = "to.si@proladite.krkovicku"
      }.build userAccount@{
        addProject {
          name = "A project"
          userOwner = this@userAccount.self
        }.build project@{
          addPermission {
            user = this@userAccount.self
            project = this@project.self
            type = ProjectPermissionType.MANAGE
          }
        }
        addProject {
          name = "A project 2"
          userOwner = this@userAccount.self
        }.build project@{
          addPermission {
            user = this@userAccount.self
            project = this@project.self
            type = ProjectPermissionType.MANAGE
          }
        }
      }
    }
}

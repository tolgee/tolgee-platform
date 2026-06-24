package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType

class ProjectPublishingTestData : BaseTestData() {
  val owner: UserAccount get() = user
  lateinit var manager: UserAccount
  lateinit var serverAdmin: UserAccount

  init {
    root.apply {
      addUserAccount {
        username = "project_manager"
        name = "Project Manager"
        manager = this
      }
      addUserAccount {
        username = "server_admin"
        name = "Server Admin"
        role = UserAccount.Role.ADMIN
        serverAdmin = this
      }
    }
    projectBuilder.build {
      addPermission {
        user = this@ProjectPublishingTestData.manager
        type = ProjectPermissionType.MANAGE
      }
    }
  }
}

package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope

class ProjectPublishingTestData : BaseTestData() {
  val owner: UserAccount get() = user
  lateinit var manager: UserAccount
  lateinit var serverAdmin: UserAccount
  lateinit var viewer: UserAccount
  lateinit var granularUser: UserAccount
  lateinit var communityUser: UserAccount

  init {
    root.apply {
      addUserAccount {
        username = "project_manager"
        name = "Project Manager"
        manager = this
      }
      addUserAccount {
        username = "project_viewer"
        name = "Project Viewer"
        viewer = this
      }
      addUserAccount {
        username = "server_admin"
        name = "Server Admin"
        role = UserAccount.Role.ADMIN
        serverAdmin = this
      }
      addUserAccount {
        username = "granular_user"
        name = "Granular User"
        granularUser = this
      }
      addUserAccount {
        username = "community_user"
        name = "Community User"
        communityUser = this
      }
    }
    projectBuilder.build {
      addPermission {
        user = this@ProjectPublishingTestData.manager
        type = ProjectPermissionType.MANAGE
      }
      addPermission {
        user = this@ProjectPublishingTestData.viewer
        type = ProjectPermissionType.VIEW
      }
      addPermission {
        user = this@ProjectPublishingTestData.granularUser
        scopes = arrayOf(Scope.TRANSLATIONS_VIEW)
      }
      addKey {
        name = "trash-me"
      }.build {
        addTranslation("en", "To be trashed")
      }
    }
  }
}

package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Project
import io.tolgee.model.enums.ProjectPermissionType

/**
 * The MCP resource's project plus [otherProject], a second project the same user manages, so a token bound to one of
 * them can be shown not to reach the other.
 */
class McpOAuthTestData : BaseTestData(userName = "mcp_oauth_user", projectName = "mcp_oauth_project") {
  lateinit var otherProject: Project

  init {
    otherProject =
      root
        .addProject {
          name = "mcp_oauth_other_project"
          organizationOwner = projectBuilder.self.organizationOwner
        }.build {
          addPermission {
            user = this@McpOAuthTestData.user
            type = ProjectPermissionType.MANAGE
          }
          addLanguage {
            name = "English"
            tag = "en"
          }
        }.self

    projectBuilder.addBranch {
      name = "main"
      isDefault = true
    }
  }
}

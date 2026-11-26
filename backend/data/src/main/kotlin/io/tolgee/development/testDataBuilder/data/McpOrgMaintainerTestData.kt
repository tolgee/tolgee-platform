package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Organization
import io.tolgee.model.Pat
import io.tolgee.model.enums.OrganizationRoleType

class McpOrgMaintainerTestData : BaseTestData(userName = "mcp_maintainer_user") {
  lateinit var pat: Pat
  lateinit var otherOrganization: Organization

  init {
    userAccountBuilder.build {
      addPat {
        description = "MCP test PAT"
        pat = this
      }
    }
    root
      .addUserAccount {
        username = "other_org_owner"
      }.build {
        defaultOrganizationBuilder.build {
          self.name = "Other Org"
          otherOrganization = self
          addRole {
            user = this@McpOrgMaintainerTestData.user
            type = OrganizationRoleType.MAINTAINER
          }
        }
      }
  }
}

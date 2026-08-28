package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType

class OwnerlessOrganizationTestData {
  val root: TestDataBuilder = TestDataBuilder()

  val member: UserAccount
  val withMember: Organization
  val withPublicProject: Organization
  val publicProject: Project

  init {
    val memberBuilder =
      root.addUserAccount {
        username = "ownerless_org_member@test.com"
      }
    member = memberBuilder.self

    val withMemberBuilder =
      root.addOrganization {
        name = "Ownerless With Member"
      }
    withMemberBuilder.addRole {
      user = member
      type = OrganizationRoleType.MEMBER
    }
    withMember = withMemberBuilder.self

    val withPublicProjectBuilder =
      root.addOrganization {
        name = "Ownerless With Public Project"
      }
    withPublicProject = withPublicProjectBuilder.self

    publicProject =
      root
        .addProject(organizationOwner = withPublicProject) {
          name = "Ownerless public project"
          public = true
        }.self
  }
}

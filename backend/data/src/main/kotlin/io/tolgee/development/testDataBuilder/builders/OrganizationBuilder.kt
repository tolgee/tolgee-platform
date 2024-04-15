package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.FT
import io.tolgee.model.MtCreditBucket
import io.tolgee.model.Organization
import io.tolgee.model.OrganizationRole
import io.tolgee.model.Permission
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType.VIEW
import io.tolgee.model.slackIntegration.OrganizationSlackWorkspace
import org.springframework.core.io.ClassPathResource

class OrganizationBuilder(
  val testDataBuilder: TestDataBuilder,
) : BaseEntityDataBuilder<Organization, OrganizationBuilder>() {
  class DATA {
    var roles: MutableList<OrganizationRoleBuilder> = mutableListOf()
    var avatarFile: ClassPathResource? = null
    var slackWorkspaces: MutableList<OrganizationSlackWorkspaceBuilder> = mutableListOf()
  }

  var defaultOrganizationOfUser: UserAccount? = null

  val data = DATA()

  fun addRole(ft: FT<OrganizationRole>) = addOperation(data.roles, ft)

  fun addMtCreditBucket(ft: FT<MtCreditBucket>): MtCreditBucketBuilder {
    val builder = MtCreditBucketBuilder()
    testDataBuilder.data.mtCreditBuckets.add(builder)
    builder.self.organization = this@OrganizationBuilder.self
    ft(builder.self)
    return builder
  }

  fun addSlackWorkspace(ft: FT<OrganizationSlackWorkspace>) = addOperation(data.slackWorkspaces, ft)

  override var self: Organization =
    Organization().also {
      it.basePermission =
        Permission(
          organization = it,
          type = VIEW,
        )
    }

  fun setAvatar(filePath: String) {
    data.avatarFile = ClassPathResource(filePath, this.javaClass.classLoader)
  }
}

package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.FT
import io.tolgee.model.*
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
    var tenant: SsoTenantBuilder? = null
    var llmProviders: MutableList<LLMProviderBuilder> = mutableListOf()
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

  fun setTenant(ft: FT<SsoTenant>): SsoTenantBuilder {
    val builder = SsoTenantBuilder(this)
    ft(builder.self)
    data.tenant = builder
    return builder
  }

  fun addLLMProvider(ft: FT<LLMProvider>): LLMProviderBuilder {
    val builder = LLMProviderBuilder(this)
    data.llmProviders.add(builder)
    ft(builder.self)
    return builder
  }
}

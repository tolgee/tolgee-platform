package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType

class TestDataBuilder(
  fn: (TestDataBuilder.() -> Unit) = {},
) {
  companion object {
    operator fun invoke(fn: (TestDataBuilder.() -> Unit) = {}) = TestDataBuilder(fn)
  }

  class DATA {
    val userAccounts = mutableListOf<UserAccountBuilder>()
    val projects = mutableListOf<ProjectBuilder>()
    val organizations = mutableListOf<OrganizationBuilder>()
    val mtCreditBuckets = mutableListOf<MtCreditBucketBuilder>()
    val invitations = mutableListOf<InvitationBuilder>()

    /**
     * These data are populated by external modules and saved via one of the
     * AdditionalTestDataSavers
     */
    var additionalData = mutableMapOf<String, Any>()
  }

  val data = DATA()

  fun addUserAccountWithoutOrganization(ft: UserAccount.() -> Unit): UserAccountBuilder {
    val builder = UserAccountBuilder(this)
    data.userAccounts.add(builder)
    ft(builder.self)
    return builder
  }

  fun addUserAccount(ft: UserAccount.() -> Unit): UserAccountBuilder {
    val builder = UserAccountBuilder(this)
    data.userAccounts.add(builder)
    ft(builder.self)
    val organizationBuilder =
      addOrganization {
        name = if (builder.self.name.isNotBlank()) builder.self.name else builder.self.username
      }.build {
        addRole {
          user = builder.self
          type = OrganizationRoleType.OWNER
        }
      }
    builder.defaultOrganizationBuilder = organizationBuilder
    return builder
  }

  fun addProject(
    organizationOwner: Organization? = null,
    ft: Project.() -> Unit,
  ): ProjectBuilder {
    val projectBuilder = ProjectBuilder(organizationOwner, testDataBuilder = this)
    data.projects.add(projectBuilder)
    ft(projectBuilder.self)
    return projectBuilder
  }

  fun addOrganization(ft: Organization.() -> Unit): OrganizationBuilder {
    val builder = OrganizationBuilder(testDataBuilder = this)
    data.organizations.add(builder)
    ft(builder.self)
    return builder
  }

  fun addAdmin(): UserAccountBuilder {
    return addUserAccount {
      username = "admin@admin.com"
      role = UserAccount.Role.ADMIN
    }
  }

  init {
    fn(this)
  }
}

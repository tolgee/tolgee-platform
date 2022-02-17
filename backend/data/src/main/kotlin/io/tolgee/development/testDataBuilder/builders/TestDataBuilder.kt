package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.UserAccount

class TestDataBuilder {
  class DATA {
    val userAccounts = mutableListOf<UserAccountBuilder>()
    val projects = mutableListOf<ProjectBuilder>()
    val organizations = mutableListOf<OrganizationBuilder>()
    val mtCreditBuckets = mutableListOf<MtCreditBucketBuilder>()
  }

  val data = DATA()

  fun addUserAccount(ft: UserAccount.() -> Unit): UserAccountBuilder {
    val builder = UserAccountBuilder(this)
    data.userAccounts.add(builder)
    ft(builder.self)
    return builder
  }

  fun addProject(
    userOwner: UserAccount? = null,
    organizationOwner: Organization? = null,
    ft: Project.() -> Unit
  ): ProjectBuilder {
    val projectBuilder = ProjectBuilder(userOwner, organizationOwner, testDataBuilder = this)
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
}

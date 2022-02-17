package io.tolgee.development.testDataBuilder

import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.UserAccount

class TestDataBuilder {
  class DATA {
    val userAccounts = mutableListOf<DataBuilders.UserAccountBuilder>()
    val projects = mutableListOf<DataBuilders.ProjectBuilder>()
    val organizations = mutableListOf<DataBuilders.OrganizationBuilder>()
    val mtCreditBuckets = mutableListOf<DataBuilders.MtCreditBucketBuilder>()
  }

  val data = DATA()

  fun addUserAccount(ft: UserAccount.() -> Unit): DataBuilders.UserAccountBuilder {
    val builder = DataBuilders.UserAccountBuilder(this)
    data.userAccounts.add(builder)
    ft(builder.self)
    return builder
  }

  fun addProject(
    userOwner: UserAccount? = null,
    organizationOwner: Organization? = null,
    ft: Project.() -> Unit
  ): DataBuilders.ProjectBuilder {
    val projectBuilder = DataBuilders.ProjectBuilder(userOwner, organizationOwner, testDataBuilder = this)
    data.projects.add(projectBuilder)
    ft(projectBuilder.self)
    return projectBuilder
  }

  fun addOrganization(ft: Organization.() -> Unit): DataBuilders.OrganizationBuilder {
    val builder = DataBuilders.OrganizationBuilder(testDataBuilder = this)
    data.organizations.add(builder)
    ft(builder.self)
    return builder
  }
}

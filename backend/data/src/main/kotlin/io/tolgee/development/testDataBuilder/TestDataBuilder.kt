package io.tolgee.development.testDataBuilder

import io.tolgee.model.Organization
import io.tolgee.model.UserAccount

class TestDataBuilder {
  class DATA {
    val userAccounts = mutableListOf<DataBuilders.UserAccountBuilder>()
    val projects = mutableListOf<DataBuilders.ProjectBuilder>()
    val organizations = mutableListOf<DataBuilders.OrganizationBuilder>()
    val mtCreditBuckets = mutableListOf<DataBuilders.MtCreditBucketBuilder>()
  }

  val data = DATA()

  fun addUserAccount(ft: DataBuilders.UserAccountBuilder.() -> Unit): DataBuilders.UserAccountBuilder {
    val builder = DataBuilders.UserAccountBuilder(this)
    data.userAccounts.add(builder)
    ft(builder)
    return builder
  }

  fun addProject(
    userOwner: UserAccount? = null,
    organizationOwner: Organization? = null,
    ft: DataBuilders.ProjectBuilder.() -> Unit
  ): DataBuilders.ProjectBuilder {
    val project = DataBuilders.ProjectBuilder(userOwner, organizationOwner, testDataBuilder = this)
    data.projects.add(project)
    ft(project)
    return project
  }

  fun addOrganization(ft: DataBuilders.OrganizationBuilder.() -> Unit): DataBuilders.OrganizationBuilder {
    val entity = DataBuilders.OrganizationBuilder(testDataBuilder = this)
    data.organizations.add(entity)
    ft(entity)
    return entity
  }
}

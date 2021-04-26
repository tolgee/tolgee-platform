package io.tolgee.development.testDataBuilder

import io.tolgee.model.Organization
import io.tolgee.model.UserAccount

class TestDataBuilder {
    class DATA {
        val userAccounts = mutableListOf<DataBuilders.UserAccountBuilder>()
        val repositories = mutableListOf<DataBuilders.RepositoryBuilder>()
        val organizations = mutableListOf<DataBuilders.OrganizationBuilder>()
    }

    val data = DATA()

    fun addUserAccount(ft: DataBuilders.UserAccountBuilder.() -> Unit): DataBuilders.UserAccountBuilder {
        val builder = DataBuilders.UserAccountBuilder(this)
        data.userAccounts.add(builder)
        ft(builder)
        return builder
    }

    fun addRepository(userOwner: UserAccount? = null,
                      organizationOwner: Organization? = null,
                      ft: DataBuilders.RepositoryBuilder.() -> Unit): DataBuilders.RepositoryBuilder {
        val repository = DataBuilders.RepositoryBuilder(userOwner, organizationOwner, testDataBuilder = this)
        data.repositories.add(repository)
        ft(repository)
        return repository
    }

    fun addOrganization(ft: DataBuilders.OrganizationBuilder.() -> Unit): DataBuilders.OrganizationBuilder {
        val entity = DataBuilders.OrganizationBuilder(testDataBuilder = this)
        data.organizations.add(entity)
        ft(entity)
        return entity
    }
}

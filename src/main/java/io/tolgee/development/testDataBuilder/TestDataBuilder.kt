package io.tolgee.development.testDataBuilder

import io.tolgee.model.Organization
import io.tolgee.model.UserAccount

class TestDataBuilder {
    class DATA {
        val userAccounts = mutableListOf<UserAccount>()
        val repositories = mutableListOf<DataBuilders.RepositoryBuilder>()
        val organizations = mutableListOf<DataBuilders.OrganizationBuilder>()
    }

    val data = DATA()

    fun addUserAccount(ft: UserAccount.() -> Unit): UserAccount {
        val account = UserAccount()
        data.userAccounts.add(account)
        ft(account)
        return account
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

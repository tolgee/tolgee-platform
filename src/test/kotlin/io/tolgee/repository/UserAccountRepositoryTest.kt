package io.tolgee.repository

import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.development.DbPopulatorReal
import io.tolgee.model.OrganizationRole
import io.tolgee.model.UserAccount
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests
import org.testng.annotations.Test

@SpringBootTest
class UserAccountRepositoryTest : AbstractTransactionalTestNGSpringContextTests() {

    @Autowired
    lateinit var userAccountRepository: UserAccountRepository

    @Autowired
    lateinit var dbPopulatorReal: DbPopulatorReal


    @Test
    fun getAllInOrganizationHasMemberRole() {
        val usersAndOrganizations = dbPopulatorReal.createUsersAndOrganizations()
        val org = usersAndOrganizations[1].organizationRoles[0].organization
        val returned = userAccountRepository.getAllInOrganization(org!!.id!!, PageRequest.of(0, 20), "")
        assertThat(returned.content).hasSize(2)
        assertThat(returned.content[0][0]).isInstanceOf(UserAccount::class.java)
        assertThat(returned.content[1][1]).isInstanceOf(OrganizationRole::class.java)
    }

    @Test
    fun getAllInOrganizationCorrectAccounts() {
        val usersAndOrganizations = dbPopulatorReal.createUsersAndOrganizations()
        val org = usersAndOrganizations[0].organizationRoles[5].organization
        val returned = userAccountRepository.getAllInOrganization(org!!.id!!, PageRequest.of(0, 20), "")
        assertThat(returned.content).hasSize(4)
    }
}

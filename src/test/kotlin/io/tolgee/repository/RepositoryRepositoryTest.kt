package io.tolgee.repository

import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.development.DbPopulatorReal
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.model.Organization
import io.tolgee.model.OrganizationRole
import io.tolgee.model.Permission
import io.tolgee.model.Repository
import org.assertj.core.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests
import org.testng.annotations.Test

@SpringBootTest
class RepositoryRepositoryTest : AbstractTransactionalTestNGSpringContextTests() {

    @Autowired
    lateinit var repositoryRepository: RepositoryRepository

    @Autowired
    lateinit var dbPopulatorReal: DbPopulatorReal

    @Test
    fun testRepositoryPreSaveHookBothSet() {
        val user = dbPopulatorReal.createUser("hello")
        val repo = Repository(name = "Test", addressPart = "hello", userOwner = user)
        val organization = Organization(
                name = "Test org",
                addressPart = "null",
                basePermissions = Permission.RepositoryPermissionType.VIEW)
        repo.organizationOwner = organization
        Assertions.assertThatExceptionOfType(Exception::class.java).isThrownBy { repositoryRepository.save(repo) }
                .withMessage("java.lang.Exception: Exactly one of organizationOwner or userOwner must be set!")
    }

    @Test
    fun testRepositoryPreSaveHookNorSet() {
        val repo = Repository(name = "Test", addressPart = "hello", userOwner = null)
        Assertions.assertThatExceptionOfType(Exception::class.java).isThrownBy { repositoryRepository.save(repo) }
                .withMessage("java.lang.Exception: Exactly one of organizationOwner or userOwner must be set!")
    }


    @Test
    fun testPermittedRepositories() {
        val users = dbPopulatorReal.createUsersAndOrganizations()
        dbPopulatorReal.createBase("No org repo", users[3].username!!)
        val result = repositoryRepository.findAllPermitted(users[3].id!!)
        assertThat(result).hasSize(10)
        assertThat(result[9][2]).isNull()
        assertThat(result[9][0]).isInstanceOf(Repository::class.java)
        assertThat(result[9][1]).isInstanceOf(Permission::class.java)
        assertThat(result[8][1]).isNull()
        assertThat(result[8][0]).isInstanceOf(Repository::class.java)
        assertThat(result[8][3]).isInstanceOf(OrganizationRole::class.java)
    }

    @Test
    fun testPermittedRepositoriesJustNoOrg() {
        val base = dbPopulatorReal.createBase("No org repo", generateUniqueString())
        val result = repositoryRepository.findAllPermitted(base.userOwner!!.id!!)
        assertThat(result).hasSize(1)
    }


    @Test
    fun testPermittedJustOrg() {
        val users = dbPopulatorReal.createUsersAndOrganizations()
        dbPopulatorReal.createBase("No org repo", users[1].username!!)
        val result = repositoryRepository.findAllPermitted(users[3].id!!)
        assertThat(result).hasSize(9)
    }
}

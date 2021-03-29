package io.tolgee.repository

import io.tolgee.development.DbPopulatorReal
import io.tolgee.model.Organization
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
    lateinit var dbPopulatorReal: DbPopulatorReal;


    @Test
    fun testRepositoryPreSaveHookBothSet() {
        val user = dbPopulatorReal.createUser("hello")
        val repo = Repository(name = "Test", addressPart = "hello", userOwner = user)
        val organization = Organization(name = "Test org", addressPart = "null", basePermissions = Permission.RepositoryPermissionType.VIEW)
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
}

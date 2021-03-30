package io.tolgee.service

import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.development.DbPopulatorReal
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.UserAccount
import io.tolgee.repository.RepositoryRepository
import io.tolgee.repository.UserAccountRepository
import org.assertj.core.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests
import org.springframework.transaction.annotation.Transactional
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import javax.persistence.EntityManager

@SpringBootTest
open class DbPopulatorTest : AbstractTransactionalTestNGSpringContextTests() {
    @Autowired
    lateinit var populator: DbPopulatorReal

    @Autowired
    lateinit var userAccountRepository: UserAccountRepository

    @Autowired
    lateinit var repositoryRepository: RepositoryRepository

    @Autowired
    lateinit var apiKeyService: ApiKeyService

    @Autowired
    lateinit var entityManager: EntityManager

    @Autowired
    lateinit var tolgeeProperties: TolgeeProperties

    lateinit var userAccount: UserAccount

    @BeforeMethod
    open fun setup() {
        populator.autoPopulate()
        userAccount = userAccountRepository.findByUsername(tolgeeProperties.authentication.initialUsername).orElseThrow({ NotFoundException() })
    }

    @Test
    @Transactional
    open fun createsUser() {
        Assertions.assertThat(userAccount.name).isEqualTo(tolgeeProperties.authentication.initialUsername)
    }

    @Test
    @Transactional
    open fun createsRepository() {
        entityManager.refresh(userAccount)
        val found = repositoryRepository.findAll().asSequence()
                .flatMap { it!!.permissions.map { it.user } }
                .find { it == userAccount }
        assertThat(found).isNotNull
    }

    @Test
    @Transactional
    open fun createsApiKey() {
        val key = apiKeyService.getAllByUser(userAccount).stream().findFirst()
        Assertions.assertThat(key).isPresent
        val (_, key1) = key.get()
        Assertions.assertThat(key1).isEqualTo("this_is_dummy_api_key")
    }
}

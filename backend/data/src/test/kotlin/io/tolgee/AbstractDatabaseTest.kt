package io.tolgee

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.model.UserAccount
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.security.UserAccountService
import io.tolgee.testing.assertions.Assertions
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.concurrent.atomic.AtomicLong

@SpringBootTest
@Import(TestDataFactory::class)
@ActiveProfiles("test")
@Transactional
abstract class AbstractDatabaseTest {
    @Autowired
    protected lateinit var entityManager: EntityManager

    @Autowired
    protected lateinit var tolgeeProperties: TolgeeProperties

    @Autowired
    protected lateinit var testDataFactory: TestDataFactory

    @Autowired
    protected lateinit var userAccountService: UserAccountService

    @MockBean
    protected lateinit var authenticationFacade: AuthenticationFacade

    companion object {
        private val userIdCounter = AtomicLong(1)
        private val organizationIdCounter = AtomicLong(1)
        private val projectIdCounter = AtomicLong(1)
    }

    @BeforeEach
    fun setup() {
        Assertions.setEntityManager(entityManager)
        mockAuthenticationFacade()
    }

    protected fun mockAuthenticationFacade() {
        val userAccount = UserAccount().apply {
            id = 1
            username = "admin"
            name = "Administrator"
            role = UserAccount.Role.ADMIN
        }
        val userAccountDto = UserAccountDto.fromEntity(userAccount)
        Mockito.`when`(authenticationFacade.authenticatedUser).thenReturn(userAccountDto)
        Mockito.`when`(authenticationFacade.authenticatedOrNull).thenReturn(userAccountDto)
        Mockito.`when`(authenticationFacade.isAuthenticated).thenReturn(true)
    }

    protected fun getUniqueString(): String {
        return UUID.randomUUID().toString()
    }

    protected fun getUniqueUserId(): Long {
        return userIdCounter.getAndIncrement()
    }

    protected fun getUniqueOrganizationId(): Long {
        return organizationIdCounter.getAndIncrement()
    }

    protected fun getUniqueProjectId(): Long {
        return projectIdCounter.getAndIncrement()
    }
} 
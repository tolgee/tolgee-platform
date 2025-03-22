package io.tolgee.testing

import io.tolgee.AbstractDatabaseTest
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.model.UserAccount
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.TransactionUtil
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

@SpringBootTest
@ActiveProfiles("test")
@Transactional
abstract class AbstractTransactionalTest {
    @Autowired
    protected lateinit var entityManager: EntityManager

    @Autowired
    protected lateinit var tolgeeProperties: TolgeeProperties

    @Autowired
    protected lateinit var userAccountService: UserAccountService

    @Autowired
    protected lateinit var transactionUtil: TransactionUtil

    @MockBean
    protected lateinit var authenticationFacade: AuthenticationFacade

    companion object {
        private val userIdCounter = AtomicLong(1)
        private val organizationIdCounter = AtomicLong(1)
        private val projectIdCounter = AtomicLong(1)
    }

    @BeforeEach
    fun setup() {
        mockAuthenticationFacade()
    }

    protected fun mockAuthenticationFacade() {
        val userAccount = UserAccount().apply {
            id = 1
            username = "admin"
            name = "Administrator"
            role = UserAccount.Role.ADMIN
        }
        Mockito.`when`(authenticationFacade.authenticatedUser).thenReturn(userAccount)
        Mockito.`when`(authenticationFacade.authenticatedOrNull).thenReturn(userAccount)
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
    
    protected fun <T> executeInTransaction(supplier: () -> T): T {
        return transactionUtil.executeInTransaction { supplier.get() }
    }
} 
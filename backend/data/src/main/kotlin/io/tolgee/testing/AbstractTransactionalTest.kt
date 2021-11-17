package io.tolgee.testing

import io.tolgee.repository.LanguageRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Transactional
abstract class AbstractTransactionalTest : AbstractTransactionalTestNGSpringContextTests() {
    @Autowired
    lateinit protected var entityManager: EntityManager

    @Autowired
    lateinit protected var languageRepository: LanguageRepository

    protected fun commitTransaction() {
        TestTransaction.flagForCommit()
        entityManager.flush()
        TestTransaction.end()
        TestTransaction.start()
        entityManager.clear()
    }
}

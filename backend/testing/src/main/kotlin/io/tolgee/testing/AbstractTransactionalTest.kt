package io.tolgee.testing

import io.tolgee.CleanDbTestListener
import io.tolgee.repository.LanguageRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.test.context.transaction.TransactionalTestExecutionListener
import javax.persistence.EntityManager

@TestExecutionListeners(
  value = [
    TransactionalTestExecutionListener::class,
    DependencyInjectionTestExecutionListener::class,
    CleanDbTestListener::class
  ]
)
abstract class AbstractTransactionalTest {
  @Autowired
  protected lateinit var entityManager: EntityManager

  @Autowired
  protected lateinit var languageRepository: LanguageRepository

  protected fun commitTransaction() {
    TestTransaction.flagForCommit()
    entityManager.flush()
    TestTransaction.end()
    TestTransaction.start()
    entityManager.clear()
  }
}

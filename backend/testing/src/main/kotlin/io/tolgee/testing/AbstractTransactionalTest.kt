package io.tolgee.testing

import io.tolgee.BatchJobCleanerListener
import io.tolgee.CleanDbTestListener
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import org.springframework.test.context.support.DirtiesContextTestExecutionListener
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.test.context.transaction.TransactionalTestExecutionListener

@TestExecutionListeners(
  value = [
    TransactionalTestExecutionListener::class,
    DependencyInjectionTestExecutionListener::class,
    CleanDbTestListener::class,
    DirtiesContextTestExecutionListener::class,
    BatchJobCleanerListener::class,
  ],
)
@ActiveProfiles(profiles = ["local"])
abstract class AbstractTransactionalTest {
  @Autowired
  protected open lateinit var entityManager: EntityManager

  protected fun commitTransaction() {
    TestTransaction.flagForCommit()
    entityManager.flush()
    TestTransaction.end()
    TestTransaction.start()
    entityManager.clear()
  }
}

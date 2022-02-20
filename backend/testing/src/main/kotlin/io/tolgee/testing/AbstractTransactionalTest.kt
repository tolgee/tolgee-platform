package io.tolgee.testing

import io.tolgee.repository.LanguageRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Transactional
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

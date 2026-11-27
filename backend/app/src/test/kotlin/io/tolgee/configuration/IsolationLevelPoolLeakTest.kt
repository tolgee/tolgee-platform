package io.tolgee.configuration

import io.tolgee.AbstractSpringTest
import io.tolgee.util.executeInNewTransaction
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.hibernate.Session
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

/** See [TransactionScopedIsolationJpaDialect]. */
@SpringBootTest
@Import(IsolationLevelPoolLeakTest.ProbeConfiguration::class)
class IsolationLevelPoolLeakTest : AbstractSpringTest() {
  @Autowired
  private lateinit var probe: IsolationProbe

  @Test
  fun `requested isolation applies to the transaction, never to the session`() {
    assertThat((platformTransactionManager as JpaTransactionManager).jpaDialect)
      .isInstanceOf(TransactionScopedIsolationJpaDialect::class.java)

    repeat(POOL_CYCLES) {
      executeInNewTransaction(
        platformTransactionManager,
        isolationLevel = TransactionDefinition.ISOLATION_SERIALIZABLE,
      ) {
        assertThat(probe.transactionIsolation()).isEqualTo("serializable")
        assertThat(probe.sessionDefaultIsolation()).isEqualTo("read committed")
      }
      assertThatThrownBy {
        executeInNewTransaction(
          platformTransactionManager,
          isolationLevel = TransactionDefinition.ISOLATION_REPEATABLE_READ,
        ) {
          assertThat(probe.transactionIsolation()).isEqualTo("repeatable read")
          throw IllegalStateException("force rollback")
        }
      }.isInstanceOf(IllegalStateException::class.java).hasMessage("force rollback")
      probe.assertReadOnlyRepeatableReadIsTransactionScoped()
      repeat(5) {
        executeInNewTransaction(platformTransactionManager) {
          assertThat(probe.transactionIsolation()).isEqualTo("read committed")
          assertThat(probe.sessionDefaultIsolation()).isEqualTo("read committed")
        }
      }
    }
  }

  @TestConfiguration
  class ProbeConfiguration {
    @Bean
    fun isolationProbe(entityManager: EntityManager) = IsolationProbe(entityManager)
  }

  open class IsolationProbe(
    private val entityManager: EntityManager,
  ) {
    open fun transactionIsolation(): String = show("transaction_isolation")

    open fun sessionDefaultIsolation(): String = show("default_transaction_isolation")

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    open fun assertReadOnlyRepeatableReadIsTransactionScoped() {
      assertThat(transactionIsolation()).isEqualTo("repeatable read")
      assertThat(sessionDefaultIsolation()).isEqualTo("read committed")
      assertThat(show("transaction_read_only")).isEqualTo("on")
      assertThat(entityManager.unwrap(Session::class.java).isDefaultReadOnly).isTrue
    }

    private fun show(setting: String): String = entityManager.createNativeQuery("show $setting").singleResult as String
  }

  companion object {
    private const val POOL_CYCLES = 30
  }
}

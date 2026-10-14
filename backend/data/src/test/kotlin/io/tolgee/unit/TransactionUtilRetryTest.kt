package io.tolgee.unit

import io.tolgee.util.RepeatedlyCannotSerializeTransactionException
import io.tolgee.util.executeInNewRepeatableTransaction
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.hibernate.exception.LockAcquisitionException
import org.junit.jupiter.api.Test
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus
import java.sql.SQLException

class TransactionUtilRetryTest {
  private val transactionManager =
    object : AbstractPlatformTransactionManager() {
      override fun doGetTransaction(): Any = Any()

      override fun doBegin(
        transaction: Any,
        definition: TransactionDefinition,
      ) = Unit

      override fun doCommit(status: DefaultTransactionStatus) = Unit

      override fun doRollback(status: DefaultTransactionStatus) = Unit
    }

  @Test
  fun `retries a serialization failure that no translation layer converted`() {
    var attempts = 0
    val result =
      executeInNewRepeatableTransaction(transactionManager) {
        attempts++
        if (attempts == 1) {
          throw LockAcquisitionException("flush failed", SQLException("could not serialize access", "40001"))
        }
        "done"
      }
    assertThat(result).isEqualTo("done")
    assertThat(attempts).isEqualTo(2)
  }

  @Test
  fun `does not retry unrelated exceptions`() {
    var attempts = 0
    assertThatThrownBy {
      executeInNewRepeatableTransaction(transactionManager) {
        attempts++
        throw IllegalStateException("not retryable")
      }
    }.isInstanceOf(IllegalStateException::class.java)
    assertThat(attempts).isEqualTo(1)
  }

  @Test
  fun `gives up after the attempt limit`() {
    assertThatThrownBy {
      executeInNewRepeatableTransaction(transactionManager) {
        throw LockAcquisitionException("always", SQLException("deadlock detected", "40P01"))
      }
    }.isInstanceOf(RepeatedlyCannotSerializeTransactionException::class.java)
  }
}

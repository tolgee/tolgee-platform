package io.tolgee.component

import jakarta.persistence.EntityManager
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.resource.jdbc.internal.AbstractLogicalConnectionImplementor
import org.hibernate.resource.transaction.spi.TransactionStatus
import org.springframework.stereotype.Component
import java.sql.Savepoint
import java.util.UUID

@Component
class SavePointManager(
  private val entityManager: EntityManager,
) {
  fun setSavepoint(): Savepoint? {
    var savepoint: Savepoint? = null
    getSession().doWork { connection -> savepoint = connection.setSavepoint(UUID.randomUUID().toString()) }
    return savepoint
  }

  fun rollbackSavepoint(savepoint: Savepoint?) {
    val session = getSession()
    session.doWork { connection ->
      connection.rollback(savepoint)
    }

    val logicalConnection =
      session.jdbcCoordinator.logicalConnection as? AbstractLogicalConnectionImplementor
        ?: throw IllegalStateException("Logical connection is not AbstractLogicalConnectionImplementor")
    // Hibernate exposes no public API to clear a transaction's rollback-only mark; without this
    // reset the commit after the savepoint rollback fails.
    val statusField = AbstractLogicalConnectionImplementor::class.java.getDeclaredField("status")
    statusField.isAccessible = true
    statusField.set(logicalConnection, TransactionStatus.ACTIVE)
  }

  fun getSession(): SharedSessionContractImplementor {
    return entityManager.unwrap(SharedSessionContractImplementor::class.java)
      ?: throw IllegalStateException("Session is null")
  }
}

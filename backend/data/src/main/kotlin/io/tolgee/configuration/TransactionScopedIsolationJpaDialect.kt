package io.tolgee.configuration

import jakarta.persistence.EntityManager
import org.hibernate.Session
import org.springframework.orm.jpa.vendor.HibernateJpaDialect
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.DelegatingTransactionDefinition
import org.springframework.transaction.support.ResourceTransactionDefinition

/**
 * Sets the requested isolation level with `SET TRANSACTION`, scoped to the transaction. Spring's
 * default goes through `Connection.setTransactionIsolation`, which pgjdbc sends as a session-level
 * `SET SESSION CHARACTERISTICS`; behind PgBouncer in transaction pooling mode that setting stays on
 * the server connection and is handed to the next client.
 */
class TransactionScopedIsolationJpaDialect : HibernateJpaDialect() {
  override fun beginTransaction(
    entityManager: EntityManager,
    definition: TransactionDefinition,
  ): Any {
    val isolationLevel = definition.isolationLevel
    val transactionData = super.beginTransaction(entityManager, WithoutIsolation(definition))
    if (isolationLevel != TransactionDefinition.ISOLATION_DEFAULT) {
      entityManager.unwrap(Session::class.java).doWork { connection ->
        connection.createStatement().use { it.execute("SET TRANSACTION ISOLATION LEVEL ${sql(isolationLevel)}") }
      }
    }
    return transactionData
  }

  private fun sql(isolationLevel: Int): String =
    when (isolationLevel) {
      TransactionDefinition.ISOLATION_READ_UNCOMMITTED -> "READ UNCOMMITTED"
      TransactionDefinition.ISOLATION_READ_COMMITTED -> "READ COMMITTED"
      TransactionDefinition.ISOLATION_REPEATABLE_READ -> "REPEATABLE READ"
      TransactionDefinition.ISOLATION_SERIALIZABLE -> "SERIALIZABLE"
      else -> throw IllegalArgumentException("Unknown isolation level $isolationLevel")
    }

  /**
   * Keeps [ResourceTransactionDefinition]: [HibernateJpaDialect.beginTransaction] only applies the
   * read-only session and flush-mode bookkeeping when the definition implements it.
   */
  private class WithoutIsolation(
    private val original: TransactionDefinition,
  ) : DelegatingTransactionDefinition(original),
    ResourceTransactionDefinition {
    override fun getIsolationLevel(): Int = TransactionDefinition.ISOLATION_DEFAULT

    override fun isLocalResource(): Boolean = (original as? ResourceTransactionDefinition)?.isLocalResource ?: false
  }
}

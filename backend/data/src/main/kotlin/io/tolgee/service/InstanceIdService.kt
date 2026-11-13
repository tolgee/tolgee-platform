package io.tolgee.service

import io.tolgee.model.InstanceId
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.tryUntilItDoesntBreakConstraint
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import java.util.Date

@Service
class InstanceIdService(
  private val entityManager: EntityManager,
  private val platformTransactionManager: PlatformTransactionManager,
) {
  fun getInstanceId(): String {
    return getOrCreate().instanceId
  }

  fun getInstanceCreatedAt(): Date? {
    return getOrCreate().createdAt
  }

  private fun getOrCreate(): InstanceId {
    return tryUntilItDoesntBreakConstraint {
      executeInNewTransaction(platformTransactionManager) {
        entityManager.find(InstanceId::class.java, 1)
          ?: InstanceId().also {
            entityManager.persist(it)
            entityManager.flush()
          }
      }
    }
  }
}

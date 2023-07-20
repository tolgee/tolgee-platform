package io.tolgee.service

import io.tolgee.model.InstanceId
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.tryUntilItDoesntBreakConstraint
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Service
class InstanceIdService(
  private val entityManager: EntityManager,
  private val platformTransactionManager: PlatformTransactionManager
) {
  @Transactional
  fun getInstanceId(): String {
    return tryUntilItDoesntBreakConstraint {
      executeInNewTransaction(platformTransactionManager) {
        val entity = entityManager.find(InstanceId::class.java, 1)
          ?: let {
            val instanceId = InstanceId()
            entityManager.persist(instanceId)
            entityManager.flush()
            instanceId
          }
        entity.instanceId
      }
    }
  }
}

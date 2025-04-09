package io.tolgee.ee.component.limitsAndReporting

import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.configuration.TransactionScopeConfig
import io.tolgee.ee.service.EeSubscriptionServiceImpl
import io.tolgee.events.EntityPreCommitEvent
import io.tolgee.model.key.Key
import io.tolgee.service.key.KeyService
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.getUsageIncreaseAmount
import org.springframework.context.annotation.Scope
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager

/**
 * Listens for key count changes and checks whether ee instance
 * is not over the limit.
 *
 * We have to use this "EntityPreCommitEvent", because the approach
 * in io.tolgee.ee.component.EeKeyCountReportingListener, doesn't throw the exception
 * properly. (In that case, it would be wrapped with other excption, which we don't want.)
 * That's why we have separate class for that.
 *
 */
@Scope(TransactionScopeConfig.SCOPE_TRANSACTION)
@Component
class EeKeyCountLimitListener(
  private val eeSubscriptionService: EeSubscriptionServiceImpl,
  private val billingConfProvider: PublicBillingConfProvider,
  private val keyService: KeyService,
  private val transactionManager: PlatformTransactionManager
) : Logging {
  private var keyCount: Long? = null

  @EventListener
  fun onActivity(event: EntityPreCommitEvent) {
    if (billingConfProvider().enabled || event.entity !is Key) {
      return
    }

    increaseKeyCount(event.getUsageIncreaseAmount())
    onKeyCountChanged()
  }

  private fun increaseKeyCount(value: Long) {
    if (keyCount == null) {
      executeInNewTransaction(transactionManager) {
        keyCount = keyService.countAllOnInstance()
      }
    }
    keyCount = keyCount!! + value
  }

  fun onKeyCountChanged() {
    eeSubscriptionService.checkKeyCount(keyCount!!)
  }
}

package io.tolgee.ee.component.limitsAndReporting

import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.configuration.TransactionScopeConfig
import io.tolgee.ee.component.limitsAndReporting.generic.WordsLimitChecker
import io.tolgee.events.EntityPreCommitEvent
import io.tolgee.model.translation.Translation
import io.tolgee.service.organization.OrganizationStatsService
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.getWordUsageIncreaseAmount
import org.springframework.context.annotation.Scope
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager

/**
 * Listens for word count changes and checks whether ee instance
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
class EeWordCountLimitListener(
  private val billingConfProvider: PublicBillingConfProvider,
  private val organizationStatsService: OrganizationStatsService,
  private val transactionManager: PlatformTransactionManager,
  private val selfHostedLimitsProvider: SelfHostedLimitsProvider,
) : Logging {
  private var wordCount: Long? = null

  @EventListener
  fun onActivity(event: EntityPreCommitEvent<Translation>) {
    if (billingConfProvider().enabled) {
      return
    }

    increaseWordCount(event.getWordUsageIncreaseAmount())
    onWordCountChanged()
  }

  private val initialWordCount: Long by lazy {
    executeInNewTransaction(transactionManager) {
      organizationStatsService.countAllWordsOnInstance()
    }
  }

  private fun increaseWordCount(value: Long) {
    if (wordCount == null) {
      wordCount = initialWordCount
    }
    wordCount = wordCount!! + value
  }

  fun onWordCountChanged() {
    if (initialWordCount > wordCount!!) {
      return
    }

    if (limits.words.limit < 0) {
      return
    }

    if (limits.autoUpgradeEnabled) {
      return
    }

    WordsLimitChecker(limits = limits).check(wordCount!!)
  }

  private val limits by lazy {
    selfHostedLimitsProvider.getLimits()
  }
}

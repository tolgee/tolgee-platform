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
 * Must listen on EntityPreCommitEvent: the approach [EeKeyCountReportingListener] uses wraps the
 * exception in another one, which loses the limit-exceeded signal.
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
    // Both guards read cached licence limits, while reading the word count runs a
    // full-instance aggregation. Machine translation commits a transaction every few keys,
    // so leaving these below the count charges instances that have no word limit to enforce
    // one aggregation per chunk.
    if (limits.words.limit < 0) {
      return
    }
    if (limits.autoUpgradeEnabled) {
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

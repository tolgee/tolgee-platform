package io.tolgee.ee.component.limitsAndReporting

import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.configuration.TransactionScopeConfig
import io.tolgee.ee.component.limitsAndReporting.generic.WordsLimitChecker
import io.tolgee.events.EntityPreCommitEvent
import io.tolgee.model.translation.Translation
import io.tolgee.service.organization.OrganizationStatsService
import io.tolgee.service.projectExportImport.ContentReplacementScope
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.getWordUsageIncreaseAmount
import org.springframework.context.annotation.Scope
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager

/**
 * Must listen on EntityPreCommitEvent: the approach [EeKeyCountReportingListener] uses wraps the
 * exception in another one, which loses the limit-exceeded signal. Only translation writes are
 * checked — see WordCountLimitTest.`restoring a soft-deleted key is not blocked …`.
 */
@Scope(TransactionScopeConfig.SCOPE_TRANSACTION)
@Component
class EeWordCountLimitListener(
  private val billingConfProvider: PublicBillingConfProvider,
  private val organizationStatsService: OrganizationStatsService,
  private val transactionManager: PlatformTransactionManager,
  private val selfHostedLimitsProvider: SelfHostedLimitsProvider,
  private val contentReplacementScope: ContentReplacementScope,
) : Logging {
  private var addedWords: Long = 0

  @EventListener
  fun onActivity(event: EntityPreCommitEvent<Translation>) {
    if (billingConfProvider().enabled) {
      return
    }
    if (contentReplacementScope.isReplacingContent) {
      return
    }
    // Both, not just the ceiling: a licence server newer than this instance can send an unknown
    // metric (falling back to KEYS_SEATS, so nothing reports words) alongside a real word ceiling,
    // and blocking on a limit the cloud is never told about leaves no way past it.
    if (!limits.metersWords || !limits.words.isEnforced) {
      return
    }
    if (limits.words.autoUpgradeEffective == true) {
      return
    }

    addedWords += event.getWordUsageIncreaseAmount()
    if (addedWords <= 0) {
      return
    }

    WordsLimitChecker(limits = limits).check(instanceWordCountBeforeTransaction + addedWords)
  }

  // `addedWords` is a raw sum of per-translation deltas, while the billed figure is MAX(word_count)
  // per key name across branches — so on a branching project the running total is only an
  // over-estimate while every decrease in the transaction is a real decrease in the billed figure.
  // A decrease on a branch sibling is subtracted from a total it never contributed to, so a mixed
  // transaction can cancel a real increase and pass unchecked. Both bounds are pinned by
  // WordCountLimitTest.`over-counts a branching project by this transaction's own writes` and
  // `a mixed transaction on a branching project can take the instance over the ceiling unchecked`.
  private val instanceWordCountBeforeTransaction: Long by lazy {
    executeInNewTransaction(transactionManager) {
      organizationStatsService.countAllWordsOnInstance()
    }
  }

  private val limits by lazy {
    selfHostedLimitsProvider.getLimits()
  }
}

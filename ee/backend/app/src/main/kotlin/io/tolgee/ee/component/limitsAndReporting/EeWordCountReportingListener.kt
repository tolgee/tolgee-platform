package io.tolgee.ee.component.limitsAndReporting

import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.ee.service.eeSubscription.usageReporting.UsageReportingService
import io.tolgee.events.BeforeOrganizationDeleteEvent
import io.tolgee.events.OnBranchSoftDeleted
import io.tolgee.events.OnProjectActivityEvent
import io.tolgee.events.OnProjectContentReplaced
import io.tolgee.model.Language
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.util.Logging
import io.tolgee.util.hasChangeTo
import io.tolgee.util.hasDeletionStateChangeOf
import io.tolgee.util.logger
import io.tolgee.util.runSentryCatching
import org.springframework.context.event.EventListener
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class EeWordCountReportingListener(
  private val billingConfProvider: PublicBillingConfProvider,
  private val selfHostedLimitsProvider: SelfHostedLimitsProvider,
  private val usageReportingService: UsageReportingService,
) : Logging {
  /**
   * The order is load-bearing, not cosmetic. [EeKeyCountReportingListener.onActivity] subscribes to
   * the same event and makes a blocking licence-server POST whenever its one-minute window is open,
   * while [onWordCountChanged] takes the exclusive lock on the single usage_to_report row and holds
   * it to commit. Running last keeps that lock from enclosing the network call, which would stall
   * every other translation-writing transaction on the instance for its duration. Atomicity is
   * unchanged: the flag is still raised inside the writer's transaction, before commit.
   */
  @Order(Ordered.LOWEST_PRECEDENCE)
  @EventListener
  fun onActivity(event: OnProjectActivityEvent) {
    if (billingConfProvider().enabled) {
      return
    }

    runSentryCatching {
      if (event.changesWordCount()) {
        onWordCountChanged()
      }
    }
  }

  @Order(Ordered.LOWEST_PRECEDENCE)
  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  fun onOrganizationDeleted(event: BeforeOrganizationDeleteEvent) {
    if (billingConfProvider().enabled) {
      return
    }
    runSentryCatching { onWordCountChanged() }
  }

  /**
   * Branch deletion is forced to [io.tolgee.activity.data.RevisionType.DEL] and Branch.deletedAt is
   * not an activity-logged property, so it never reaches the activity predicate below.
   */
  @Order(Ordered.LOWEST_PRECEDENCE)
  @EventListener
  fun onBranchDeleted(event: OnBranchSoftDeleted) {
    if (billingConfProvider().enabled) {
      return
    }
    runSentryCatching { onWordCountChanged() }
  }

  /**
   * Bulk deletes and entities written with activity logging off produce no activity event, so an
   * admin project import would otherwise leave the cloud billing the pre-import figure.
   */
  @Order(Ordered.LOWEST_PRECEDENCE)
  @EventListener
  fun onProjectContentReplaced(event: OnProjectContentReplaced) {
    if (billingConfProvider().enabled) {
      return
    }
    runSentryCatching { onWordCountChanged() }
  }

  fun onWordCountChanged() {
    if (!selfHostedLimitsProvider.getLimits().metersWords) {
      return
    }
    logger.debug("Word count change detected. Marking it for the next report.")
    usageReportingService.markWordUsageChanged()
  }

  private fun OnProjectActivityEvent.changesWordCount(): Boolean {
    if (modifiedEntities.keys.any { it == Translation::class }) {
      return true
    }
    if (hasDeletionStateChangeOf(*SOFT_DELETABLE_COUNTED_ENTITIES)) {
      return true
    }
    if (hasChangeTo(Project::class, "useBranching")) {
      return true
    }
    // Name and namespace are part of the key the billable figure groups by, so a rename regroups it
    // without any translation being touched.
    return hasChangeTo(Key::class, "name", "namespace")
  }

  companion object {
    // No Organization: its deletedAt is not @ActivityLoggedProp and the event is project-scoped,
    // so it could never match. Organization deletion is covered by onOrganizationDeleted.
    private val SOFT_DELETABLE_COUNTED_ENTITIES =
      arrayOf(Project::class, Key::class, Language::class)
  }
}

package io.tolgee.ee.component.limitsAndReporting

import io.tolgee.activity.data.RevisionType
import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.ee.service.NoActiveSubscriptionException
import io.tolgee.ee.service.eeSubscription.EeSubscriptionServiceImpl
import io.tolgee.ee.service.eeSubscription.usageReporting.UsageReportingService
import io.tolgee.events.BeforeOrganizationDeleteEvent
import io.tolgee.events.OnProjectActivityEvent
import io.tolgee.model.Language
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.branching.Branch
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.service.organization.OrganizationStatsService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import io.tolgee.util.runSentryCatching
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class EeWordCountReportingListener(
  private val eeSubscriptionService: EeSubscriptionServiceImpl,
  private val billingConfProvider: PublicBillingConfProvider,
  private val organizationStatsService: OrganizationStatsService,
  private val usageReportingService: UsageReportingService,
) : Logging {
  /**
   * Listens for project activity events and checks if any relevant entity modifications
   * (like translations or project/organization deletions) occurred to report word count changes.
   */
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

  /**
   * Soft-delete and restore surface only as a deletedAt modification; miss one and the instance
   * keeps reporting — and being billed for — words it no longer hosts.
   */
  private fun OnProjectActivityEvent.changesWordCount(): Boolean {
    if (modifiedEntities.keys.any { it == Translation::class }) {
      return true
    }
    return SOFT_DELETABLE_COUNTED_ENTITIES.any { type ->
      modifiedEntities[type]?.any { it.value.isSoftDeletionChange() } == true
    }
  }

  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  fun onOrganizationDeleted(event: BeforeOrganizationDeleteEvent) {
    if (billingConfProvider().enabled) {
      return
    }
    onWordCountChanged()
  }

  fun onWordCountChanged() {
    try {
      logger.debug("Word count change detected. Reporting...")
      val words = organizationStatsService.countAllWordsOnInstance()
      val subscription = eeSubscriptionService.findSubscriptionDto()
      if (subscription != null) {
        logger.debug("Local subscription with license key ${subscription.licenseKey} found.")
      }
      usageReportingService.reportUsage(subscription = subscription, words = words)
    } catch (e: NoActiveSubscriptionException) {
      logger.debug("No active subscription, skipping usage reporting.")
    }
  }

  /**
   * Only a change to deletedAt counts. Creating an entity also lists the field among its
   * modifications, and treating that as a deletion would report usage on every new key.
   */
  private fun ActivityModifiedEntity.isSoftDeletionChange(): Boolean =
    revisionType == RevisionType.MOD && modifications.contains("deletedAt")

  companion object {
    /**
     * Entities whose soft-deletion removes their words from the instance count — see the
     * deleted_at filters in OrganizationStatsService.countAllWordsOnInstance.
     */
    private val SOFT_DELETABLE_COUNTED_ENTITIES =
      listOf(Project::class, Organization::class, Key::class, Language::class, Branch::class)
  }
}

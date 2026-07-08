package io.tolgee.ee.component.limitsAndReporting

import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.ee.service.NoActiveSubscriptionException
import io.tolgee.ee.service.eeSubscription.EeSubscriptionServiceImpl
import io.tolgee.ee.service.eeSubscription.usageReporting.UsageReportingService
import io.tolgee.events.BeforeOrganizationDeleteEvent
import io.tolgee.events.OnProjectActivityEvent
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.translation.Translation
import io.tolgee.service.organization.OrganizationStatsService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import io.tolgee.util.runSentryCatching
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * This class listens for word count changes and reports such changes to the Tolgee Cloud.
 *
 * It reports:
 *  - When the word count is changed due to modifications in translations.
 *  - When a project or organization is deleted, as these events also impact the word count.
 *
 * This listener uses the deferred reporting mechanism, which means that if multiple
 * word count changes occur within a short time period (less than 1 minute), only one
 * report will be sent to Tolgee Cloud, reducing API calls.
 */
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
      val modifiedEntityClasses = event.modifiedEntities.keys.toSet()

      val isTranslationsChanged = modifiedEntityClasses.any { it == Translation::class }

      val isProjectDeletedChanged =
        event.modifiedEntities[Project::class]?.any { it.value.modifications.contains("deletedAt") } == true

      val isOrganizationDeletedChanged =
        event.modifiedEntities[Organization::class]?.any { it.value.modifications.contains("deletedAt") } == true

      if (isTranslationsChanged || isProjectDeletedChanged || isOrganizationDeletedChanged) {
        onWordCountChanged()
      }
    }
  }

  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  fun onOrganizationDeleted(event: BeforeOrganizationDeleteEvent) {
    onWordCountChanged()
  }

  /**
   * Reports changes in word count to Tolgee Cloud.
   *
   * This method is called when translation-related entities are modified or deleted.
   * It uses the deferred reporting mechanism, which may delay the actual API call
   * by up to 1 minute if another report was sent recently, preventing excessive
   * API calls when many translations are modified in quick succession.
   */
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
}

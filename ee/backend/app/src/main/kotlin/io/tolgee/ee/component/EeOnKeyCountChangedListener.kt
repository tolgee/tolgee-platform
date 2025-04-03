package io.tolgee.ee.component

import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.ee.service.EeSubscriptionServiceImpl
import io.tolgee.ee.service.NoActiveSubscriptionException
import io.tolgee.events.BeforeOrganizationDeleteEvent
import io.tolgee.events.OnProjectActivityEvent
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.service.key.KeyService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import io.tolgee.util.runSentryCatching
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class EeOnKeyCountChangedListener(
  private val eeSubscriptionService: EeSubscriptionServiceImpl,
  private val billingConfProvider: PublicBillingConfProvider,
  private val keyService: KeyService,
) : Logging {
  @EventListener
  fun onActivity(event: OnProjectActivityEvent) {
    if (billingConfProvider().enabled) {
      return
    }

    runSentryCatching {
      val modifiedEntityClasses = event.modifiedEntities.keys.toSet()

      val isKeysChanged = modifiedEntityClasses.any { it == Key::class }

      val isProjectDeletedChanged =
        event.modifiedEntities[Project::class]?.any { it.value.modifications.contains("deletedAt") } == true

      if (isKeysChanged || isProjectDeletedChanged) {
        onKeyCountChanged()
      }
    }
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun onOrganizationDeleted(event: BeforeOrganizationDeleteEvent) {
    onKeyCountChanged()
  }

  fun onKeyCountChanged() {
    try {
      val keys = keyService.countAllOnInstance()
      val subscription = eeSubscriptionService.findSubscriptionDto()

      eeSubscriptionService.reportUsage(subscription = subscription, keys = keys)
    } catch (e: NoActiveSubscriptionException) {
      logger.debug("No active subscription, skipping usage reporting.")
    }
  }
}

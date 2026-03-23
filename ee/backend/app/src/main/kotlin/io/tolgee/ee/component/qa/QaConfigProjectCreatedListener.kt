package io.tolgee.ee.component.qa

import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.events.OnProjectCreated
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class QaConfigProjectCreatedListener(
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
) {
  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  fun onProjectCreated(event: OnProjectCreated) {
    val project = event.project
    // Uses EnabledFeaturesProvider (org-level only) intentionally — at creation time,
    // useQaChecks hasn't been set yet, so ProjectFeatureGuard would reject.
    val orgId = project.organizationOwner.id
    if (enabledFeaturesProvider.isFeatureEnabled(orgId, Feature.QA_CHECKS)) {
      project.useQaChecks = true
    }
  }
}

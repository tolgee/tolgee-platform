package io.tolgee.ee.component.qa

import io.tolgee.ee.service.qa.ProjectQaConfigService
import io.tolgee.events.OnProjectCreated
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class QaConfigProjectCreatedListener(
  private val projectQaConfigService: ProjectQaConfigService,
) {
  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  fun onProjectCreated(event: OnProjectCreated) {
    projectQaConfigService.initializeDefaultSettings(event.project.id)
  }
}

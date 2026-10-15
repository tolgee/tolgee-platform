package io.tolgee.ee.component.aiReview

import io.tolgee.ee.service.aiReview.AiReviewRuleChangedEvent
import io.tolgee.ee.service.aiReview.AiReviewTriggerService
import io.tolgee.repository.aiReview.AiReviewRuleRepository
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.runSentryCatching
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AiReviewRuleChangeListener(
  private val aiReviewRuleRepository: AiReviewRuleRepository,
  private val triggerService: AiReviewTriggerService,
  private val platformTransactionManager: PlatformTransactionManager,
) {
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  fun onRuleChanged(event: AiReviewRuleChangedEvent) {
    runSentryCatching {
      executeInNewTransaction(platformTransactionManager) {
        val rule =
          aiReviewRuleRepository.findByProjectIdAndId(event.projectId, event.ruleId) ?: return@executeInNewTransaction
        triggerService.startReevaluationAfterRuleChange(event.projectId, rule)
      }
    }
  }
}

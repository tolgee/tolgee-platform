package io.tolgee.ee.component.aiReview

import io.tolgee.ee.service.aiReview.AiReviewRuleService
import io.tolgee.events.OnLanguageSoftDeleted
import io.tolgee.events.OnNamespaceRenamed
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class AiReviewRuleScopeListener(
  private val aiReviewRuleService: AiReviewRuleService,
) {
  @EventListener
  fun onLanguageSoftDeleted(event: OnLanguageSoftDeleted) {
    aiReviewRuleService.removeLanguage(event.projectId, event.languageId)
  }

  @EventListener
  fun onNamespaceRenamed(event: OnNamespaceRenamed) {
    aiReviewRuleService.renameNamespace(event.projectId, event.oldName, event.newName)
  }
}

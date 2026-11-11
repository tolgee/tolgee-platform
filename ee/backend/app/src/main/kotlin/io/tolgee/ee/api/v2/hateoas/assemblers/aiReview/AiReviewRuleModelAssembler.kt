package io.tolgee.ee.api.v2.hateoas.assemblers.aiReview

import io.tolgee.ee.api.v2.hateoas.model.aiReview.AiReviewRuleCountsModel
import io.tolgee.ee.api.v2.hateoas.model.aiReview.AiReviewRuleModel
import io.tolgee.ee.service.aiReview.AiReviewRuleStatsService.AiReviewRuleCounts
import io.tolgee.model.aiReview.AiReviewRule
import org.springframework.stereotype.Component

@Component
class AiReviewRuleModelAssembler {
  fun toModel(
    rule: AiReviewRule,
    counts: AiReviewRuleCounts?,
  ): AiReviewRuleModel =
    AiReviewRuleModel(
      id = rule.id,
      name = rule.name,
      enabled = rule.enabled,
      tagIn = rule.tagIn,
      tagNotIn = rule.tagNotIn,
      namespaceIn = rule.namespaceIn,
      namespaceNotIn = rule.namespaceNotIn,
      languageIds = rule.languages.map { it.id }.sorted(),
      branchIn = rule.branchIn,
      branchNotIn = rule.branchNotIn,
      threshold = rule.threshold,
      never = rule.never,
      mode = rule.mode,
      promptId = rule.prompt?.id,
      skipOpenQaIssues = rule.skipOpenQaIssues,
      counts = counts?.toModel(),
    )

  private fun AiReviewRuleCounts.toModel() =
    AiReviewRuleCountsModel(
      autoReviewed = autoReviewed,
      failed = failed,
      scored = scored,
      wouldBeAutoReviewed = wouldBeAutoReviewed,
      backlog = backlog,
    )
}

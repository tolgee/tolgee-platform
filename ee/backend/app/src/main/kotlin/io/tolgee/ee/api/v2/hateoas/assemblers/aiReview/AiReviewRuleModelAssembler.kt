package io.tolgee.ee.api.v2.hateoas.assemblers.aiReview

import io.tolgee.ee.api.v2.hateoas.model.aiReview.AiReviewRuleModel
import io.tolgee.ee.api.v2.hateoas.model.aiReview.AiReviewRuleStatsModel
import io.tolgee.ee.data.aiReview.AiReviewRuleStats
import io.tolgee.model.aiReview.AiReviewRule
import org.springframework.stereotype.Component

@Component
class AiReviewRuleModelAssembler {
  fun toModel(
    rule: AiReviewRule,
    stats: AiReviewRuleStats?,
  ): AiReviewRuleModel =
    AiReviewRuleModel(
      id = rule.id,
      name = rule.name,
      enabled = rule.enabled,
      mode = rule.mode,
      threshold = rule.threshold,
      languageThresholds = rule.languageThresholds.toList(),
      promptId = rule.prompt?.id,
      promptName = rule.prompt?.name,
      skipWithOpenQaIssues = rule.skipWithOpenQaIssues,
      tagIn = rule.tagIn.toList(),
      tagNotIn = rule.tagNotIn.toList(),
      namespaceIn = rule.namespaceIn.toList(),
      namespaceNotIn = rule.namespaceNotIn.toList(),
      languageIds = rule.languageIds.toList(),
      branchIn = rule.branchIn.toList(),
      branchNotIn = rule.branchNotIn.toList(),
      stats = (stats ?: AiReviewRuleStats()).toModel(),
    )

  private fun AiReviewRuleStats.toModel() =
    AiReviewRuleStatsModel(
      scored = scored,
      wouldAutoReview = wouldAutoReview,
      autoReviewed = autoReviewed,
      rejected = rejected,
      failed = failed,
    )
}

package io.tolgee.ee.api.v2.hateoas.assemblers.aiReview

import io.tolgee.ee.api.v2.hateoas.model.aiReview.AiReviewRuleModel
import io.tolgee.ee.api.v2.hateoas.model.aiReview.AiReviewRuleOverrideModel
import io.tolgee.model.aiReview.AiReviewRule
import org.springframework.stereotype.Component

@Component
class AiReviewRuleModelAssembler {
  fun toModel(rule: AiReviewRule): AiReviewRuleModel =
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
      overrides =
        rule.overrides
          .sortedBy { it.language.id }
          .map { AiReviewRuleOverrideModel(it.language.id, it.threshold, it.never) },
    )
}

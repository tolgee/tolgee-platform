package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.Language
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.aiReview.AiReviewRuleLanguageOverride

class AiReviewRuleBuilder(
  val projectBuilder: ProjectBuilder,
) : BaseEntityDataBuilder<AiReviewRule, AiReviewRuleBuilder>() {
  override var self: AiReviewRule = AiReviewRule(project = projectBuilder.self)

  fun addOverride(
    language: Language,
    threshold: Int = 85,
    never: Boolean = false,
  ): AiReviewRuleLanguageOverride {
    val override = AiReviewRuleLanguageOverride(rule = self, language = language, threshold = threshold, never = never)
    self.overrides.add(override)
    return override
  }
}

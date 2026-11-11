package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.aiReview.AiReviewRule

class AiReviewRuleBuilder(
  val projectBuilder: ProjectBuilder,
) : BaseEntityDataBuilder<AiReviewRule, AiReviewRuleBuilder>() {
  override var self: AiReviewRule = AiReviewRule(project = projectBuilder.self)
}

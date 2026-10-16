package io.tolgee.ee.api.v2.hateoas.model.aiReview

import io.tolgee.hateoas.aiReview.AiReviewIssueModel
import io.tolgee.model.enums.aiReview.AiReviewOutcome
import org.springframework.hateoas.RepresentationModel

data class TranslationAiReviewModel(
  val translationId: Long,
  val outcome: AiReviewOutcome,
  val score: Int?,
  val threshold: Int?,
  val dryRun: Boolean,
  val ruleId: Long?,
  val ruleName: String?,
  val ruleScope: AiReviewRuleScopeModel?,
  val promptId: Long?,
  val promptName: String?,
  val providerName: String?,
  val reviewedAt: Long?,
  val issues: List<AiReviewIssueModel>,
  val errorCode: String?,
  val errorDetail: String?,
  val resolvedAt: Long?,
) : RepresentationModel<TranslationAiReviewModel>()

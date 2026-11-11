package io.tolgee.ee.api.v2.hateoas.model.aiReview

import io.tolgee.hateoas.userAccount.SimpleUserAccountModel
import io.tolgee.model.enums.aiReview.AiReviewFailureReason
import io.tolgee.model.enums.aiReview.AiReviewStatus
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "aiReviews", itemRelation = "aiReview")
open class TranslationAiReviewModel(
  val status: AiReviewStatus,
  val score: Int?,
  val threshold: Int?,
  val issues: List<AiReviewIssueModel>,
  val ruleId: Long?,
  val ruleName: String?,
  val promptName: String?,
  val providerName: String?,
  val model: String?,
  val reviewedAt: Long?,
  val failureReason: AiReviewFailureReason?,
  val lastFailureReason: AiReviewFailureReason?,
  val lastFailedAt: Long?,
  val resolvedBy: SimpleUserAccountModel?,
  val resolvedAt: Long?,
  val scoreOnlyRun: Boolean,
) : RepresentationModel<TranslationAiReviewModel>()

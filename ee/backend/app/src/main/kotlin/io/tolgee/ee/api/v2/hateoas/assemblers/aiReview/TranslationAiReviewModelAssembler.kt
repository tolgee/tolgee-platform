package io.tolgee.ee.api.v2.hateoas.assemblers.aiReview

import io.tolgee.ee.api.v2.controllers.aiReview.TranslationAiReviewController
import io.tolgee.ee.api.v2.hateoas.model.aiReview.AiReviewIssueModel
import io.tolgee.ee.api.v2.hateoas.model.aiReview.TranslationAiReviewModel
import io.tolgee.hateoas.userAccount.SimpleUserAccountModelAssembler
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TranslationAiReviewModelAssembler(
  private val simpleUserAccountModelAssembler: SimpleUserAccountModelAssembler,
) : RepresentationModelAssemblerSupport<TranslationAiReview, TranslationAiReviewModel>(
    TranslationAiReviewController::class.java,
    TranslationAiReviewModel::class.java,
  ) {
  override fun toModel(entity: TranslationAiReview): TranslationAiReviewModel =
    TranslationAiReviewModel(
      status = entity.status,
      score = entity.score,
      threshold = entity.threshold,
      issues = entity.issues.map { AiReviewIssueModel.of(it) },
      ruleId = entity.rule?.id,
      ruleName = entity.rule?.name,
      promptName = entity.promptName,
      providerName = entity.providerName,
      model = entity.model,
      reviewedAt = entity.reviewedAt?.time,
      failureReason = entity.failureReason,
      lastFailureReason = entity.lastFailureReason,
      lastFailedAt = entity.lastFailedAt?.time,
      resolvedBy = entity.resolvedBy?.let { simpleUserAccountModelAssembler.toModel(it) },
      resolvedAt = entity.resolvedAt?.time,
      dryRun = entity.ruleMode == AiReviewRuleMode.DRY_RUN,
    )
}

package io.tolgee.ee.api.v2.hateoas.assemblers.aiReview

import io.tolgee.ee.api.v2.hateoas.model.aiReview.AiReviewRuleScopeModel
import io.tolgee.ee.api.v2.hateoas.model.aiReview.TranslationAiReviewModel
import io.tolgee.hateoas.aiReview.AiReviewIssueModel
import io.tolgee.model.aiReview.TranslationAiReview
import org.springframework.stereotype.Component

@Component
class TranslationAiReviewModelAssembler {
  fun toModel(review: TranslationAiReview): TranslationAiReviewModel =
    TranslationAiReviewModel(
      translationId = review.translation.id,
      outcome = review.outcome,
      score = review.score,
      threshold = review.threshold,
      dryRun = review.dryRun,
      ruleId = review.rule?.id,
      ruleName = review.rule?.name,
      ruleScope =
        review.rule?.let {
          AiReviewRuleScopeModel(
            tagIn = it.tagIn.toList(),
            tagNotIn = it.tagNotIn.toList(),
            namespaceIn = it.namespaceIn.toList(),
            namespaceNotIn = it.namespaceNotIn.toList(),
            languageIds = it.languageIds.toList(),
            branchIn = it.branchIn.toList(),
            branchNotIn = it.branchNotIn.toList(),
          )
        },
      promptId = review.prompt?.id,
      promptName = review.prompt?.name,
      providerName = review.providerName,
      reviewedAt = review.reviewedAt?.time,
      issues = review.issues.orEmpty().map { AiReviewIssueModel.of(it) },
      errorCode = review.errorCode,
      errorDetail = review.errorDetail,
      resolvedAt = review.resolvedAt?.time,
    )
}

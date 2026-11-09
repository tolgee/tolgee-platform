package io.tolgee.service.aiReview

import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.translation.Translation

/** See docs/ai-review/README.md 3.3 "AI-managed review". */
object AiReviewManaged {
  /** Needs the alias `r` for TranslationAiReview and `t` for its Translation. */
  const val JPQL =
    "(t.state = io.tolgee.model.enums.TranslationState.REVIEWED and (" +
      "(r.status in (io.tolgee.model.enums.aiReview.AiReviewStatus.APPROVED," +
      " io.tolgee.model.enums.aiReview.AiReviewStatus.BELOW_THRESHOLD)" +
      " and r.textHash = ${AiReviewTextHash.CURRENT_TEXT_JPQL})" +
      " or (t.auto = true and r.status not in (" +
      "io.tolgee.model.enums.aiReview.AiReviewStatus.CONFIRMED," +
      " io.tolgee.model.enums.aiReview.AiReviewStatus.REJECTED," +
      " io.tolgee.model.enums.aiReview.AiReviewStatus.DRY_RUN_PASS," +
      " io.tolgee.model.enums.aiReview.AiReviewStatus.DRY_RUN_FAIL))))"

  val HUMAN_DECISIONS = setOf(AiReviewStatus.CONFIRMED, AiReviewStatus.REJECTED)

  private val NOT_AI_MANAGED = HUMAN_DECISIONS + setOf(AiReviewStatus.DRY_RUN_PASS, AiReviewStatus.DRY_RUN_FAIL)

  fun of(
    translation: Translation,
    review: TranslationAiReview?,
  ): Boolean {
    if (isAiApproval(translation, review)) return true
    if (translation.state != TranslationState.REVIEWED || review == null) return false
    if (review.status == AiReviewStatus.BELOW_THRESHOLD && review.textHash == AiReviewTextHash.of(translation)) {
      return true
    }
    return translation.auto && keepsAiManaged(review)
  }

  fun keepsAiManaged(review: TranslationAiReview): Boolean = review.status !in NOT_AI_MANAGED

  fun isAiApproval(
    translation: Translation,
    review: TranslationAiReview?,
  ): Boolean {
    if (translation.state != TranslationState.REVIEWED) return false
    if (review == null || review.status != AiReviewStatus.APPROVED) return false
    return review.textHash == AiReviewTextHash.of(translation)
  }
}

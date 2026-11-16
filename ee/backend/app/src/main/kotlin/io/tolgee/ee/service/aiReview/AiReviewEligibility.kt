package io.tolgee.ee.service.aiReview

import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.translation.Translation
import io.tolgee.service.aiReview.AiReviewEligibleText
import io.tolgee.service.aiReview.AiReviewManaged
import io.tolgee.service.aiReview.AiReviewQueued
import io.tolgee.service.aiReview.AiReviewTextHash

object AiReviewEligibility {
  fun isEligible(
    translation: Translation,
    baseLanguageId: Long?,
    review: TranslationAiReview?,
  ): Boolean {
    if (translation.language.id == baseLanguageId) return false
    if (isTrashed(translation)) return false
    if (!AiReviewEligibleText.of(translation)) return false
    return isRequeuedHumanDecision(review) || !isHumanReviewed(translation, review)
  }

  fun isRequeuedHumanDecision(review: TranslationAiReview?): Boolean =
    review != null && review.status in HUMAN_DECISIONS && review.reReviewRequested

  fun isTrashed(translation: Translation): Boolean =
    translation.key.deletedAt != null || translation.key.branch?.deletedAt != null

  fun isHumanReviewed(
    translation: Translation,
    review: TranslationAiReview?,
  ): Boolean = translation.state == TranslationState.REVIEWED && !isAiManagedReview(translation, review)

  fun isAiManagedReview(
    translation: Translation,
    review: TranslationAiReview?,
  ): Boolean = AiReviewManaged.of(translation, review)

  fun keepsAiManaged(review: TranslationAiReview): Boolean = AiReviewManaged.keepsAiManaged(review)

  fun isAiApproval(
    translation: Translation,
    review: TranslationAiReview?,
  ): Boolean = AiReviewManaged.isAiApproval(translation, review)

  fun isHumanDecision(review: TranslationAiReview): Boolean = review.status in HUMAN_DECISIONS

  /** See docs/ai-review/README.md 3.4 "A `FAILED` row without a failure reason is only a marker". */
  fun isMarker(review: TranslationAiReview): Boolean =
    review.status == AiReviewStatus.FAILED && review.failureReason == null

  fun isProtectedReviewed(translation: Translation): Boolean =
    translation.key.project.translationProtection == TranslationProtection.PROTECT_REVIEWED &&
      translation.state == TranslationState.REVIEWED

  fun isQueued(review: TranslationAiReview): Boolean = AiReviewQueued.of(review)

  fun isReviewForCurrentText(
    translation: Translation,
    review: TranslationAiReview,
  ): Boolean = review.textHash == currentTextHash(translation)

  fun currentTextHash(translation: Translation): String = AiReviewTextHash.of(translation)

  val HUMAN_DECISIONS = AiReviewManaged.HUMAN_DECISIONS
}

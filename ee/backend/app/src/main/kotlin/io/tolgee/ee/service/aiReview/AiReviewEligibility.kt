package io.tolgee.ee.service.aiReview

import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.translation.Translation
import io.tolgee.service.aiReview.AiReviewTextHash

object AiReviewEligibility {
  fun isEligible(
    translation: Translation,
    baseLanguageId: Long?,
    review: TranslationAiReview?,
  ): Boolean {
    if (translation.language.id == baseLanguageId) return false
    if (translation.text.isNullOrBlank()) return false
    if (translation.state == TranslationState.DISABLED) return false
    if (translation.outdated) return false
    return !isHumanReviewed(translation, review)
  }

  fun isHumanReviewed(
    translation: Translation,
    review: TranslationAiReview?,
  ): Boolean = translation.state == TranslationState.REVIEWED && !isAiManagedReview(translation, review)

  /**
   * A Reviewed machine translation whose state the AI set and still manages: an AI approval, an
   * approval whose text MT replaced under Protect reviewed, or its queued or failed re-review. A human
   * review clears `auto`, so it never matches.
   */
  fun isAiManagedReview(
    translation: Translation,
    review: TranslationAiReview?,
  ): Boolean {
    if (isAiApproval(translation, review)) return true
    if (translation.state != TranslationState.REVIEWED || !translation.auto) return false
    return review != null && review.status !in NOT_AI_MANAGED
  }

  fun isAiApproval(
    translation: Translation,
    review: TranslationAiReview?,
  ): Boolean {
    if (translation.state != TranslationState.REVIEWED) return false
    if (review == null || review.status != AiReviewStatus.APPROVED) return false
    return isReviewForCurrentText(translation, review)
  }

  fun isHumanDecision(review: TranslationAiReview): Boolean = review.status in HUMAN_DECISIONS

  fun isProtectedReviewed(translation: Translation): Boolean =
    translation.key.project.translationProtection == TranslationProtection.PROTECT_REVIEWED &&
      translation.state == TranslationState.REVIEWED

  fun isQueued(review: TranslationAiReview): Boolean =
    review.status == AiReviewStatus.PENDING || review.reReviewRequested

  fun isReviewForCurrentText(
    translation: Translation,
    review: TranslationAiReview,
  ): Boolean = review.textHash == currentTextHash(translation)

  fun currentTextHash(translation: Translation): String = AiReviewTextHash.of(translation)

  val HUMAN_DECISIONS = setOf(AiReviewStatus.CONFIRMED, AiReviewStatus.REJECTED)

  private val NOT_AI_MANAGED = HUMAN_DECISIONS + setOf(AiReviewStatus.DRY_RUN_PASS, AiReviewStatus.DRY_RUN_FAIL)
}

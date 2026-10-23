package io.tolgee.ee.service.aiReview

import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.translation.Translation
import org.apache.commons.codec.digest.DigestUtils.sha256Hex

object AiReviewEligibility {
  fun isReviewForCurrentText(
    translation: Translation,
    review: TranslationAiReview,
  ): Boolean = review.textHash == sha256Hex(translation.text ?: "")

  fun isAiApproval(
    translation: Translation,
    review: TranslationAiReview?,
  ): Boolean {
    if (translation.state != TranslationState.REVIEWED) return false
    if (review == null || review.status != AiReviewStatus.APPROVED) return false
    return isReviewForCurrentText(translation, review)
  }

  fun isHumanReviewed(
    translation: Translation,
    review: TranslationAiReview?,
  ): Boolean = translation.state == TranslationState.REVIEWED && !isAiApproval(translation, review)

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
}

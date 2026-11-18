package io.tolgee.service.aiReview

import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.Translation_
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate

/** The Kotlin and Criteria forms must agree, or the outcome filter lists keys whose cells show nothing. */
object AiReviewResultApplies {
  fun stillApplies(
    status: AiReviewStatus,
    state: TranslationState,
  ): Boolean =
    when (status) {
      AiReviewStatus.APPROVED -> state == TranslationState.REVIEWED
      AiReviewStatus.BELOW_THRESHOLD -> state == TranslationState.TRANSLATED || state == TranslationState.REVIEWED
      AiReviewStatus.FAILED -> state == TranslationState.TRANSLATED
      AiReviewStatus.PENDING,
      AiReviewStatus.SCORE_ONLY_PASS,
      AiReviewStatus.SCORE_ONLY_FAIL,
      AiReviewStatus.SKIPPED_QA_ISSUES,
      AiReviewStatus.OVERRIDDEN,
      -> true
    }

  fun criteria(
    cb: CriteriaBuilder,
    statusPath: Path<AiReviewStatus>,
    translation: Path<Translation>,
    status: AiReviewStatus,
  ): Predicate {
    val hasStatus = cb.equal(statusPath, status)
    val state = translation.get(Translation_.state)
    val translated = cb.equal(state, TranslationState.TRANSLATED)
    val reviewed = cb.equal(state, TranslationState.REVIEWED)
    return when (status) {
      AiReviewStatus.APPROVED -> cb.and(hasStatus, reviewed)
      AiReviewStatus.BELOW_THRESHOLD -> cb.and(hasStatus, cb.or(translated, reviewed))
      AiReviewStatus.FAILED -> cb.and(hasStatus, translated)
      AiReviewStatus.PENDING,
      AiReviewStatus.SCORE_ONLY_PASS,
      AiReviewStatus.SCORE_ONLY_FAIL,
      AiReviewStatus.SKIPPED_QA_ISSUES,
      AiReviewStatus.OVERRIDDEN,
      -> hasStatus
    }
  }
}

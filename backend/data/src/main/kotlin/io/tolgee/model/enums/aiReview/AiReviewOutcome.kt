package io.tolgee.model.enums.aiReview

import io.tolgee.model.enums.TranslationState

enum class AiReviewOutcome {
  PENDING,
  AUTO_REVIEWED,
  WOULD_AUTO_REVIEW,
  BELOW_THRESHOLD,
  FAILED,
  CONFIRMED,
  REJECTED,
  ;

  fun isShownFor(state: TranslationState): Boolean = this in VISIBLE && !isObsoleteFor(state)

  fun isObsoleteFor(state: TranslationState): Boolean =
    isShownOnlyWhileTranslated && state != TranslationState.TRANSLATED

  val isShownOnlyWhileTranslated: Boolean get() = this in SHOWN_ONLY_WHILE_TRANSLATED

  companion object {
    val VISIBLE = setOf(AUTO_REVIEWED, WOULD_AUTO_REVIEW, BELOW_THRESHOLD, FAILED)
    val REEVALUATED = setOf(AUTO_REVIEWED, WOULD_AUTO_REVIEW)
    private val SHOWN_ONLY_WHILE_TRANSLATED = setOf(BELOW_THRESHOLD, FAILED)
  }
}

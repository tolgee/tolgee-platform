package io.tolgee.service.aiReview

import io.tolgee.model.enums.TranslationState

/** Null means "keep the current value". The outdated flag is always set by the caller. */
data class SourceChangeDecision(
  val newState: TranslationState? = TranslationState.TRANSLATED,
  val setAuto: Boolean? = null,
  val deleteAiReviewRow: Boolean = false,
) {
  companion object {
    fun keepOrTranslated(keepState: Boolean): SourceChangeDecision {
      if (keepState) return SourceChangeDecision(newState = null)
      return SourceChangeDecision()
    }
  }
}

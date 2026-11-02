package io.tolgee.service.aiReview

import io.tolgee.model.enums.TranslationState

/**
 * What happens to one translation when its source text changes. Null means "keep the current
 * value". The outdated flag is always set by the caller.
 */
data class SourceChangeDecision(
  val newState: TranslationState? = TranslationState.TRANSLATED,
  val setAuto: Boolean? = null,
  val deleteAiReviewRow: Boolean = false,
)

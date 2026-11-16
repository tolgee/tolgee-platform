package io.tolgee.service.aiReview

import io.tolgee.model.translation.Translation

data class AiReviewRowState(
  val aiManaged: Boolean,
  val currentApproval: Boolean,
) {
  companion object {
    val NONE = AiReviewRowState(aiManaged = false, currentApproval = false)
  }
}

interface AiReviewRowAccess {
  fun reviewStates(translations: Collection<Translation>): Map<Long, AiReviewRowState>

  fun deleteRow(translationId: Long)
}

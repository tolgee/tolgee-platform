package io.tolgee.service.aiReview

import io.tolgee.model.translation.Translation
import org.springframework.stereotype.Service

data class AiReviewRowState(
  val aiManaged: Boolean,
  val currentApproval: Boolean,
) {
  companion object {
    val NONE = AiReviewRowState(aiManaged = false, currentApproval = false)
  }
}

interface AiReviewRowAccess {
  fun isAiManagedReview(translation: Translation): Boolean

  fun hasCurrentApproval(translation: Translation): Boolean

  fun reviewStates(translations: Collection<Translation>): Map<Long, AiReviewRowState>

  fun deleteRow(translationId: Long)
}

@Service
class AiReviewRowAccessOssStub : AiReviewRowAccess {
  override fun isAiManagedReview(translation: Translation): Boolean = false

  override fun hasCurrentApproval(translation: Translation): Boolean = false

  override fun reviewStates(translations: Collection<Translation>): Map<Long, AiReviewRowState> = emptyMap()

  override fun deleteRow(translationId: Long) {
    // No-op: AI review is an EE feature
  }
}

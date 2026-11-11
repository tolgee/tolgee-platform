package io.tolgee.service.aiReview

import io.tolgee.model.translation.Translation
import org.springframework.stereotype.Service

@Service
class AiReviewRowAccessOssStub : AiReviewRowAccess {
  override fun reviewStates(translations: Collection<Translation>): Map<Long, AiReviewRowState> = emptyMap()

  override fun deleteRow(translationId: Long) {
    // No-op: AI review is an EE feature
  }
}

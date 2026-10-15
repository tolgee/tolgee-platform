package io.tolgee.service.aiReview

import io.tolgee.batch.data.BatchTranslationTargetItem
import org.springframework.stereotype.Service

@Service
class AiReviewBatchServiceOssStub : AiReviewBatchService {
  override fun reviewItem(
    projectId: Long,
    item: BatchTranslationTargetItem,
  ) = Unit

  override fun reevaluateItem(
    projectId: Long,
    item: BatchTranslationTargetItem,
  ) = Unit
}

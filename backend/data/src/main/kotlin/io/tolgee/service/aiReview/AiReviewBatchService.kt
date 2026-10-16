package io.tolgee.service.aiReview

import io.tolgee.batch.data.BatchTranslationTargetItem

interface AiReviewBatchService {
  fun reviewItem(
    projectId: Long,
    item: BatchTranslationTargetItem,
  )

  fun reevaluateItem(
    projectId: Long,
    item: BatchTranslationTargetItem,
  )
}

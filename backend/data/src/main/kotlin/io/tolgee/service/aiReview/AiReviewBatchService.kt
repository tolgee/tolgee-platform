package io.tolgee.service.aiReview

import io.tolgee.batch.data.BatchJobDto

interface AiReviewBatchService {
  fun reviewTranslation(
    job: BatchJobDto,
    translationId: Long,
  )
}

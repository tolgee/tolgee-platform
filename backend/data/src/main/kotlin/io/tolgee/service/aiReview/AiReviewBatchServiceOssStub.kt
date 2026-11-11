package io.tolgee.service.aiReview

import io.tolgee.batch.data.BatchJobDto
import org.springframework.stereotype.Service

@Service
class AiReviewBatchServiceOssStub : AiReviewBatchService {
  override fun reviewTranslation(
    job: BatchJobDto,
    translationId: Long,
  ) {
    // No-op: AI review is an EE feature
  }
}

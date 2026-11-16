package io.tolgee.service.aiReview

import org.springframework.stereotype.Service

@Service
class AiReviewDemotionServiceOssStub : AiReviewDemotionService {
  override fun demoteKeys(
    projectId: Long,
    keyIds: Collection<Long>,
  ) {
    // No-op: AI review is an EE feature
  }
}

package io.tolgee.service.aiReview

interface AiReviewDemotionService {
  fun demoteKeys(
    projectId: Long,
    keyIds: Collection<Long>,
  )
}
